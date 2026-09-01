# Part 6 - CSRF and Session Management

## What CSRF is

> **Learning note:** Browsers automatically attach cookies — including the session cookie (`JSESSIONID`) — to *every* request sent to the origin that set them. An attacker who lures a logged-in user to a malicious page can embed a form or script that POSTs to your application (for example `/employees/42/update` or `/pos/checkout`). The request carries the victim's session cookie, so the server executes the action as if the victim clicked the button. The server has no built-in way to distinguish "this POST came from my own page" from "this POST came from evil.com". Classic vectors include auto-submitting forms (`<form action="https://app/logout" method="post"><script>document.forms[0].submit()</script></form>`), image tags with side-effect URLs, and fetch/XHR calls from a compromised third-party script. Simply "checking the Referer header" or "requiring a custom header" does not protect cookie-based sessions because the attacker's request *also* carries the cookie and can include headers if CORS is misconfigured. The defense must prove the request *originated* from a page the server itself rendered.

## The countermeasure this app uses — the Synchronizer Token Pattern

The application relies on Spring Security's built-in **Synchronizer Token Pattern** (also called the "CSRF token" pattern):

1. When a session is created, Spring Security generates a cryptographically random token and stores it in the session.
2. Every server-rendered form that performs a state-changing action (`POST`, `PUT`, `DELETE`, `PATCH`) receives this token as a hidden field.
3. On submission, `CsrfFilter` compares the submitted token to the one in the session. A mismatch or missing token produces **403 Forbidden**.

The `SecurityConfig` javadoc explains why CSRF is *not* disabled:

```java
/**
 * Form login backed by {@code Employee} rows.
 *
 * <p>There are no hardcoded credentials and no default passwords: every login is
 * an employee with a BCrypt hash in the database. CSRF protection stays enabled
 * because the UI is server-rendered and Thymeleaf injects the token into forms
 * automatically.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
```

**Crucially, the app never calls `.csrf().disable()`** — a common but dangerous shortcut in tutorials that assume a pure REST/SPA architecture. Because StoreMS uses server-side rendering with Thymeleaf, the synchronizer pattern is the correct, zero-boilerplate choice.

## How the token reaches every form

Thymeleaf's `th:action` attribute automatically injects the CSRF token as a hidden input named `_csrf` (and the header `X-CSRF-TOKEN` for AJAX) when the form's `method` is not `GET`. The developer writes:

```html
<form th:action="@{/login}" method="post">
```

and Thymeleaf emits:

```html
<form action="/login" method="post">
  <input type="hidden" name="_csrf" value="a1b2c3d4-...">
  ...
</form>
```

**Login form** — `login.html:28-29`:

```html
<!-- th:action makes Thymeleaf inject the CSRF token automatically. -->
<form th:action="@{/login}" method="post">
```

**Employee management** — `manager/employee-detail.html:25` (update), `69` (access), `84`, `106`, `124`, `132` — six distinct POST forms, each protected.

**Inventory** — `manager/inventory.html:40, 50, 60, 65, 74, 127` — set price, set quantity, adjust up/down, remove, add item.

**Products** — `manager/products.html:19, 72, 76, 82` — create, activate, deactivate, delete.

**Till (POS)** — `pos/till.html:63, 113, 122, 138, 149` — add line, update line, remove line, checkout, clear.

**Employees list** — `manager/employees.html:19` — create employee.

**Shift report logout** — `sales/shift-report.html:93-96`:

```html
<!-- The actual sign-out. POST so CSRF applies. -->
<form th:action="@{/logout}" method="post" class="inline" th:if="${ending}">
    <button class="btn btn-lg" type="submit">Sign out</button>
</form>
```

**Total POST surface:** 23 forms across the application. Every single one uses `th:action="@{...}" method="post"`, so every single one receives the CSRF token automatically. GET forms (search, filter, pagination) do not need tokens because they have no side effects — they are safe by HTTP definition.

## Logout, done safely

The logout configuration lives in `SecurityConfig` lines 50-54:

```java
.logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/login?loggedOut")
        .invalidateHttpSession(true)
        .clearAuthentication(true));
```

| Setting | Purpose |
|---|---|
| `logoutUrl("/logout")` | Only `POST /logout` is accepted. A `GET /logout` would be trivially CSRF-able (an `<img src="/logout">` on any page would log the user out). |
| `invalidateHttpSession(true)` | Destroys the server-side session and issues a new `JSESSIONID` (or removes the cookie). The old session ID can no longer be replayed. |
| `clearAuthentication(true)` | Clears the `SecurityContext`, so any thread-local principal is removed immediately. |
| `logoutSuccessUrl("/login?loggedOut")` | Redirects to the login page with a "You have been signed out" flash message (`login.html:24-26`). |

**Layered flow — "End shift & log out"**

1. Top-bar link in `layout.html:76` (comment lines 72-75 explains the design):

```html
<!--
  Log out routes through the shift report first, which is what makes the
  report "generated at logout". The real logout POST lives on that page.
-->
<a class="btn btn-quiet btn-sm" th:href="@{/my-sales/report(ending=true)}">End shift & log out</a>
```

2. `MySalesController` serves the shift report (`MySalesController.java:63-75`, javadoc lines 24-28):

```java
/** Shift summary, shown on the way out. */
@GetMapping("/my-sales/report")
String shiftReport(@RequestParam(required = false)
                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                   @RequestParam(name = "ending", defaultValue = "false") boolean ending,
                   Model model) {
    LocalDay day = (date == null) ? LocalDate.now() : date;
    DailySalesReport report =
            reportService.forEmployee(CurrentUser.storeId(), CurrentUser.employeeId(), day);

    model.addAttribute("report", report);
    model.addAttribute("ending", ending);
    return "sales/shift-report";
}
```

3. `shift-report.html` renders the logout form only when `ending=true` (`shift-report.html:93-96` quoted above). The user sees their shift totals, can download CSV/PDF, then clicks **Sign out** — a POST to `/logout` with a valid CSRF token.

This design ensures:
- The employee *reviews* their shift before the session ends.
- Logout is a state-changing POST protected by CSRF (not a GET).
- The session is fully invalidated on the server, not just the cookie cleared.

## Session creation and fixation

When authentication succeeds, Spring Security relies on the **servlet container's default session-fixation protection**. The typical flow:

1. Unauthenticated request arrives → container creates a session (or reuses an existing `JSESSIONID` if the client presented one).
2. User submits credentials → `UsernamePasswordAuthenticationFilter` authenticates.
3. On success, `SessionAuthenticationStrategy` (by default `ChangeSessionIdAuthenticationStrategy` / `MigrateSessionAuthenticationStrategy` depending on container) **creates a new session ID** and migrates attributes. The old session ID is abandoned.
4. The attacker cannot "plant" a session ID and ride it after the victim logs in, because the ID changes at step 3.

The application does not override this behaviour. The only explicit session manipulation is the **invalidation on logout** (`invalidateHttpSession(true)` in `SecurityConfig.java:53`), which is the visible counterpart: just as a fresh session ID is issued at login, the session is destroyed at logout.

> **Why it matters:** If logout merely deleted the cookie (`Set-Cookie: JSESSIONID=; Max-Age=0`) but left the server-side session alive, an attacker who captured the session ID (via XSS, network sniffing on non-HTTPS, or session fixation before login) could continue using it. `invalidateHttpSession(true)` guarantees the server-side session data is removed.

## Learning notes

> **Learning note:** CSRF and XSS are often confused. **CSRF** = the browser *automatically* sends the victim's cookies with a forged request; the attacker doesn't need to read the response. **XSS** = the attacker injects script that runs *in the victim's origin*, letting them read the response, steal tokens, and act as the user. CSRF tokens do not stop XSS; Content-Security-Policy and output encoding do.

> **Learning note:** The "double-submit cookie" pattern (send the token in a cookie *and* a header/form field, compare both) is weaker than the synchronizer pattern because the cookie is still sent automatically. If the attacker can write a cookie (subdomain takeover, MITM on non-HTTPS, or a related-domain XSS), they can set the CSRF cookie to a known value. The synchronizer token lives only in the server session and the rendered page — the attacker cannot read or write it.

> **Learning note:** Modern browsers send cookies with `SameSite=Lax` by default (since ~2020). This blocks cross-site POSTs from third-party contexts, providing a *defense-in-depth* layer. However, `Lax` still allows top-level navigations (links, GET forms), and an attacker can sometimes trigger a POST via a top-level navigation (e.g., `target="_blank"` + `window.opener.location`). Do not rely on `SameSite` alone — keep the synchronizer token.

> **Learning note:** OWASP CSRF Prevention Cheat Sheet (cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html) lists the synchronizer token as the primary defense for server-rendered apps, exactly what Spring Security + Thymeleaf provides out of the box.

> **Chapter summary:**  
> - Every state-changing form in StoreMS carries a per-session CSRF token injected by `th:action` — 23 forms verified.  
> - CSRF is explicitly *not* disabled; the synchronizer pattern is the correct default for server-rendered UIs.  
> - Logout is a `POST /logout` with `invalidateHttpSession(true)` and `clearAuthentication(true)`, preventing both CSRF logout and session replay.  
> - The "End shift & log out" flow routes through a shift-report screen, making logout deliberate and auditable.  
> - Session fixation is mitigated by the servlet container + Spring Security defaults (new session ID on authentication).  
> - `SameSite=Lax` cookies add a modern complementary layer but do not replace the token.
# Part 4 - Authorization and Roles

In this chapter you learn how StoreMS decides who can see what and do what. Authentication (proving who you are) happens at login. Authorization (deciding what you are allowed to do) kicks in on every subsequent request. The system uses role-based access control (RBAC) where permissions are grouped into roles, and users are assigned one or more roles.

## Roles as a concept

Spring Security distinguishes between **authorities** and **roles**:

- An **authority** is a single permission — a string such as `ROLE_EMPLOYEE` or `ROLE_STORE_MANAGER`.
- A **role** is an authority with the `ROLE_` prefix. When you call `hasRole("STORE_MANAGER")`, Spring checks for the authority `ROLE_STORE_MANAGER`. When you call `hasAuthority("ROLE_STORE_MANAGER")`, you are checking the raw authority directly.

StoreMS defines its role names as constants in `Roles.java`:

```java
// src/main/java/com/salias/storems/security/Roles.java
public static final String STORE_MANAGER = "STORE_MANAGER";
public static final String EMPLOYEE = "EMPLOYEE";

public static final String ROLE_STORE_MANAGER = "ROLE_" + STORE_MANAGER;
public static final String ROLE_EMPLOYEE = "ROLE_" + EMPLOYEE;
```

> **Learning note:** Authentication verifies identity (username + password). Authorization verifies permission (what you are allowed to access). A user can authenticate successfully but still receive a 403 Forbidden if they lack the required role for the requested URL.

## The two roles and why the manager is additive

StoreMS has exactly two roles:

1. `EMPLOYEE` — the baseline. Every authenticated user has this role.
2. `STORE_MANAGER` — an additive role. A manager is an employee who also manages a store.

The design is **additive**: every staff member is an employee first, and a subset of those employees also hold the manager role. This is documented in `Roles.java`:

```java
// src/main/java/com/salias/storems/security/Roles.java (lines 6-8)
/**
 * A store manager holds both roles: managers can also work a till, so
 * {@code STORE_MANAGER} is additive rather than an alternative to
 * {@code EMPLOYEE}.
 */
```

The authority assembly happens in `StoremsUserDetailsService` at login time:

```java
// src/main/java/com/salias/storems/security/StoremsUserDetailsService.java (lines 53-55)
List<GrantedAuthority> authorities = new ArrayList<>();
authorities.add(new SimpleGrantedAuthority(Roles.ROLE_EMPLOYEE));
managed.ifPresent(m -> authorities.add(new SimpleGrantedAuthority(Roles.ROLE_STORE_MANAGER)));
```

Every user receives `ROLE_EMPLOYEE`. Only users whose employee ID appears in the `store_manager` table receive the additional `ROLE_STORE_MANAGER`.

Why additive beats "manager OR employee": a manager needs to work the till alongside their management duties. With separate roles, you would need complex logic to grant till access to managers. With additive roles, the URL rule for till paths simply requires `ROLE_EMPLOYEE`, and managers automatically inherit it because they hold both roles.

> **Why it matters:** This pattern scales. New roles can be added without rewriting existing code — just grant the new authority alongside `ROLE_EMPLOYEE`.

## Where roles become row-level facts

Roles are not stored in a separate role table that could drift out of sync. Instead, they are **derived from data** at login. The database encodes privilege through the `StoreManager` entity:

```java
// src/main/java/com/salias/storems/model/storemanager/StoreManager.java (lines 17-22)
/**
 * Assigns one employee as the manager of one store.
 *
 * <p>The primary key is derived from the store, so a store can have at most one
 * manager. The {@code manager_id} unique constraint stops one employee from
 * managing two stores.
 */
```

The entity maps:

- `store_id` as the primary key — a store has at most one manager.
- `manager_id` with `unique = true` — an employee manages at most one store.

```java
// src/main/java/com/salias/storems/model/storemanager/StoreManager.java (lines 36-38)
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "manager_id", nullable = false, unique = true)
private Employee manager;
```

There is no `user_roles` table. At login, `StoremsUserDetailsService` queries `store_manager` to see if the authenticating employee appears there:

```java
// src/main/java/com/salias/storems/security/StoremsUserDetailsService.java (lines 50-55)
Store employedAt = employee.getStoreAssociatedTo();
Optional<StoreManager> managed = storeManagerRepository.findByManager_Id(employee.getId());

List<GrantedAuthority> authorities = new ArrayList<>();
authorities.add(new SimpleGrantedAuthority(Roles.ROLE_EMPLOYEE));
managed.ifPresent(m -> authorities.add(new SimpleGrantedAuthority(Roles.ROLE_STORE_MANAGER)));
```

If a row exists, the manager role is granted. If not, the user remains a plain employee. No separate role assignment step exists, so there is no role drift — the authority is always consistent with the current database state.

## The URL rules

The `SecurityConfig` filter chain defines which roles can access which URL patterns. Here is the complete `authorizeHttpRequests` block:

```java
// src/main/java/com/salias/storems/security/SecurityConfig.java (lines 39-44)
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers(MANAGER_PATHS).hasRole(Roles.STORE_MANAGER)
        .requestMatchers(TILL_PATHS).hasRole(Roles.EMPLOYEE)
        .anyRequest().authenticated())
```

The path arrays are defined as:

```java
// src/main/java/com/salias/storems/security/SecurityConfig.java (lines 24-33)
/** Manager-only areas. Employees get 403 here. */
private static final String[] MANAGER_PATHS = {
        "/dashboard/**", "/inventory/**", "/products/**",
        "/employees/**", "/sales/**", "/reports/**"
};

/** Till and own-sales areas. Managers hold ROLE_EMPLOYEE too, so they can sell. */
private static final String[] TILL_PATHS = {
        "/pos/**", "/my-sales/**"
};
```

Walking through each rule:

1. **First rule (line 40):** `requestMatchers(...).permitAll()` — Static assets, the login page, and error pages are accessible to everyone, authenticated or not.

2. **Second rule (line 41):** `/actuator/health` is also public — useful for load balancers and health checks.

3. **Third rule (line 42):** `requestMatchers(MANAGER_PATHS).hasRole(Roles.STORE_MANAGER)` — The manager paths require `ROLE_STORE_MANAGER`. Only users who appear in the `store_manager` table pass this check.

4. **Fourth rule (line 43):** `requestMatchers(TILL_PATHS).hasRole(Roles.EMPLOYEE)` — The till paths require `ROLE_EMPLOYEE`. Every authenticated user has this role, including managers.

5. **Fifth rule (line 44):** `anyRequest().authenticated()` — Any URL that does not match the earlier patterns requires authentication but has no role requirement. This catches fallback cases.

**Matcher order matters.** Spring Security evaluates rules in declaration order and stops at the first match. Putting `permitAll` rules first ensures public resources are always accessible. Putting specific path rules before `anyRequest()` prevents the catch-all from blocking them. The final `anyRequest().authenticated()` is a deny-by-default — if a URL matches nothing specific, the user must at least be logged in.

If an employee tries to access `/dashboard`, they receive HTTP 403 Forbidden because they lack `ROLE_STORE_MANAGER`. If a manager accesses `/pos`, they succeed because they have `ROLE_EMPLOYEE`.

## Hiding links is not authorisation

The navigation bar hides manager-only links for non-managers. This is done in the layout template using a flag provided by `GlobalModelAdvice`:

```java
// src/main/java/com/salias/storems/controller/GlobalModelAdvice.java (lines 27-31)
@ModelAttribute("isManager")
boolean isManager(Authentication authentication) {
    return authentication != null && authentication.getAuthorities().stream()
            .anyMatch(a -> Roles.ROLE_STORE_MANAGER.equals(a.getAuthority()));
}
```

The template uses this flag:

```html
<!-- src/main/resources/templates/fragments/layout.html (lines 43-55) -->
<span th:if="${isManager}" class="divider"></span>
<a th:if="${isManager}" th:href="@{/dashboard}"
   th:classappend="${active == 'dashboard'} ? 'on'">Dashboard</a>
<a th:if="${isManager}" th:href="@{/inventory}"
   th:classappend="${active == 'inventory'} ? 'on'">Inventory</a>
<!-- ... more manager links ... -->
```

However, the layout file contains a critical comment:

```html
<!-- src/main/resources/templates/fragments/layout.html (lines 20-24) -->
<!--
  Top bar. Navigation links are hidden by role for tidiness only; the actual
  authorisation is the URL rules in SecurityConfig plus store-scoping in every
  handler. A hidden link is not a permission check.
-->
```

The `GlobalModelAdvice` class reinforces this in its Javadoc:

```java
// src/main/java/com/salias/storems/controller/GlobalModelAdvice.java (lines 12-14)
/**
 * Templates use these to hide navigation the user cannot reach. That is
 * cosmetic only — the real gate is the URL rules in {@code SecurityConfig} plus
 * store-scoping in each handler. Hiding a link is not authorisation.
 */
```

Why does this matter? An attacker who knows the URL `/dashboard` can type it directly into the browser and submit a request. The browser will send the request regardless of whether the link is visible in the nav menu. If the URL rule did not exist, the attacker would succeed. The URL rule blocks the request server-side, before any business logic runs. Client-side hiding is a convenience for legitimate users — it is not a security control.

> **Learning note:** Never rely on UI hiding for access control. Always enforce permissions server-side in the URL rules (or deeper, in service-layer checks). The attacker sees the network request, not your HTML.

## Learning notes

- **Least privilege:** Staff see only the till and their own sales (`/pos`, `/my-sales`). Managers see those plus management functions (`/dashboard`, `/inventory`, `/products`, `/employees`, `/sales`, `/reports`). Each role sees the minimum needed for their job.

- **Deny-by-default:** The final `anyRequest().authenticated()` rule ensures that any new URL added to the application is protected by default. Developers must explicitly permit public access.

- **Defense in depth:** The URL rule is the first gate. Later chapters cover service-layer store scoping, which ensures that even if a request sneaks past the URL rule, the handler only returns data from the user's own store.

- **OWASP Broken Access Control (A01):** In the OWASP Top 10, Broken Access Control ranks first. It includes missing authorization, forced browsing to restricted URLs, and privilege escalation. StoreMS addresses this with explicit URL rules, role checks, and no privilege escalation path (additive roles are granted, not earned).

> **Why it matters:** Access control flaws are among the most common and impactful vulnerabilities in web applications. A single missed authorization check can expose sensitive customer data, allow inventory manipulation, or let a regular employee elevate to manager privileges. The URL-level check in `SecurityConfig` is your first line of defense.

---

> **Chapter summary:**
>
> - Roles in Spring Security are authorities with a `ROLE_` prefix; `hasRole("X")` checks for `ROLE_X`.
> - StoreMS uses two additive roles: every user gets `ROLE_EMPLOYEE`, and only managers get `ROLE_STORE_MANAGER` on top.
> - Manager status is derived from the `store_manager` table at login — no separate role table, no drift.
> - The `authorizeHttpRequests` block enforces URL-level access: manager paths need `STORE_MANAGER`, till paths need `EMPLOYEE`, everything else needs authentication.
> - UI navigation hiding via `th:if="${isManager}"` is cosmetic only; the real gate is the URL rule.
> - Deny-by-default (`anyRequest().authenticated()`) and least privilege keep the attack surface small.
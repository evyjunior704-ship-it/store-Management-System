# Part 2 - Authentication

Authentication is the gatekeeper that proves an identity before granting access. In StoreMS, every request to a protected resource must present valid credentials. This chapter traces a login from the HTML form all the way through to the Spring Security `Authentication` object that lives in the session.

> **Learning note:** Authentication is the first of the three core security concepts in OWASP A07:2021 (Identification and Authentication Failures). Authorization (what you can do once logged in) comes after.

## The login flow, start to finish

When a user signs in, several components work in sequence. Each step is handled by a different part of the Spring Security filter chain.

### 1. GET /login renders the form

The unauthenticated user requests the login page. `LoginController` serves it:

```java
// src/main/java/com/salias/storems/controller/LoginController.java:11-14
@GetMapping("/login")
String login() {
    return "login";
}
```

Spring Security is configured to permit access to `/login` without credentials (see `SecurityConfig.java:40`). The controller returns the logical view name `login`, which Thymeleaf resolves to `login.html`.

### 2. User fills the form and POSTs to /login

The browser submits a form with two fields: `username` and `password`. The form in `login.html` sends a POST to `/login`:

```html
<!-- src/main/resources/templates/login.html:29 -->
<form th:action="@{/login}" method="post">
    <div class="field">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" autocomplete="username"
               placeholder="e.g. alopez" required autofocus>
    </div>
    <div class="field">
        <label for="password">Password</label>
        <input type="password" id="password" name="password"
               autocomplete="current-password" placeholder="Your password" required>
    </div>
    <button class="btn btn-lg" type="submit" style="width:100%">Sign in</button>
</form>
```

The `th:action` directive injects a CSRF token into the form automatically (covered in Chapter 6).

### 3. UsernamePasswordAuthenticationFilter intercepts the POST

Spring Security's `UsernamePasswordAuthenticationFilter` sits in the filter chain. It extracts the `username` and `password` parameters from the request and builds an `Authentication` object with `UsernamePasswordAuthenticationToken`.

This token is then passed to the `AuthenticationManager`, which delegates to the configured `AuthenticationProvider`.

### 4. DaoAuthenticationProvider asks the UserDetailsService

The security config wires in Spring's `DaoAuthenticationProvider`, which knows two things: a `UserDetailsService` (to load the user) and a `PasswordEncoder` (to verify the password). The provider calls `StoremsUserDetailsService.loadUserByUsername()`:

```java
// src/main/java/com/salias/storems/security/StoremsUserDetailsService.java:44-48
@Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Employee employee = employeeRepository.findByUsernameWithStore(username)
            // Deliberately vague: do not reveal whether the username exists.
            .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
```

This method queries the database for an employee row matching the username. The query uses a fetch join to load the associated store in a single query:

```java
// src/main/java/com/salias/storems/model/employee/EmployeeRepository.java:17-22
@Query("""
        SELECT e FROM Employee e
        JOIN FETCH e.storeAssociatedTo
        WHERE e.username = :username
        """)
Optional<Employee> findByUsernameWithStore(String username);
```

If no row matches, the exception message is deliberately vague: `"Bad credentials"` rather than "User not found" or "Username does not exist." This prevents username enumeration.

### 5. Password encoder checks the hash

Once the `UserDetailsService` returns a `UserDetails` object (`StoremsPrincipal`), the `DaoAuthenticationProvider` compares the submitted password against the stored hash using the `PasswordEncoder` bean:

```java
// src/main/java/com/salias/storems/security/SecurityConfig.java:69-72
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Spring Security calls `UserDetails.getPassword()` to get the stored hash, runs it through BCrypt with the submitted password as input, and compares the result. BCrypt is a slow, salted hash algorithm specifically designed to resist brute-force attacks.

### 6. On success: AuthenticationSuccessHandler redirects based on role

If the password matches, the `AuthenticationSuccessHandler` determines where to send the user:

```java
// src/main/java/com/salias/storems/security/SecurityConfig.java:59-67
/** Managers land on their dashboard, staff go straight to the till. */
@Bean
AuthenticationSuccessHandler authenticationSuccessHandler() {
    return (request, response, authentication) -> {
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> Roles.ROLE_STORE_MANAGER.equals(a.getAuthority()));
        response.sendRedirect(request.getContextPath() + (isManager ? "/dashboard" : "/pos"));
    };
}
```

- Users with `ROLE_STORE_MANAGER` redirect to `/dashboard`
- Users with only `ROLE_EMPLOYEE` redirect to `/pos`

This happens in `SecurityConfig.java:47`, wired via the `.successHandler(successHandler)` line in the form login configuration.

### 7. On failure: redirect to /login?error

If the password does not match, Spring Security's default behavior redirects back to the login page with an error query parameter:

```java
// src/main/java/com/salias/storems/security/SecurityConfig.java:48
.failureUrl("/login?error")
```

The login template checks for this parameter and displays a generic error:

```html
<!-- src/main/resources/templates/login.html:21-23 -->
<div class="flash flash-err" th:if="${param.error}">
    Incorrect username or password. Try again.
</div>
```

> **Learning note:** The flash message is the same whether the username does not exist OR the password is wrong. This uniformity is intentional — it prevents attackers from distinguishing valid usernames from invalid ones.

---

## No hardcoded credentials

StoreMS ships with zero default accounts. Every login attempt authenticates against a row in the `employee` table.

```java
// src/main/java/com/salias/storems/security/SecurityConfig.java:13-19
/**
 * Form login backed by {@code Employee} rows.
 *
 * <p>There are no hardcoded credentials and no default passwords: every login is
 * an employee with a BCrypt hash in the database. CSRF protection stays enabled
 * because the UI is server-rendered and Thymeleaf injects the token into forms
 * automatically.
 */
```

The password encoder is explicitly BCrypt:

```java
// src/main/java/com/salias/storems/security/SecurityConfig.java:69-72
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Every `Employee` row carries a `password_hash` column containing a BCrypt hash. There is no:

- Hardcoded username like `admin` or `root`
- Default password like `password`, `changeme`, or `store123`
- Vendor backdoor account

> **Why it matters:** OWASP A07:2021 (Identification and Authentication Failures) flags default credentials as a top-ten risk. Attackers scan for default admin accounts on exposed services. StoreMS eliminates this attack surface entirely — the only way in is a legitimate employee record.

---

## The UserDetailsService deep dive

The `StoremsUserDetailsService` is the bridge between Spring Security and the application's data model. It implements `UserDetailsService`, a single-method interface:

```java
// src/main/java/com/salias/storems/security/StoremsUserDetailsService.java:44-74
@Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Employee employee = employeeRepository.findByUsernameWithStore(username)
            // Deliberately vague: do not reveal whether the username exists.
            .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

    Store employedAt = employee.getStoreAssociatedTo();
    Optional<StoreManager> managed = storeManagerRepository.findByManager_Id(employee.getId());

    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(Roles.ROLE_EMPLOYEE));
    managed.ifPresent(m -> authorities.add(new SimpleGrantedAuthority(Roles.ROLE_STORE_MANAGER)));

    // A manager is scoped to the store they manage. Normally that is also where
    // they are employed; if the data disagrees, the managed store wins.
    Store scopedStore = managed
            .map(StoreManager::getStoreId)
            .filter(managedStoreId -> !managedStoreId.equals(employedAt.getStoreId()))
            .flatMap(storeRepository::findById)
            .orElse(employedAt);

    return new StoremsPrincipal(
            employee.getId(),
            scopedStore.getStoreId(),
            scopedStore.getStoreName(),
            "%s %s".formatted(employee.getEmpFirstName(), employee.getEmpLastName()),
            employee.getUsername(),
            employee.getPasswordHash(),
            employee.isEnabled(),
            authorities);
}
```

### Why the vague error message

```java
.orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
```

If the username is not found, Spring Security throws `"Bad credentials"` — the same message it throws if the password is wrong. This prevents **username enumeration**, where an attacker tries different usernames and watches for "User not found" vs "Wrong password" to discover valid accounts.

### The fetch-joined JPQL query

```java
@Query("""
        SELECT e FROM Employee e
        JOIN FETCH e.storeAssociatedTo
        WHERE e.username = :username
        """)
Optional<Employee> findByUsernameWithStore(String username);
```

The `JOIN FETCH` clause loads the associated `store` entity in the same SQL query, avoiding the N+1 problem. Without it, accessing `employee.getStoreAssociatedTo()` would trigger a lazy load and execute a second query per authentication.

### Role derivation from data, not config

StoreMS does not store roles in a separate table or enum. Instead, roles are derived from whether the employee appears in the `store_manager` table:

```java
List<GrantedAuthority> authorities = new ArrayList<>();
authorities.add(new SimpleGrantedAuthority(Roles.ROLE_EMPLOYEE));
managed.ifPresent(m -> authorities.add(new SimpleGrantedAuthority(Roles.ROLE_STORE_MANAGER)));
```

Every authenticated user gets `ROLE_EMPLOYEE`. If the employee's ID appears in the `store_manager` table, they additionally receive `ROLE_STORE_MANAGER`. This is additive: a manager can both manage a store AND work a till.

### The "managed store wins" rule

```java
// A manager is scoped to the store they manage. Normally that is also where
// they are employed; if the data disagrees, the managed store wins.
Store scopedStore = managed
        .map(StoreManager::getStoreId)
        .filter(managedStoreId -> !managedStoreId.equals(employedAt.getStoreId()))
        .flatMap(storeRepository::findById)
        .orElse(employedAt);
```

If a manager is assigned to manage Store A but is employed at Store B, the system uses Store A (the managed store). This ensures managers always see data for the store they control.

---

## The principal object

When authentication succeeds, Spring Security stores a **principal** — an object that represents the logged-in user. In StoreMS, this is `StoremsPrincipal`:

```java
// src/main/java/com/salias/storems/security/StoremsPrincipal.java:16
public class StoremsPrincipal implements UserDetails, Serializable {
```

`StoremsPrincipal` implements `org.springframework.security.core.userdetails.UserDetails`, the Spring Security contract for a user record. This interface requires several methods:

```java
// src/main/java/com/salias/storems/security/StoremsPrincipal.java:73-76
@Override
public String getPassword() {
    return passwordHash;
}
```

`getPassword()` returns the BCrypt hash, not the plaintext. During authentication, the `DaoAuthenticationProvider` reads this value and runs the submitted password through BCrypt to compare.

```java
// src/main/java/com/salias/storems/security/StoremsPrincipal.java:84-86
@Override
public boolean isEnabled() {
    return enabled;
}
```

`isEnabled()` comes directly from the `employee.isEnabled()` database column. If an employee is disabled in the database, they cannot log in — Spring Security treats disabled accounts as locked.

The other `UserDetails` methods return `true` by default:

```java
// src/main/java/com/salias/storems/security/StoremsPrincipal.java:88-101
@Override
public boolean isAccountNonExpired() { return true; }

@Override
public boolean isAccountNonLocked() { return true; }

@Override
public boolean isCredentialsNonExpired() { return true; }
```

StoreMS does not implement account expiration, locking, or credential expiration — these methods satisfy the interface contract but always return `true`.

The principal also carries business data beyond what Spring Security requires:

```java
// src/main/java/com/salias/storems/security/StoremsPrincipal.java:20-27
private final Long employeeId;
private final Long storeId;
private final String storeName;
private final String displayName;
private final String username;
private final String passwordHash;
private final boolean enabled;
private final List<GrantedAuthority> authorities;
```

Controllers and services can inject the `Authentication` object and cast it to `StoremsPrincipal` to access `getEmployeeId()`, `getStoreId()`, and `getStoreName()` without querying the database again. Every query is automatically scoped to the user's store.

---

## The login page UX and CSRF

The login form balances usability with security.

### Autocomplete attributes

```html
<input type="text" id="username" name="username" autocomplete="username"
       placeholder="e.g. alopez" required autofocus>
<input type="password" id="password" name="password"
       autocomplete="current-password" placeholder="Your password" required>
```

- `autocomplete="username"` tells the browser to offer stored usernames
- `autocomplete="current-password"` tells the browser to offer the password saved for this site

This is not merely a convenience feature. Modern browsers refuse to fill `autocomplete="new-password"` or omit the attribute in non-secure contexts (HTTP). By explicitly marking these fields, the browser knows this is a login form and can protect the credentials appropriately.

> **Learning note:** Older implementations used `autocomplete="off"` to prevent password managers from filling fields. This is now considered an anti-pattern — it confuses users, and browsers increasingly ignore it. The current guidance is to use the correct `autocomplete` values so browsers and password managers can help rather than hinder.

### Flash messages

```html
<div class="flash flash-err" th:if="${param.error}">
    Incorrect username or password. Try again.
</div>
<div class="flash flash-ok" th:if="${param.loggedOut}">
    You have been signed out. See you next shift.
</div>
```

- On failure: a generic "Incorrect username or password" message, same regardless of whether the username exists
- On logout: a friendly "You have been signed out" confirmation

The error message does not distinguish between wrong username, wrong password, disabled account, or missing user. This is the same principle as the vague `UsernameNotFoundException` — prevent enumeration.

### CSRF protection

```html
<!-- th:action makes Thymeleaf inject the CSRF token automatically. -->
<form th:action="@{/login}" method="post">
```

The `th:action` Thymeleaf attribute automatically injects a hidden CSRF token field into the rendered HTML:

```html
<input type="hidden" name="_csrf" value="a1b2c3d4-e5f6-...">
```

Every POST, PUT, and DELETE request includes this token. The server validates it on receipt. Without a valid token, the request is rejected. This prevents cross-site request forgery, where a malicious site tricks a logged-in user into submitting a request to StoreMS without their knowledge.

> **Learning note:** CSRF is covered in detail in Chapter 6, but the key point here is that it is automatic. Thymeleaf handles it for every form, and Spring Security validates it on every state-changing request.

---

## Learning notes

> **Learning note:** `UserDetails` is Spring Security's representation of a user. It is an interface with methods like `getUsername()`, `getPassword()`, `getAuthorities()`, and flags like `isEnabled()`. Your implementation (`StoremsPrincipal`) provides the actual values from your database.

> **Learning note:** `UserDetailsService` is the interface for loading user data. It has one method: `loadUserByUsername(String username)`. Spring Security calls this during authentication to fetch the user record. You implement it to bridge between Spring Security and your data model.

> **Learning note:** `DaoAuthenticationProvider` is the workhorse. It combines two components you must provide: a `UserDetailsService` (to load the user) and a `PasswordEncoder` (to verify the password). When you configure form login, Spring wires these together automatically.

> **Learning note:** Never store plaintext passwords. StoreMS uses BCrypt, a slow, salted hash. If the database is leaked, attackers cannot easily reverse the hashes. The plaintext password is never stored — only the hash is persisted, and the encoder verifies by re-hashing the input and comparing.

> **Learning note:** Username enumeration is an information disclosure attack. If your login says "User not found" for invalid usernames but "Wrong password" for valid ones, an attacker can discover which usernames exist in your system. StoreMS avoids this by using identical messages for all failures.

> **Why it matters:** OWASP A07:2021 (Identification and Authentication Failures) specifically calls out verbose error messages as a vector for username enumeration. Generic, identical error responses are a defensive control against this class of attack. StoreMS implements this at both the service layer (the exception message) and the UI layer (the flash text).

---

## Chapter summary

- The login flow runs through `UsernamePasswordAuthenticationFilter` → `DaoAuthenticationProvider` → `UserDetailsService` → `PasswordEncoder`, ending with a redirect based on the user's role.
- StoreMS has no hardcoded credentials — every login queries the `employee` table for a BCrypt hash.
- `StoremsUserDetailsService.loadUserByUsername()` returns a `StoremsPrincipal` with authorities derived from the `store_manager` table, not from configuration.
- `StoremsPrincipal` implements `UserDetails`, providing the password hash via `getPassword()` and account status via `isEnabled()`.
- The login form uses `autocomplete` attributes for browser password-manager integration and displays identical error messages to prevent username enumeration.
- CSRF protection is automatic — Thymeleaf injects the token, and Spring Security validates it on every POST.
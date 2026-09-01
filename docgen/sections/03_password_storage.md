# Part 3 - Password Storage

## Why hashes, not passwords

Never store a plaintext password. This is the most repeated rule in application security, and for good reason. When you store a password as-is, anyone who gains read access to your database obtains every credential immediately. The damage does not stop at your application - users frequently reuse passwords across banking, email, and work accounts, so a breach of your database creates risk far beyond your store management system.

A **one-way hash** is a cryptographic function that transforms input (the password) into a fixed-length output (the hash) that cannot be reversed. You compute the hash when the user creates or changes their password, store only the hash, and when they log in you hash what they type and compare the results. The server never knows the original password.

> **Learning note:** A standard cryptographic hash (SHA-256, MD5, SHA-1) is designed for speed - it processes data in microseconds. This is the opposite of what you want for passwords. A password cracker can try billions of hashes per second on GPU hardware. Password hashing algorithms like BCrypt are deliberately slow, using thousands of iterations to make each guess expensive.

The distinction between cryptographic hashes and password hashing algorithms matters. SHA-256 treats every input independently - the same password always produces the same hash. An attacker with a precomputed **rainbow table** (a mapping of common passwords to their SHA-256 hashes) can look up your entire password column in seconds. Password hashing algorithms solve this by including a unique **salt** per password and running thousands of rounds of computation.

If StoreMS stored plaintext passwords in the database and a data breach occurred, attackers would immediately have:

- Usernames and passwords that work on your system
- Credentials that likely work on other services (credential stuffing attacks)
- Employee addresses and personal information from the employee table

The Employee model makes this explicit in its Javadoc:

```java
/** BCrypt hash. Never holds a plaintext password. */
@Column(name = "password_hash", length = 100)
private String passwordHash;
```

See `Employee.java:74-76`. The column is named `password_hash`, not `password`, reinforcing that only a hash ever resides there. The field `username` at line 71 is nullable by design - most employees can exist without system access.

> **Why it matters:** OWASP Identification and Authentication Failures (A01:2021) lists "storing passwords using insecure and reversible encryption" as a critical vulnerability. Using proper password hashing is one of the most effective defenses you can implement.

---

## BCrypt in one screen

Spring Security provides BCrypt support out of the box. In StoreMS, the encoder is configured as a Spring bean in SecurityConfig:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

See `SecurityConfig.java:69-72`. This single bean is injected wherever password encoding is needed - in EmployeeService, in DemoDataSeeder, and internally by Spring Security's authentication infrastructure.

BCrypt differs from simple hash functions in three critical ways:

1. **Per-password random salt**: BCrypt generates a 128-bit random salt for each password. The same plaintext password produces different hashes every time. This defeats rainbow tables entirely - an attacker must crack each hash individually.

2. **Configurable work factor**: BCrypt's cost parameter (the "work factor") controls how many iterations the algorithm runs. The default of 10 means 2^10 = 1024 rounds. Each round doubles the computation time. A single BCrypt hash takes roughly 100 milliseconds on modern hardware - fast enough for login, slow enough to make brute force impractical.

3. **Adaptive algorithm**: As hardware improves, you can increase the cost factor. BCrypt stores the cost in the hash output, so existing hashes remain valid while new passwords use the current cost.

The BCrypt hash format is recognizable in the database:

```
$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWX
```

- `$2a$` identifies the BCrypt version
- `10$` is the cost factor (work factor)
- The remaining 53 characters encode the 22-character salt and the 31-character hash

The column length of 100 in Employee.java:75 provides headroom. BCrypt produces exactly 60 characters, but the format may change to `$2b$` or `$2y$` in future versions, and allowing extra space ensures database migrations are unnecessary later.

---

## The credential lifecycle in code

Credentials change in four distinct scenarios in StoreMS. Understanding each path ensures you know exactly when and how passwords are hashed.

### Creating an employee with access

When a manager creates a new employee via the web UI, the request hits EmployeeController.create:

```java
@PostMapping("/employees")
String create(@RequestParam String firstName,
              @RequestParam String lastName,
              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
              @RequestParam String address,
              @RequestParam String role,
              @RequestParam(required = false) String username,
              @RequestParam(required = false) String password,
              RedirectAttributes flash) {
    try {
        employeeService.create(CurrentUser.storeId(), firstName, lastName, dob,
                address, role, username, password);
        flash.addFlashAttribute("success",
                "%s %s added to this store.".formatted(firstName, lastName));
    } catch (IllegalArgumentException e) {
        flash.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/employees";
}
```

See `EmployeeController.java:43-61`. This endpoint accepts optional `username` and `password` parameters. When both are provided, the employee receives system access immediately.

The service layer handles the actual work:

```java
@Transactional
public Employee create(Long storeId,
                       String firstName,
                       String lastName,
                       LocalDate dob,
                       String address,
                       String role,
                       String username,
                       String rawPassword) {
    Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("No store with id " + storeId));

    Employee employee = new Employee(firstName, lastName, dob, address, role, store);
    applyCredentials(employee, username, rawPassword);
    return employeeRepository.save(employee);
}
```

See `EmployeeService.java:57-72`. The `applyCredentials` method (lines 149-169) is the core logic shared by both creation and updates.

### Setting or changing credentials

When a manager updates an employee's access (either during creation or later), this endpoint handles it:

```java
@PostMapping("/employees/{id}/access")
String setAccess(@PathVariable Long id,
                 @RequestParam String username,
                 @RequestParam(required = false) String password,
                 RedirectAttributes flash) {
    try {
        employeeService.setCredentials(CurrentUser.storeId(), id, username, password);
        flash.addFlashAttribute("success", "System access updated.");
    } catch (IllegalArgumentException e) {
        flash.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/employees/" + id;
}
```

See `EmployeeController.java:76-88`. Note that the password field is `required = false` - this allows updating just the username without changing the password.

The service method:

```java
/** Sets or replaces login credentials. A blank password leaves the existing one. */
@Transactional
public Employee setCredentials(Long storeId, Long employeeId, String username, String rawPassword) {
    Employee employee = findAtStore(storeId, employeeId);
    applyCredentials(employee, username, rawPassword);
    return employee;
}
```

See `EmployeeService.java:91-97`. The crucial behavior is in `applyCredentials`:

```java
private void applyCredentials(Employee employee, String username, String rawPassword) {
    if (username == null || username.isBlank()) {
        return;
    }
    String trimmed = username.trim();

    boolean takenBySomeoneElse = !trimmed.equals(employee.getUsername())
            && employeeRepository.existsByUsername(trimmed);
    if (takenBySomeoneElse) {
        throw new IllegalArgumentException("Username '%s' is already taken".formatted(trimmed));
    }

    String hash = (rawPassword == null || rawPassword.isBlank())
            ? employee.getPasswordHash()
            : passwordEncoder.encode(rawPassword);

    if (hash == null) {
        throw new IllegalArgumentException("A password is required when granting system access");
    }
    employee.setCredentials(trimmed, hash);
}
```

See `EmployeeService.java:149-169`. This method implements two important behaviors:

1. **Uniqueness guard**: The call to `employeeRepository.existsByUsername(trimmed)` ensures no two employees claim the same username. See line 155-156. The check excludes the employee currently being updated (the `!trimmed.equals(employee.getUsername())` part).

2. **Blank password preserves existing hash**: When `rawPassword` is null or blank, the code reuses the existing hash (lines 161-163). This allows a manager to change a username without forcing a password reset, or to set a new password without touching the username. If a username is provided but password is blank, the old password remains valid.

### Revoking access

When an employee leaves or loses system access, the manager can revoke credentials:

```java
@PostMapping("/employees/{id}/revoke-access")
String revokeAccess(@PathVariable Long id, RedirectAttributes flash) {
    employeeService.revokeAccess(CurrentUser.storeId(), id);
    flash.addFlashAttribute("success", "System access revoked. The employee record is kept.");
    return "redirect:/employees/" + id;
}
```

See `EmployeeController.java:90-95`. The service implementation:

```java
@Transactional
public Employee revokeAccess(Long storeId, Long employeeId) {
    Employee employee = findAtStore(storeId, employeeId);
    employee.revokeCredentials();
    return employee;
}
```

See `EmployeeService.java:99-104`. The Employee model sets both fields to null:

```java
/** Revokes system access without deleting the employee record. */
public void revokeCredentials() {
    this.username = null;
    this.passwordHash = null;
}
```

See `Employee.java:177-181`. The employee record persists for audit and reference purposes, but the login credentials are completely removed. Spring Security's authentication will fail because the employee has no hash to compare against.

---

## Access management from the manager UI

The employee detail page exposes two forms for credential management. Both use POST and include CSRF tokens (Thymeleaf injects these automatically).

### Grant or update access form

```html
<form th:action="@{'/employees/' + ${employee.id} + '/access'}" method="post" class="row">
    <div>
        <label for="username">Username</label>
        <input type="text" id="username" name="username"
               th:value="${employee.username}" autocomplete="off" required>
    </div>
    <div>
        <label for="password">New password</label>
        <input type="password" id="password" name="password"
               autocomplete="new-password" placeholder="leave blank to keep current">
    </div>
    <div class="shrink"><button class="btn" type="submit">Update access</button></div>
</form>
```

See `employee-detail.html:69-81`. The password field explicitly says "leave blank to keep current" and the placeholder guides the user. The autocomplete attributes prevent browsers from filling unexpected values.

### Revoke access form

```html
<form th:action="@{'/employees/' + ${employee.id} + '/revoke-access'}"
      method="post" style="margin-top:14px" th:if="${employee.username}">
    <button class="btn btn-sm btn-danger" type="submit">Revoke access</button>
    <span class="hint">Removes the login. The employee record is kept.</span>
</form>
```

See `employee-detail.html:83-87`. The form only appears when the employee currently has access (`th:if="${employee.username}"`). This prevents confusing empty states.

### HTTP surface summary

| Endpoint | Method | Parameters | Purpose |
|----------|--------|------------|---------|
| `/employees` | POST | firstName, lastName, dob, address, role, username, password | Create employee |
| `/employees/{id}/access` | POST | username, password | Set or update credentials |
| `/employees/{id}/revoke-access` | POST | (none) | Remove credentials |

All three endpoints require the manager role. Spring Security's CSRF protection is enabled - every form submission includes a hidden `_csrf` token that Spring validates on receipt.

---

## Demo data sharp edges

The DemoDataSeeder is a useful development tool, but it contains password behaviors you must never replicate in production:

```java
/**
 * A small, realistic dataset for exercising the UI and the two roles.
 *
 * <p>Run with {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=demoseed}.
 *
 * <p>Unlike the bulk seeder, this produces a shape you can actually log into:
 * three stores, each with one manager and four staff, so manager-only screens and
 * employee-only screens are both reachable. Every login shares one password,
 * which is why this profile is for local use only.
 */
```

See `DemoDataSeeder.java:36-44`. The Javadoc explicitly warns "for local use only."

### Shared password across all accounts

```java
@Value("${storems.demo.password:demo1234}")
private String demoPassword;
```

See `DemoDataSeeder.java:114-115`. Every seeded account gets the same password. At line 146, the hash is computed once:

```java
String hash = passwordEncoder.encode(demoPassword);
```

Then applied to every manager and staff account. See lines 160 and 178. This means if you deploy with this profile and do not change passwords, anyone who guesses "demo1234" can log in as any seeded employee.

### The re-seeding guard

```java
@Override
@Transactional
public void run(ApplicationArguments args) {
    if (storeRepository.count() > 0) {
        log.warn("Data already present ({} stores). Demo seeder refusing to run; "
                + "truncate the tables first.", storeRepository.count());
        return;
    }
    // ... seeding continues
}
```

See `DemoDataSeeder.java:139-144`. The seeder checks for existing data and refuses to run twice. This prevents accidental duplication but does not protect against a fresh database. You must explicitly truncate tables to re-seed.

> **Learning note:** Shared default passwords are a serious anti-pattern in production. In a real deployment, each user's initial password must be unique, expire on first use, or require immediate change. The DemoDataSeeder is acceptable only because it targets a disposable development environment. The warning in the Javadoc and the refusal to re-seed are mitigations, but the fundamental risk remains if someone mistakenly activates this profile in staging or production.

---

## Learning notes

> **Learning note:** Salting defeats rainbow tables because each password gets a unique random salt. Even if two users choose "password123", their BCrypt hashes look completely different. The attacker must crack each hash separately, multiplying their effort by the number of accounts.

> **Learning note:** The BCrypt work factor controls computational cost. At cost 10, a single hash takes about 100ms. At cost 12, it takes 400ms. At cost 8, it takes 25ms. The deliberate slowness frustrates brute force attacks while remaining imperceptible to users logging in once per session.

> **Learning note:** When Spring Security authenticates, it does not compare hashes manually. The `DaoAuthenticationProvider` calls `passwordEncoder.matches(rawPassword, storedHash)` internally. This comparison is constant-time - it takes the same duration regardless of where (or whether) the mismatch occurs. This prevents timing attacks where attackers measure how long the server takes to reject a password to extract information about the hash.

> **Why it matters:** The OWASP Password Storage Cheat Sheet recommends:
>
> - Use adaptive, salted hashing algorithms (BCrypt, Argon2id, scrypt, PBKDF2)
> - Use a work factor that takes 100-1000ms
> - Never use reversible encryption for passwords
> - Consider adding a server-side pepper (a secret key not stored in the database) for additional defense-in-depth
> - Argon2id is the modern winner if your platform supports it; BCrypt remains acceptable but Argon2id is preferred for new projects

---

### Column and field reference

| Location | Field/Bean | Type | Length | Notes |
|----------|------------|------|--------|-------|
| `Employee.java:71` | username | String | 60 | Nullable; unique where present |
| `Employee.java:75` | passwordHash | String | 100 | Holds BCrypt hash only |
| `SecurityConfig.java:70` | passwordEncoder | BCryptPasswordEncoder | Bean | Spring-managed singleton |

> **Chapter summary:**
>
> - StoreMS never stores plaintext passwords - only BCrypt hashes in the password_hash column
> - BCrypt provides per-password random salt, configurable work factor, and adaptive algorithm design
> - Credentials change through four code paths: creation, username-only update, password change, and revocation
> - The applyCredentials method reuses the existing hash when password is blank, enabling username changes without forced password reset
> - The demo seeder uses a shared password - this is acceptable only for local development, never production
> - Spring Security handles hash comparison using constant-time comparison via the BCryptPasswordEncoder.matches method
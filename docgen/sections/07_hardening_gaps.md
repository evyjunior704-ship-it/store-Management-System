# Part 7 - Hardening, Testing, and Known Gaps

## Turning errors into pages, not stack traces — the @ControllerAdvice pattern

Spring MVC exposes a single extension point for centralising exception handling: `@ControllerAdvice`. The `WebExceptionHandler` class at `C:\Users\evyju\OneDrive\Documents\codes\storeMS\storems\src\main\java\com\salias\storems\controller\WebExceptionHandler.java` demonstrates the pattern. Every domain exception maps to a friendly error page with a correct HTTP status code; raw stack traces never reach the browser.

```java
package com.salias.storems.controller;

import com.salias.storems.security.AccessDeniedForStoreException;
import com.salias.storems.service.InsufficientStockException;
import com.salias.storems.service.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Turns domain exceptions into pages rather than stack traces.
 */
@ControllerAdvice
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("title", "Not found");
        model.addAttribute("message", ex.getMessage());
        return "error/message";
    }

    @ExceptionHandler(AccessDeniedForStoreException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String wrongStore(AccessDeniedForStoreException ex, Model model) {
        log.warn("Cross-store access attempt: {}", ex.getMessage());
        model.addAttribute("title", "Not your store");
        model.addAttribute("message", ex.getMessage());
        return "error/message";
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String insufficientStock(InsufficientStockException ex, Model model) {
        model.addAttribute("title", "Not enough stock");
        model.addAttribute("message", ex.getMessage());
        return "error/message";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badRequest(IllegalArgumentException ex, Model model) {
        model.addAttribute("title", "That request could not be processed");
        model.addAttribute("message", ex.getMessage());
        return "error/message";
    }
}
```

Key points a student should internalise:

- **Correct HTTP semantics**: `ResourceNotFoundException` → 404, `AccessDeniedForStoreException` → 403, `InsufficientStockException` → 409 (conflict), `IllegalArgumentException` → 400. Returning 200 with an error message body is an anti-pattern; scanners and proxies rely on status codes.
- **Audit trail on forbidden access**: line 33 logs a `WARN` entry with the exception message. This creates an observable signal for SIEM rules without exposing internals to the caller.
- **Shared view, no leakage**: all handlers return `error/message` (line 27, 36, 44, 52). The template at `C:\Users\evyju\OneDrive\Documents\codes\storeMS\storems\src\main\resources\templates\error\message.html` renders only the `title` and `message` model attributes — no stack trace, no class names, no SQL fragments, no file paths.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/layout :: head(${title})}"></head>
<body>
<header th:replace="~{fragments/layout :: topbar('')}"></header>
<main>
    <div class="panel">
        <h1 th:text="${title}">Something went wrong</h1>
        <p class="sub" th:text="${message}">Details</p>
        <div class="actions">
            <a class="btn btn-ghost" th:href="@{/}">Back to start</a>
        </div>
    </div>
</main>
</body>
</html>
```

> **Why it matters:** Raw exception pages leak framework versions, library internals, SQL schema fragments, and absolute file paths — all intelligence an attacker uses to tailor exploits. "Show message, hide trace" is the baseline hygiene rule.

## Input validation as a security control — Bean Validation annotations on Employee

The `Employee` entity at `C:\Users\evyju\OneDrive\Documents\codes\storeMS\storems\src\main\java\com\salias\storems\model\employee\Employee.java` carries Bean Validation (JSR 380) annotations directly on its fields. The `spring-boot-starter-validation` dependency declared in `pom.xml` (lines 26-29) activates the validator.

```java
    @NotBlank
    @Column(name = "emp_first_name", nullable = false, length = 120)
    private String empFirstName;

    @NotBlank
    @Column(name = "emp_last_name", nullable = false, length = 120)
    private String empLastName;

    @Past
    @NotNull
    @Column(name = "emp_dob", nullable = false)
    private LocalDate empDob;

    @NotBlank
    @Column(name = "emp_address", nullable = false, length = 255)
    private String empAddress;

    @NotBlank
    @Column(name = "emp_role", nullable = false, length = 80)
    private String empRole;
```

- `@NotBlank` (lines 41, 45, 54, 58) rejects `null`, empty string, and whitespace-only input.
- `@NotNull` (line 50) combined with `@Past` (line 49) ensures the date of birth exists and is strictly before today — a domain rule that also blocks malformed payloads such as future dates or the Unix epoch.
- `@Column(length = ...)` provides a database-level guard but is not a substitute for validation; the annotations enforce the same limits at the application boundary.

Validation runs at two enforcement points automatically:
1. **Spring MVC** — when a `@Valid` `@ModelAttribute` or `@RequestBody` is bound, the validator runs before the controller method executes.
2. **JPA** — `spring.jpa.properties.javax.persistence.validation.mode=auto` (default) triggers validation on `persist`/`merge`.

A robust API would also validate at the boundary (DTO layer) so that internal entities are never polluted by invalid data. Here the entity *is* the form-backing object; the framework enforcement points are the only gates.

> **Learning note:** Validation is a data-integrity control *and* a thin line against malformed input. It does not replace output encoding (XSS defence) — those are orthogonal layers.

## Secrets and configuration hygiene

The main `application.properties` at `C:\Users\evyju\OneDrive\Documents\codes\storeMS\storems\src\main\resources\application.properties` reads every credential from the environment; no secret lives in source control.

```
# ===============================
# = DATA SOURCE CONFIGURATION
# ===============================
# Credentials are read from the environment. See README.md for setup.
spring.datasource.url=${STOREMS_DB_URL:jdbc:postgresql://localhost:5432/storems}
spring.datasource.username=${STOREMS_DB_USER:postgres}
spring.datasource.password=${STOREMS_DB_PASSWORD}

# ===============================
# = JPA / HIBERNATE CONFIGURATION
# ===============================
# `update` is convenient during development only. Use a migration tool
# (Flyway/Liquibase) before deploying anywhere real.
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

- Line 7: `STOREMS_DB_PASSWORD` has **no default**. The application fails to start unless the variable is set — a forcing function against accidental commits.
- Lines 5-6: URL and username have dev defaults for convenience, but the comment on line 4 makes the intent explicit.
- Lines 12-13: The migration comment flags `ddl-auto=update` as development-only. In production, `update` lets Hibernate silently `ALTER TABLE` on startup — it can merge incompatible types, drop columns it thinks are unused, and bypass ACL reviews. A migration tool (Flyway/Liquibase) gives versioned, reviewable, rollbackable schema changes.
- Line 15: `show-sql=true` writes every SQL statement to the application log. In production this can leak PII, order amounts, and credential-bearing queries to log aggregation systems. It should be `false` outside dev.

Tests use an in-memory H2 database so no real credentials are ever needed in CI. The test properties at `C:\Users\evyju\OneDrive\Documents\codes\storeMS\storems\src\test\resources\application.properties`:

```
# Tests run against an in-memory H2 database so they need no local Postgres
# and no credentials.
spring.datasource.url=jdbc:h2:mem:storems;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

- Lines 3-5: H2 with empty password — zero secret surface in the test runtime.
- Line 8: `create-drop` gives a clean schema per test run (no migration drift).
- Line 9: SQL logging off by default in tests.

## How this app is tested

Both service test classes use `@SpringBootTest` with `@Transactional` (rollback after each test). The test files are:
- `C:\Users\evyju\OneDrive\Documents\codes\storeMS\storems\src\test\java\com\salias\storems\service\SalesServiceTest.java` (lines 1-107)
- `C:\Users\evyju\OneDrive\Documents\codes\storeMS\storems\src\test\java\com\salias\storems\service\StoreServiceTest.java` (lines 1-81)

They exercise business logic (stock deduction, cross-store rejection, manager assignment) but do **not** yet use `spring-security-test` annotations such as `@WithMockUser`. The dependency is declared in `pom.xml` (lines 63-67):

```xml
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
```

A security-aware test suite would add:
- `@WithMockUser(roles = "STORE_MANAGER")` / `@WithMockUser(roles = "EMPLOYEE")` on test methods to simulate authenticated principals.
- Assertions that forbidden routes return 403 (e.g., an employee hitting `/employees`).
- Cross-store access tests: a manager authenticated for store A attempting to mutate store B data should receive `AccessDeniedForStoreException`.
- `CurrentUser` principal injection tests verifying `assertOwnStore()` behaviour.

`spring-security-test` makes this straightforward — it auto-configures a `TestSecurityContextHolder` so principals are trivial to inject.

## Known gaps

A candid list with citations so a student can treat each as homework.

| # | Gap | Evidence |
|---|-----|----------|
| 1 | **No per-account lockout / rate-limiting** | `SecurityConfig` (not shown here) configures form login but no `AuthenticationFailureHandler` with attempt counting, no `UserDetails` `isAccountNonLocked()` override — the `Employee` entity has no `failedAttempts` or `lockedUntil` fields. |
| 2 | **`ddl-auto=update` + `show-sql=true` in main properties** | `application.properties` lines 14-15 — development defaults committed to the main config file. |
| 3 | **Demo seeder shares one password across accounts** | `README.md` line 65: "Every login shares one demo password — useful for exercising both roles in the UI." Deliberate and profile-gated (`demoseed`), but a foot-gun if the profile is accidentally activated against a real DB. |
| 4 | **No remember-me, no explicit session timeout** | `SecurityConfig` does not call `rememberMe()` or `sessionManagement().maximumSessions(...)` / `invalidSessionUrl(...)` — relies on servlet-container defaults (typically 30 min). |
| 5 | **`actuator/health` permitAll matcher present but actuator not on classpath** | `SecurityConfig` contains `.requestMatchers("/actuator/health").permitAll()` but `spring-boot-starter-actuator` is absent from `pom.xml`. The matcher is inert — a judgement call (explicit allow-list) not a bug. |
| 6 | **Default Spring Security headers only (not customised)** | No `headers()` DSL in `SecurityConfig` — defaults include `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`, `Referrer-Policy: no-referrer`. No CSP, no `Permissions-Policy`, no HSTS directive. |

> **Learning note:** OWASP A05:2021 Security Misconfiguration — default credentials, unnecessary features enabled, verbose errors, missing headers. Every line above is a misconfiguration waiting for a deploy.

> **Why it matters:** Defense in depth means layers. Validation, error handling, secret hygiene, and tests are *layers* — none is sufficient alone.

## Chapter summary

> **Chapter summary:** (1) Centralised `@ControllerAdvice` maps every domain exception to a friendly page with correct HTTP status; the 403 handler logs a WARN for audit. (2) Bean Validation on the entity (`@NotBlank`, `@Past`, `@NotNull`) provides a thin but real line against malformed input at both MVC and JPA enforcement points. (3) Main config reads all secrets from env vars — password has no default; dev defaults for DDL and SQL logging are explicitly flagged. (4) Tests use H2 with no credentials; `spring-security-test` is on the classpath but not yet used for authz assertions. (5) Six known gaps — lockout, dev DDL/SQL, shared demo password, missing remember-me/session policy, inert actuator matcher, default-only security headers — each cited to the exact line that proves the gap exists.
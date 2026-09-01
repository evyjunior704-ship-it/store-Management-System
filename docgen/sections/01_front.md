# StoreMS Security Playbook

## Purpose and audience

This playbook teaches cybersecurity through the lens of a real, working application. Every concept we discuss lands in concrete code you can read, trace, and experiment with.

### Who this playbook is for

You are a cybersecurity student, junior developer, or anyone building their first secure web application. You have basic familiarity with Java, Spring, and HTML, but you want to understand *why* security measures matter and *how* they actually work.

### How to read this playbook

Follow the three-step pattern:

1. **Concept first** — We introduce a security principle in plain terms: what threat it counters, why it matters in a store-management system.
2. **Actual code next** — We show the implementation in StoreMS, line by line. You will see exactly where the defense lives.
3. **Why it matters** — We connect back to the threat and explain what would happen without this protection.

This pattern repeats across every chapter. By the end, you will have traced authentication, authorization, session management, and data scoping from browser to database.

### How code references work

Every time we cite code, we use the format `file:line`. This means "look at the cited file at that specific line number." For example, `SecurityConfig.java:71` points to the BCrypt password encoder definition in the SecurityConfig class. Open the file, find the line, and read the surrounding context.

> **Learning note:** You will get the most out of this playbook if you open the referenced files and follow along. Security is not abstract — it lives in specific lines of code that you can read, modify, and test.

---

## Project at a glance

StoreMS is a store management system for retail businesses that operate multiple locations. It provides point-of-sale (POS) terminals, inventory tracking, employee scheduling, sales reporting, and role-based access control.

### What StoreMS does

- **Point of Sale** — Staff log sales against their assigned store, with real-time inventory deduction.
- **Inventory Management** — Managers track stock levels, add products, and run low-stock reports.
- **Employee Management** — Managers add staff, assign roles, and view performance.
- **Sales Reporting** — Dashboards show daily totals, trends, and shift-by-shift breakdowns.

### Tech stack

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 4.0.1 |
| Language | Java | 21 |
| Security | Spring Security | (via Spring Boot) |
| Template Engine | Thymeleaf | (via Spring Boot) |
| ORM | Spring Data JPA / Hibernate | (via Spring Boot) |
| Database | PostgreSQL | Runtime |
| PDF Reports | OpenPDF | 2.0.3 |
| Build Tool | Maven | 3.x |

This is extracted from the Maven POM file:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
</parent>
<groupId>com.salias</groupId>
<artifactId>storems</artifactId>
```

> **Why it matters:** Spring Boot 4.0.1 brings modern security defaults. The form login, CSRF tokens, and session management we configure in later chapters build on these defaults. Understanding the stack helps you know what security features come "for free" versus what requires explicit configuration.

### Architecture shape

StoreMS follows a classic server-rendered web architecture:

1. **Browser** submits forms (login, sales entry, inventory updates) to the server.
2. **Thymeleaf** templates render HTML on the server, injecting CSRF tokens automatically.
3. **Spring MVC controllers** handle HTTP requests, delegate to services, and return view names.
4. **JPA repositories** map to PostgreSQL tables, with Hibernate managing the schema.
5. **Spring Security** intercepts every request, enforcing authentication and authorization before controllers execute.

```
Browser <-> Thymeleaf Templates <-> Spring MVC Controllers <-> Services <-> JPA/PostgreSQL
                                                      ^
                                                      |
                                              Spring Security (filter chain)
```

Every database query is scoped to the authenticated user's store. There is no trust of client-side parameters for data access — the backend always enforces store boundaries.

---

## The security package map

The security logic lives in a dedicated package: `com.salias.storems.security`. Below is a map of every file in that package and its role in the defense architecture.

| File | Description |
|------|-------------|
| `SecurityConfig.java` | Main security configuration. Defines form login, BCrypt encoding, URL-based role rules, and success handlers. Lines 36-57 configure the filter chain; lines 70-72 define the password encoder. |
| `Roles.java` | Defines role constants used throughout the application. Lines 12-16 declare `ROLE_EMPLOYEE` and `ROLE_STORE_MANAGER`. |
| `StoremsPrincipal.java` | The `UserDetails` implementation that represents the logged-in user. Lines 20-27 hold employee ID, store ID, store name, and authorities. Line 63-66 provides `isStoreManager()` for quick role checks. |
| `StoremsUserDetailsService.java` | Custom `UserDetailsService` that loads from the `Employee` table. Lines 44-74 authenticate users, derive roles from the `store_manager` table, and attach the scoped store to the principal. |
| `CurrentUser.java` | Utility class that resolves the current principal from the security context. Lines 18-24 provide `require()`, lines 26-32 provide `storeId()` and `employeeId()`, and lines 38-42 enforce store boundaries via `assertOwnStore()`. |
| `AccessDeniedForStoreException.java` | Custom exception thrown when a user attempts to access data from another store. Lines 8-10 carry the requested store ID in the message. |

These six files work together as a layered defense:

- **Authentication layer** — `StoremsUserDetailsService` validates credentials; `StoremsPrincipal` holds identity.
- **Authorization layer** — `SecurityConfig` maps URLs to roles; `Roles` defines the role vocabulary.
- **Data scoping layer** — `CurrentUser` enforces store boundaries on every request; `AccessDeniedForStoreException` signals violations.

> **Learning note:** When we say "store scoping," we mean that every query includes `WHERE store_id = :currentStoreId`. The ID comes from the principal, never from the URL. This prevents a manager at Store A from viewing Store B's inventory by manipulating the URL.

---

## A threat model in plain words

StoreMS defends against realistic attacks. This section names each threat, describes it in plain terms, and maps it to the chapter where its defense is explained.

### Credential theft and replay

An attacker obtains a user's password and attempts to reuse it. This includes phishing, keylogging, or sniffing credentials on the network.

**StoreMS defense:** Passwords are never stored in plain text. The application uses BCrypt hashing with per-password salt (`SecurityConfig.java:71`). Each login generates a fresh session with a secure cookie. No credentials travel over the wire in recoverable form.

*Defense covered in: Chapter 2 — Authentication and Session Management*

### Unauthorized access (role escalation)

A user attempts to reach a page they should not see — for example, an employee tries to access the manager dashboard by typing `/dashboard` in the URL bar.

**StoreMS defense:** `SecurityConfig.java:40-44` defines URL-based access rules. Manager paths require `ROLE_STORE_MANAGER`; till paths require `ROLE_EMPLOYEE`. The filter chain rejects unauthorized requests before they reach any controller.

*Defense covered in: Chapter 3 — Authorization and Role-Based Access Control*

### Cross-store data access (IDOR)

A manager at Store A changes the `storeId` parameter in a URL to view or modify Store B's inventory, sales, or employee records.

**StoreMS defense:** URL path parameters are never trusted for data access. `CurrentUser.java:38-42` defines `assertOwnStore()`, which throws `AccessDeniedForStoreException` if the requested store does not match the authenticated user's store. Every controller that accepts a store ID from the URL must call this method first.

*Defense covered in: Chapter 4 — Data Scoping and IDOR Prevention*

### Cross-Site Request Forgery (CSRF)

An attacker tricks a logged-in user into submitting a form to the application without their knowledge — for example, changing the employee's own password via a hidden form on an attacker's site.

**StoreMS defense:** CSRF protection stays enabled. Thymeleaf automatically injects a CSRF token into every form (`_csrf` hidden field). The Spring Security filter chain validates the token on every POST, PUT, DELETE request.

*Defense covered in: Chapter 5 — CSRF Protection*

### Information disclosure (error messages)

An application reveals too much in error messages — stack traces, database table names, internal class paths — which helps attackers map the system.

**StoreMS defense:** The custom `UsernameNotFoundException` message is deliberately vague: "Bad credentials" (`StoremsUserDetailsService.java:48`). Error pages (`error/message.html`) display friendly messages without technical detail. Production configuration suppresses stack traces.

*Defense covered in: Chapter 6 — Secure Error Handling and Information Disclosure*

### Secrets leakage (hardcoded credentials)

Developers leave database passwords, API keys, or admin credentials in source code. An attacker who obtains the source can access the entire system.

**StoreMS defense:** Database credentials are read from environment variables (`application.properties:5-7`). There is no hardcoded username or password in any source file. The `spring.datasource.password` property pulls `${STOREMS_DB_PASSWORD}`, which must be set in the deployment environment.

*Defense covered in: Chapter 7 — Configuration Security and Secrets Management*

### Default or weak credentials

An application ships with a default admin account (e.g., `admin`/`admin`) that attackers can look up in documentation or common credential lists.

**StoreMS defense:** There are no hardcoded logins. Every user is an `Employee` record in the database, and every password is a BCrypt hash. There is no default account — the system is unusable until an administrator creates the first employee via a database seed script or administrative tool.

*Defense covered in: Chapter 2 — Authentication and Session Management*

> **Why it matters:** A single weak link — one hardcoded password, one unchecked URL parameter — can compromise the entire system. This threat model shows that security must be layered. Each defense addresses a specific attack vector, and they work together.

---

## Two roles, one employee

StoreMS uses a two-authority role model that reflects real-world retail operations. Understanding this model is essential for every security decision in the application.

### The two roles

The system defines two security roles in `Roles.java:12-16`:

```java
public static final String STORE_MANAGER = "STORE_MANAGER";
public static final String EMPLOYEE = "EMPLOYEE";

public static final String ROLE_STORE_MANAGER = "ROLE_" + STORE_MANAGER;
public static final String ROLE_EMPLOYEE = "ROLE_" + EMPLOYEE;
```

Every authenticated user has at least `ROLE_EMPLOYEE`. This role grants access to the point-of-sale terminal and the employee's own sales history.

Managers hold *both* roles. The word "manager" in retail typically means "someone who can also run the register." A store manager needs to open the store, count the drawer, process a return, and step in on a busy day. The additive model — manager as employee-plus — matches this reality.

### How roles are assigned

`StoremsUserDetailsService.java:53-55` demonstrates the role assignment logic:

```java
List<GrantedAuthority> authorities = new ArrayList<>();
authorities.add(new SimpleGrantedAuthority(Roles.ROLE_EMPLOYEE));
managed.ifPresent(m -> authorities.add(new SimpleGrantedAuthority(Roles.ROLE_STORE_MANAGER)));
```

Every user receives `ROLE_EMPLOYEE` unconditionally. If the employee's ID appears in the `store_manager` table, they also receive `ROLE_STORE_MANAGER`. Roles are derived from data in the schema, not from a hardcoded configuration file.

### URL path rules

`SecurityConfig.java:25-33` maps these roles to URL patterns:

```java
private static final String[] MANAGER_PATHS = {
        "/dashboard/**", "/inventory/**", "/products/**",
        "/employees/**", "/sales/**", "/reports/**"
};

private static final String[] TILL_PATHS = {
        "/pos/**", "/my-sales/**"
};
```

The rule on line 42 requires `ROLE_STORE_MANAGER` for manager paths. The rule on line 43 requires `ROLE_EMPLOYEE` for till paths. Because managers hold both roles, they can access both areas. Employees (holding only `ROLE_EMPLOYEE`) are rejected from manager paths with a 403 Forbidden response.

This is not an either/or model — it is additive. A manager can sell at the till and manage the inventory from the same session.

> **Learning note:** When designing role systems, ask: "Does this role represent a separate job function, or an extension of a base role?" StoreMS chose additive because a manager's job includes an employee's tasks. If your system models mutually exclusive roles (e.g., admin vs. user with no overlap), you would use separate role constants and conditional assignment instead.

> **Why it matters:** The additive model simplifies authorization rules and matches business reality. However, it also means that any user with `ROLE_EMPLOYEE` can access sensitive sales data. The store-scoping layer (Chapter 4) ensures that even an employee cannot view another store's sales — but within their own store, the employee sees everything. This is a deliberate architectural choice.

---

> **Chapter summary:** This chapter established the foundation for the entire playbook. You learned that StoreMS is a Spring Boot 4.0.1 application using form login, BCrypt hashing, and role-based URL authorization. The security package contains six files that work in layers: authentication via `StoremsUserDetailsService`, identity via `StoremsPrincipal`, authorization via `SecurityConfig`, and data scoping via `CurrentUser`. The threat model identifies seven attack vectors that subsequent chapters will counter, from credential theft to IDOR to information disclosure. Finally, you understand the two-role additive model — every user is an employee, and managers are employees with extra authority — which drives every authorization decision in the application.
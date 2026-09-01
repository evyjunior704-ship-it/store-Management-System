# Part 5 - Store Isolation (IDOR Defense)

Multi-tenant applications face a unique authorization challenge: ensuring that one tenant cannot access another tenant's data. In StoreMS, each store operates as an isolated unit. A manager or employee at Store A must never see, modify, or delete data belonging to Store B. This chapter examines the authorization architecture that enforces these boundaries, focusing on the prevention of Insecure Direct Object Reference (IDOR) attacks.

## What IDOR Is

> **Learning note:** IDOR (Insecure Direct Object Reference) occurs when an application uses an identifier from the client (usually in the URL path or form data) to access a resource without verifying that the caller is authorized for that specific resource. OWASP categorizes this under "Broken Object Level Authorization" (BOLA), representing one of the most common and impactful access control vulnerabilities.

Consider a multi-tenant store management system where each store has its own employees. If the URL pattern `/employees/42` directly uses the employee ID without verifying store membership, an attacker can simply increment the ID to access employees at other stores. The vulnerability unfolds as follows:

1. Alice manages Store 1 (storeId=1) and logs into the application
2. Alice views her own employee profile at `/employees/101` - this page works
3. Alice wonders what happens if she changes the URL to `/employees/201` (guessing an ID from Store 2)
4. Without proper isolation, the server returns employee details for Store 2's employee
5. Alice can now enumerate all employees at competing stores, harvest contact information, or manipulate their records

In systems without store isolation, an IDOR attack requires only URL manipulation. The attacker does not need special privileges, authentication bypass, or SQL injection - they simply request a different object's ID, and the server complies.

## The Store Isolation Pattern

StoreMS enforces a project-wide rule that eliminates this vulnerability class: the store identifier **always** comes from the authenticated session, never from the URL or request parameters. Every service layer method receives the caller's storeId as an explicit parameter, derived from `SecurityContextHolder` via `CurrentUser.storeId()`. Path parameters are allowed for object identifiers (employeeId, contactId, productId) but these are always validated against the caller's store context before any operation proceeds.

This pattern shifts the trust boundary from the client-supplied URL to the server-established session. Even if an attacker manipulates a URL to reference another store's data, the server ignores that manipulation and uses the storeId from the authenticated principal instead. The attacker's session belongs to Store 1, so all operations - regardless of what URL parameter they provide - execute within Store 1's context.

> **Why it matters:** In multi-tenant applications, store isolation is not optional security - it is a fundamental data separation requirement. Without it, one tenant's vulnerability becomes every tenant's breach. The pattern described here eliminates entire classes of horizontal privilege escalation attacks.

## The CurrentUser Guard

The `CurrentUser` class serves as the single source of truth for authorization context. It resolves the authenticated principal from Spring Security's `SecurityContextHolder` and provides static convenience methods for accessing store and employee identifiers.

```java
package com.salias.storems.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the logged-in principal.
 *
 * <p>Every screen scopes its queries to {@code getStoreId()}, so a manager or
 * employee can only ever see their own store. Path parameters are never trusted
 * for scoping.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static StoremsPrincipal require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof StoremsPrincipal principal)) {
            throw new IllegalStateException("No authenticated StoreMS user in the security context");
        }
        return principal;
    }

    public static Long storeId() {
        return require().getStoreId();
    }

    public static Long employeeId() {
        return require().getEmployeeId();
    }

    /**
     * Fails when the requested store is not the caller's own store. Used on every
     * handler that accepts a store or employee id from the URL.
     */
    public static void assertOwnStore(Long storeId) {
        if (storeId != null && !storeId.equals(storeId())) {
            throw new AccessDeniedForStoreException(storeId);
        }
    }
}
```

The class design reflects several intentional security decisions. The `require()` method (lines 18-24) enforces that a valid authenticated principal exists before any authorization check proceeds. If no authentication exists or the principal is not a `StoremsPrincipal`, an `IllegalStateException` aborts the request immediately. This ensures every protected operation has a verified identity.

The `storeId()` method (lines 26-28) extracts the store identifier from the authenticated principal. This value originates from the login process and is tied to the user's session - it cannot be spoofed through request parameters. Similarly, `employeeId()` (lines 30-32) retrieves the caller's employee identifier for operations that need both store and employee context.

The `assertOwnStore()` method (lines 38-42) provides an explicit authorization guard for handlers that accept store or employee identifiers in the URL. The javadoc at lines 34-37 documents its purpose: this guard is applied "on every handler that accepts a store or employee id from the URL." When the provided storeId does not match the caller's storeId, the method throws `AccessDeniedForStoreException`, which triggers a 403 response.

## Service Layer Authorization

Authorization checks are not only applied at the controller layer but are also enforced within the service layer. This defense-in-depth approach ensures that even if a controller mistakenly omits a check, the service layer still validates the store context before returning or modifying data.

The `EmployeeService.findAtStore()` method exemplifies this pattern at lines 47-55 of `EmployeeService.java`:

```java
/** Loads an employee, refusing if they work at a different store. */
public Employee findAtStore(Long storeId, Long employeeId) {
    Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("No employee with id " + employeeId));
    if (!employee.getStoreAssociatedTo().getStoreId().equals(storeId)) {
        throw new ResourceNotFoundException(
                "Employee %d does not work at store %d".formatted(employeeId, storeId));
    }
    return employee;
}
```

This method performs two critical checks. First, it retrieves the employee by ID, throwing `ResourceNotFoundException` if no such employee exists. Second, it compares the employee's store against the provided storeId and throws another `ResourceNotFoundException` if they do not match. The exception message "Employee X does not work at store Y" deliberately obscures whether the employee exists at all - an attacker attempting to access an employee from another store receives the same 404 response as a request for a non-existent employee. This prevents information disclosure about valid identifiers across stores.

Every method that modifies or accesses employee-related data calls `findAtStore()` to ensure the operation is scoped correctly. The `update()` method (lines 75-89), `setCredentials()` method (lines 93-97), and `revokeAccess()` method (lines 100-104) all invoke `findAtStore()` as their first operation, ensuring unauthorized access fails before any modification occurs.

### Emergency Contact Scope Validation

Emergency contacts present a two-level relationship: the contact belongs to an employee, and the employee belongs to a store. The scope check must traverse this relationship to verify the contact's employee works at the caller's store. The `updateEmergencyContact()` method at lines 125-138 demonstrates this pattern:

```java
@Transactional
public EmergencyInfo updateEmergencyContact(Long storeId,
                                            Long contactId,
                                            String contactName,
                                            String phoneNumber,
                                            String relationship) {
    EmergencyInfo contact = emergencyInfoRepository.findById(contactId)
            .orElseThrow(() -> new ResourceNotFoundException("No emergency contact " + contactId));
    // Scope check: the contact's employee must work at the caller's store.
    findAtStore(storeId, contact.getEmployee().getId());

    contact.setContactName(contactName);
    contact.setPhoneNumber(phoneNumber);
    contact.setRelationship(relationship);
    return contact;
}
```

The comment at line 132 explicitly documents the scope check. The code first loads the contact by its own ID, then validates that the contact's employee belongs to the caller's store by calling `findAtStore()` with `contact.getEmployee().getId()`. The `deleteEmergencyContact()` method at lines 142-147 applies the identical pattern, ensuring no contact can be accessed or removed from a store other than the caller's own store.

## EmployeeController Request Handling

The `EmployeeController` demonstrates how controllers translate HTTP requests into service calls that include the store context from the session. The `detail()` method at lines 35-41 shows the pattern:

```java
@GetMapping("/employees/{id}")
String detail(@PathVariable Long id, Model model) {
    Long storeId = CurrentUser.storeId();
    model.addAttribute("employee", employeeService.findAtStore(storeId, id));
    model.addAttribute("contacts", employeeService.findEmergencyContacts(storeId, id));
    return "manager/employee-detail";
}
```

The handler receives the employee ID from the URL path (`@PathVariable Long id`), but it **never** uses this ID for authorization directly. Instead, it retrieves the storeId from `CurrentUser.storeId()` (line 37) and passes both values to the service layer. The service layer then validates that the employee belongs to the caller's store.

The `update()` handler at lines 63-74 follows the same pattern:

```java
@PostMapping("/employees/{id}/update")
String update(@PathVariable Long id,
              @RequestParam String firstName,
              @RequestParam String lastName,
              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
              @RequestParam String address,
              @RequestParam String role,
              RedirectAttributes flash) {
    employeeService.update(CurrentUser.storeId(), id, firstName, lastName, dob, address, role);
    flash.addFlashAttribute("success", "Employee details updated.");
    return "redirect:/employees/" + id;
}
```

The `@PathVariable Long id` is accepted from the URL, but the service call includes `CurrentUser.storeId()` as the first argument. Even if an attacker manipulates the URL to reference another store's employee, the update operation will execute within the attacker's store context and fail the `findAtStore()` check.

The emergency contact handlers at lines 111-131 apply the identical pattern. Each handler receives contactId from the URL but passes `CurrentUser.storeId()` to the service method, which validates the contact's employee's store affiliation.

## MySalesController: Employee-Specific Data

Some operations require scoping by both store and employee. An employee should only see their own sales records, not sales by colleagues at the same store. The `MySalesController.mySales()` method at lines 48-60 demonstrates dual-scope filtering:

```java
@GetMapping("/my-sales")
String mySales(@RequestParam(required = false)
               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
               Model model) {
    LocalDate day = (date == null) ? LocalDate.now() : date;
    Long storeId = CurrentUser.storeId();
    Long employeeId = CurrentUser.employeeId();

    model.addAttribute("date", day);
    model.addAttribute("sales", salesService.findEmployeeSalesOn(storeId, employeeId, day));
    model.addAttribute("totals", salesService.totalsForEmployeeOn(storeId, employeeId, day));
    return "sales/my-sales";
}
```

Both `storeId` and `employeeId` are retrieved from the session (lines 53-54), never from URL parameters. These values are passed to `findEmployeeSalesOn()` and `totalsForEmployeeOn()`, both of which accept both identifiers as parameters. The underlying repository query filters by both dimensions, ensuring an employee sees only their own sales within their own store.

The report generation endpoints at lines 63-93 follow the same pattern. The `shiftReport()` method at lines 63-75:

```java
@GetMapping("/my-sales/report")
String shiftReport(@RequestParam(required = false)
                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                   @RequestParam(name = "ending", defaultValue = "false") boolean ending,
                   Model model) {
    LocalDate day = (date == null) ? LocalDate.now() : date;
    DailySalesReport report =
            reportService.forEmployee(CurrentUser.storeId(), CurrentUser.employeeId(), day);

    model.addAttribute("report", report);
    model.addAttribute("ending", ending);
    return "sales/shift-report";
}
```

Both CSV and PDF download endpoints (lines 77-93) apply identical scoping, retrieving storeId and employeeId from `CurrentUser` rather than accepting them as request parameters.

## The 403 Response Handler

When an attacker attempts cross-store access despite these protections, the `WebExceptionHandler.wrongStore()` method catches the `AccessDeniedForStoreException` and renders a generic error page. The handler at lines 30-37 of `WebExceptionHandler.java`:

```java
@ExceptionHandler(AccessDeniedForStoreException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
String wrongStore(AccessDeniedForStoreException ex, Model model) {
    log.warn("Cross-store access attempt: {}", ex.getMessage());
    model.addAttribute("title", "Not your store");
    model.addAttribute("message", ex.getMessage());
    return "error/message";
}
```

The handler performs two important actions. First, it logs a warning message that includes the attempted store identifier (`log.warn("Cross-store access attempt: {}", ex.getMessage())`). This provides an audit trail for security monitoring, allowing administrators to detect and investigate unauthorized access patterns. Second, it renders a generic error page with title "Not your store" and the exception message, avoiding the disclosure of whether the target store or resource actually exists.

The `AccessDeniedForStoreException` class (lines 1-11 of `AccessDeniedForStoreException.java`) carries the attempted store identifier:

```java
package com.salias.storems.security;

/**
 * Thrown when a user touches data belonging to a store other than their own.
 */
public class AccessDeniedForStoreException extends RuntimeException {

    public AccessDeniedForStoreException(Long requestedStoreId) {
        super("You do not have access to store " + requestedStoreId);
    }
}
```

The exception message includes the requested storeId for audit purposes but delivers a 403 Forbidden response to the client. This differs from the 404 response for missing resources, allowing the application to distinguish between invalid requests and unauthorized requests while still avoiding the disclosure of whether the target store exists.

## Learning Notes

> **Learning note:** Authorization must be applied at every layer, not just at the URL routing level. Spring Security's URL patterns determine which URLs a user can access (function-level authorization), but they cannot verify whether a user can access a specific employee's record within that URL (object-level authorization). Object-level checks belong in the service layer, where the business domain model can validate relationships.

> **Learning note:** Never trust client-supplied identifiers for authorization decisions. The client controls only the browser, the URL, and the form data. Any of these can be manipulated. Authorization decisions must rely on server-controlled data - in this case, the authenticated session established during login.

> **Learning note:** Defense in depth means applying checks at multiple levels. StoreMS checks store membership at both the controller level (via `CurrentUser.assertOwnStore()`) and the service level (via `findAtStore()`). If one layer fails due to developer error, the other layer still blocks the attack.

> **Learning note:** The choice between 403 and 404 for cross-store access involves a trade-off between usability and security. A 403 reveals that the resource exists but the user lacks access, which could help attackers enumerate valid identifiers. A 404 hides whether the resource exists, making enumeration attacks more difficult. StoreMS uses 404-like error messages ("Employee X does not work at store Y") to avoid revealing cross-store data existence.

> **Learning note:** OWASP ranks Broken Access Control (including BOLA/IDOR) as the top web application security risk. The 2021 OWASP Top 10 lists "A01:2021 - Broken Access Control" as the most critical category, emphasizing that access control vulnerabilities remain the primary attack vector in real-world applications.

---

> **Chapter summary:** StoreMS prevents cross-store data access through a multi-layered authorization architecture. The `CurrentUser` class extracts store and employee identifiers from the authenticated session, never trusting client-supplied values. Every service method receives the caller's storeId as an explicit parameter and validates object ownership before returning data. Emergency contact operations traverse the employee relationship to verify store membership. Cross-store access attempts result in 403 responses with server-side logging for audit purposes. This pattern eliminates the entire class of IDOR attacks by shifting trust from client-controlled identifiers to server-controlled session state.
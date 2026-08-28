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

package com.salias.storems.security;

/**
 * Thrown when a user touches data belonging to a store other than their own.
 */
public class AccessDeniedForStoreException extends RuntimeException {

    public AccessDeniedForStoreException(Long requestedStoreId) {
        super("You do not have access to store " + requestedStoreId);
    }
}

package com.salias.storems.service;

/**
 * Thrown when a requested record does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

package com.salias.storems.service;

/**
 * Thrown when a sale would take a store's stock of a product below zero.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}

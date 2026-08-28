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

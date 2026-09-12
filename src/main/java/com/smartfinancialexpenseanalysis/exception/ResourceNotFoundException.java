package com.smartfinancialexpenseanalysis.exception;

/**
 * Thrown when a requested resource (e.g. expense, category) is not found
 * or cannot be accessed by the requesting user.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

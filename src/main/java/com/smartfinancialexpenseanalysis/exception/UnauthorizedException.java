package com.smartfinancialexpenseanalysis.exception;

/**
 * Thrown when an unauthenticated or invalid user attempts to access a protected resource.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

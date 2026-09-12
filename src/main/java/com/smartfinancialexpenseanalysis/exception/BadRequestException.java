package com.smartfinancialexpenseanalysis.exception;

/**
 * Thrown when a request contains invalid logical arguments, such as
 * startDate occurring after endDate in filter queries.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

package com.smartfinancialexpenseanalysis.exception;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler providing uniform JSON error responses across the REST API.
 * Ensures stack traces and sensitive internal information are never exposed to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotated request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String firstErrorMessage = ex.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Validation failed: Please check your input");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, firstErrorMessage));
    }

    /**
     * Handles invalid credentials during login attempts.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles unauthorized access attempts when a user is not authenticated.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles conflict exceptions such as attempting to register with an existing email.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles role authorization failures (e.g. USER accessing ADMIN routes).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse(false, "Access denied: You do not have permission to perform this action"));
    }

    /**
     * Handles resource not found exceptions (e.g. expense not found or not belonging to current user).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles bad request logical errors such as invalid date ranges.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, ex.getMessage()));
    }

    /**
     * Handles type mismatch errors on request parameters (e.g. invalid paymentMethod enum or bad date format).
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String message;
        if ("paymentMethod".equalsIgnoreCase(paramName)) {
            message = "Invalid payment method. Allowed values are: CASH, UPI, CARD, BANK_TRANSFER, OTHER";
        } else if ("startDate".equalsIgnoreCase(paramName) || "endDate".equalsIgnoreCase(paramName) || "date".equalsIgnoreCase(paramName)) {
            message = "Invalid date format for " + paramName + ". Please use ISO format (YYYY-MM-DD)";
        } else {
            message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), paramName);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, message));
    }

    /**
     * Handles unreadable or malformed JSON request bodies, including invalid enum constants.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        String msg = "Malformed request body or invalid field values";
        if (ex.getMessage() != null && ex.getMessage().contains("PaymentMethod")) {
            msg = "Invalid payment method. Allowed values are: CASH, UPI, CARD, BANK_TRANSFER, OTHER";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, msg));
    }

    /**
     * Catches any unhandled generic exceptions to avoid exposing internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "An unexpected error occurred. Please try again later."));
    }
}

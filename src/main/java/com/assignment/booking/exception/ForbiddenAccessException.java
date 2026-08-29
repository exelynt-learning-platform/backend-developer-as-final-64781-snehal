package com.assignment.booking.exception;

/**
 * Raised when an authenticated user is not allowed to access a resource.
 * This maps to HTTP 403 Forbidden, not 401 Unauthorized.
 */
public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException(String message) {
        super(message);
    }
}

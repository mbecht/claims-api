package com.mbecht.claims_api.exception;

/**
 * Thrown when a request conflicts with the current state of a resource,
 * e.g. an illegal claim status transition. Maps to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    private final ErrorCode errorCode;

    public ConflictException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

package com.mbecht.claims_api.exception;

/**
 * Thrown when a requested resource (e.g. a claim or policy ID) does not exist.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

package com.mbecht.claims_api.exception;

/**
 * Thrown when a request is well-formed but breaks a business rule,
 * e.g. a claim against an expired policy. Maps to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

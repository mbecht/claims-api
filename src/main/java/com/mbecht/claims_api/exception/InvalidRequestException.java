package com.mbecht.claims_api.exception;

/**
 * A request value has the right type but is not allowed, such as a sort field outside
 * the permitted list. Maps to HTTP 400 with {@link ErrorCode#VALIDATION_FAILED}.
 */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

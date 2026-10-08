package com.mbecht.claims_api.exception;

/**
 * Thrown for any failed login attempt, whether the username doesn't exist or the password is
 * wrong. Deliberately carries no message of its own — GlobalExceptionHandler uses one fixed
 * message for every case, so a caller can't use the response to tell which usernames exist.
 * Maps to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {
}

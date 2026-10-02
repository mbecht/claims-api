package com.mbecht.claims_api.exception;

/**
 * Machine-readable error identifiers included in every error response.
 * New values are added as new business rules and lookups are implemented.
 */
public enum ErrorCode {
    CLAIM_NOT_FOUND,
    POLICY_NOT_FOUND,
    POLICY_NOT_YET_ACTIVE,
    POLICY_EXPIRED,
    CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT,
    ILLEGAL_STATUS_TRANSITION,
    BUSINESS_RULE_VIOLATION,
    VALIDATION_FAILED,
    MALFORMED_REQUEST,
    NO_SUCH_ENDPOINT,
    INTERNAL_ERROR
}

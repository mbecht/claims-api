package com.mbecht.claims_api.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints that each throw one of GlobalExceptionHandler's
 * handled exception types, since no real controller triggers them yet.
 */
@RestController
class ExceptionTestController {

    @GetMapping("/test/resource-not-found")
    void triggerResourceNotFound() {
        throw new ResourceNotFoundException(ErrorCode.CLAIM_NOT_FOUND, "Claim 999 not found.");
    }

    @GetMapping("/test/conflict")
    void triggerConflict() {
        throw new ConflictException(
                ErrorCode.ILLEGAL_STATUS_TRANSITION, "Cannot transition claim from SUBMITTED to PAID.");
    }

    @GetMapping("/test/business-rule")
    void triggerBusinessRule() {
        throw new BusinessRuleException(ErrorCode.BUSINESS_RULE_VIOLATION, "Policy POL-123 is expired.");
    }

    @GetMapping("/test/unexpected")
    void triggerUnexpected() {
        throw new IllegalStateException("db password is hunter2, do not leak this to the client");
    }
}

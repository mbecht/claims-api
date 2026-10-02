package com.mbecht.claims_api;

import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.exception.BusinessRuleException;
import com.mbecht.claims_api.exception.ErrorCode;
import com.mbecht.claims_api.service.CoverageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CoverageValidatorTest {

    private CoverageValidator coverageValidator;

    @BeforeEach
    public void setUp() {
        coverageValidator = new CoverageValidator();
    }

    private Policy policyWith(int policyNumber, LocalDate coverageStart, LocalDate coverageEnd, BigDecimal coverageLimit) {
        User user = new User(null, null, null);
        return new Policy(policyNumber, user, coverageStart, coverageEnd, coverageLimit);
    }

    @Test
    public void incidentDateAfterCoverageEndIsRejected() {
        // An incident reported after the policy's coverage end date is a POLICY_EXPIRED business rule violation.
        Policy policy = policyWith(1002, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), new BigDecimal("1000.00"));
        LocalDateTime incidentDate = LocalDateTime.of(2026, 8, 14, 9, 30);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> coverageValidator.validate(policy, incidentDate, new BigDecimal("500.00")));

        assertEquals(ErrorCode.POLICY_EXPIRED, ex.getErrorCode());
        assertEquals(
                "Incident date 2026-08-14 is after policy 1002 coverage ended on 2026-06-30.",
                ex.getMessage());
    }

    @Test
    public void incidentDateBeforeCoverageStartIsRejected() {
        // An incident reported before the policy's coverage start date is a POLICY_NOT_YET_ACTIVE business rule violation.
        Policy policy = policyWith(1003, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), new BigDecimal("1000.00"));
        LocalDateTime incidentDate = LocalDateTime.of(2024, 12, 31, 9, 30);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> coverageValidator.validate(policy, incidentDate, new BigDecimal("500.00")));

        assertEquals(ErrorCode.POLICY_NOT_YET_ACTIVE, ex.getErrorCode());
        assertEquals(
                "Incident date 2024-12-31 is before policy 1003 coverage starts on 2025-01-01.",
                ex.getMessage());
    }

    @Test
    public void claimAmountExceedingCoverageLimitIsRejected() {
        // A claim amount that exceeds the policy's coverage limit is a CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT business rule violation.
        Policy policy = policyWith(1004, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), new BigDecimal("1000.00"));
        LocalDateTime incidentDate = LocalDateTime.of(2025, 3, 15, 10, 0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> coverageValidator.validate(policy, incidentDate, new BigDecimal("1500.00")));

        assertEquals(ErrorCode.CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT, ex.getErrorCode());
        assertEquals(
                "Claim amount 1500.00 exceeds policy 1004 coverage limit of 1000.00.",
                ex.getMessage());
    }

    @Test
    public void claimAmountEqualToCoverageLimitIsAccepted() {
        Policy policy = policyWith(1005, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), new BigDecimal("1000.00"));
        LocalDateTime incidentDate = LocalDateTime.of(2025, 3, 15, 10, 0);

        assertDoesNotThrow(() -> coverageValidator.validate(policy, incidentDate, new BigDecimal("1000.00")));
    }

    @Test
    public void incidentDateEqualToCoverageStartIsAccepted() {
        Policy policy = policyWith(1006, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), new BigDecimal("1000.00"));
        LocalDateTime incidentDate = LocalDateTime.of(2025, 1, 1, 10, 0);

        assertDoesNotThrow(() -> coverageValidator.validate(policy, incidentDate, new BigDecimal("500.00")));
    }

    @Test
    public void incidentDateEqualToCoverageEndIsAccepted() {
        Policy policy = policyWith(1007, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 6, 30), new BigDecimal("1000.00"));
        LocalDateTime incidentDate = LocalDateTime.of(2026, 6, 30, 10, 0);

        assertDoesNotThrow(() -> coverageValidator.validate(policy, incidentDate, new BigDecimal("500.00")));
    }
}

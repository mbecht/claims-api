package com.mbecht.claims_api.service;

import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.exception.BusinessRuleException;
import com.mbecht.claims_api.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Checks a claim against its policy's coverage rules: the incident date must fall
 * within the coverage period and the claim amount must not exceed the coverage limit.
 */
@Component
public class CoverageValidator {

    public void validate(Policy policy, LocalDateTime incidentDateTime, BigDecimal claimAmount) {
        LocalDate incidentDate = incidentDateTime.toLocalDate();

        if (incidentDate.isAfter(policy.getCoverageEnd())) {
            throw new BusinessRuleException(
                    ErrorCode.POLICY_EXPIRED,
                    "Incident date %s is after policy %d coverage ended on %s.".formatted(
                            incidentDate, policy.getPolicyNumber(), policy.getCoverageEnd()));
        }
        if (incidentDate.isBefore(policy.getCoverageStart())) {
            throw new BusinessRuleException(
                    ErrorCode.POLICY_NOT_YET_ACTIVE,
                    "Incident date %s is before policy %d coverage starts on %s.".formatted(
                            incidentDate, policy.getPolicyNumber(), policy.getCoverageStart()));
        }
        if (claimAmount.compareTo(policy.getCoverageLimit()) > 0) {
            throw new BusinessRuleException(
                    ErrorCode.CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT,
                    "Claim amount %s exceeds policy %d coverage limit of %s.".formatted(
                            claimAmount, policy.getPolicyNumber(), policy.getCoverageLimit()));
        }
    }
}

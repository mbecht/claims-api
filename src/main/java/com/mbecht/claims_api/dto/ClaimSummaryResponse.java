package com.mbecht.claims_api.dto;

import com.mbecht.claims_api.entity.ClaimStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClaimSummaryResponse(
        Long id,
        String claimNumber,
        Integer policyNumber,
        LocalDateTime incidentDate,
        BigDecimal amount,
        ClaimStatus status,
        LocalDateTime submittedAt
) {
}

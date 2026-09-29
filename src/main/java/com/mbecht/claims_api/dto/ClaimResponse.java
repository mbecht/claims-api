package com.mbecht.claims_api.dto;

import com.mbecht.claims_api.entity.ClaimStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClaimResponse(
        Long id,
        String claimNumber,
        Integer policyNumber,
        LocalDateTime incidentDate,
        BigDecimal amount,
        String description,
        ClaimStatus status,
        LocalDateTime submittedAt
) {
}

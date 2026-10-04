package com.mbecht.claims_api.dto;

import com.mbecht.claims_api.entity.ClaimStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClaimDetailResponse(
        Long id,
        String claimNumber,
        LocalDateTime incidentDate,
        BigDecimal amount,
        String description,
        ClaimStatus status,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        PolicyInfo policy
) {

    public record PolicyInfo(
            Integer policyNumber,
            LocalDate coverageStart,
            LocalDate coverageEnd,
            BigDecimal coverageLimit
    ) {
    }
}

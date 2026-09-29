package com.mbecht.claims_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmitClaimRequest(

        @NotNull
        @Positive
        Integer policyNumber,

        @NotNull
        @PastOrPresent
        LocalDateTime incidentDate,

        @NotNull
        @Positive
        BigDecimal amount,

        @NotBlank
        @Size(max = 1000)
        String description
) {
}

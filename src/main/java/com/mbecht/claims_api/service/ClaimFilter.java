package com.mbecht.claims_api.service;

import com.mbecht.claims_api.entity.ClaimStatus;

import java.time.LocalDate;

/**
 * Optional filters for listing claims. A null field means "do not filter on this".
 * Both submittedFrom and submittedTo are inclusive dates; if submittedFrom is after
 * submittedTo, no claim can match and the result is empty.
 */
public record ClaimFilter(
        ClaimStatus status,
        Integer policyNumber,
        LocalDate submittedFrom,
        LocalDate submittedTo
) {
}

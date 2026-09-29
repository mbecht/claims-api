package com.mbecht.claims_api.service;

import com.mbecht.claims_api.repository.ClaimRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
public class ClaimNumberGenerator {

    private static final String PREFIX = "CLM";

    private final ClaimRepository claimRepository;

    public ClaimNumberGenerator(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @Transactional
    public String next() {
        long sequenceValue = claimRepository.nextClaimNumberSequenceValue();
        int year = Year.now().getValue();
        return "%s-%d-%06d".formatted(PREFIX, year, sequenceValue);
    }
}

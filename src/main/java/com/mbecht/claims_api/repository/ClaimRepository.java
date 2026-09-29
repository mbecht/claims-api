package com.mbecht.claims_api.repository;

import com.mbecht.claims_api.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @Query(value = "SELECT nextval('claim_number_seq')", nativeQuery = true)
    Long nextClaimNumberSequenceValue();
}

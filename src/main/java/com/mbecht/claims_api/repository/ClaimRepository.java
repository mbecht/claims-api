package com.mbecht.claims_api.repository;

import com.mbecht.claims_api.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

    @Query(value = "SELECT nextval('claim_number_seq')", nativeQuery = true)
    Long nextClaimNumberSequenceValue();

    /**
     * Overrides the inherited search so each claim's policy is fetched in the same
     * SELECT (one query per page) instead of one extra query per claim.
     */
    @Override
    @EntityGraph(attributePaths = "policy")
    Page<Claim> findAll(Specification<Claim> spec, Pageable pageable);
}

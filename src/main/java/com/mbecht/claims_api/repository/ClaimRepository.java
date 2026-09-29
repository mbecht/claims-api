package com.mbecht.claims_api.repository;

import com.mbecht.claims_api.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
}

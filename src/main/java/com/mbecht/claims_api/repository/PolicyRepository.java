package com.mbecht.claims_api.repository;

import com.mbecht.claims_api.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(Integer policyNumber);
}

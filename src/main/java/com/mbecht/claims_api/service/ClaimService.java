package com.mbecht.claims_api.service;

import com.mbecht.claims_api.dto.ClaimResponse;
import com.mbecht.claims_api.dto.SubmitClaimRequest;
import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.exception.ErrorCode;
import com.mbecht.claims_api.exception.ResourceNotFoundException;
import com.mbecht.claims_api.repository.ClaimRepository;
import com.mbecht.claims_api.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final ClaimNumberGenerator claimNumberGenerator;
    private final CoverageValidator coverageValidator;

    public ClaimService(ClaimRepository claimRepository,
                         PolicyRepository policyRepository,
                         ClaimNumberGenerator claimNumberGenerator,
                         CoverageValidator coverageValidator) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.claimNumberGenerator = claimNumberGenerator;
        this.coverageValidator = coverageValidator;
    }

    @Transactional
    public ClaimResponse submitClaim(SubmitClaimRequest request) {
        Policy policy = policyRepository.findByPolicyNumber(request.policyNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.POLICY_NOT_FOUND,
                        "No policy found with number " + request.policyNumber()));

        coverageValidator.validate(policy, request.incidentDate(), request.amount());

        String claimNumber = claimNumberGenerator.next();

        Claim claim = new Claim(
                claimNumber,
                policy,
                request.incidentDate(),
                request.amount(),
                request.description());

        Claim saved = claimRepository.save(claim);

        return toResponse(saved);
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPolicy().getPolicyNumber(),
                claim.getIncidentDate(),
                claim.getAmount(),
                claim.getDescription(),
                claim.getStatus(),
                claim.getCreatedAt());
    }
}

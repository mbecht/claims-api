package com.mbecht.claims_api.service;

import com.mbecht.claims_api.dto.ClaimDetailResponse;
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
    public ClaimDetailResponse submitClaim(SubmitClaimRequest request) {
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

        return toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClaimDetailResponse getClaim(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CLAIM_NOT_FOUND,
                        "No claim found with id " + id));

        return toDetailResponse(claim);
    }

    private ClaimDetailResponse toDetailResponse(Claim claim) {
        Policy policy = claim.getPolicy();

        return new ClaimDetailResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getIncidentDate(),
                claim.getAmount(),
                claim.getDescription(),
                claim.getStatus(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                new ClaimDetailResponse.PolicyInfo(
                        policy.getPolicyNumber(),
                        policy.getCoverageStart(),
                        policy.getCoverageEnd(),
                        policy.getCoverageLimit()));
    }

}

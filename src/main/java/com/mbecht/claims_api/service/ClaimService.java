package com.mbecht.claims_api.service;

import com.mbecht.claims_api.dto.ClaimResponse;
import com.mbecht.claims_api.dto.SubmitClaimRequest;
import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.exception.BusinessRuleException;
import com.mbecht.claims_api.exception.ErrorCode;
import com.mbecht.claims_api.exception.ResourceNotFoundException;
import com.mbecht.claims_api.repository.ClaimRepository;
import com.mbecht.claims_api.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final ClaimNumberGenerator claimNumberGenerator;

    public ClaimService(ClaimRepository claimRepository,
                         PolicyRepository policyRepository,
                         ClaimNumberGenerator claimNumberGenerator) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.claimNumberGenerator = claimNumberGenerator;
    }

    @Transactional
    public ClaimResponse submitClaim(SubmitClaimRequest request) {
        Policy policy = policyRepository.findByPolicyNumber(request.policyNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.POLICY_NOT_FOUND,
                        "No policy found with number " + request.policyNumber()));

        LocalDate incidentDate = request.incidentDate().toLocalDate();
        BigDecimal claimAmount = request.amount();
        if (incidentDate.isAfter(policy.getCoverageEnd())) {
            throw new BusinessRuleException(
                    ErrorCode.POLICY_EXPIRED,
                    "Incident date %s is after policy %d coverage ended on %s.".formatted(
                            incidentDate, policy.getPolicyNumber(), policy.getCoverageEnd()));
        }
        if (incidentDate.isBefore(policy.getCoverageStart())) {
            throw new BusinessRuleException(
                ErrorCode.POLICY_NOT_YET_ACTIVE,
                "Incident date %s is before policy %d coverage starts on %s.".formatted(
                    incidentDate, policy.getPolicyNumber(), policy.getCoverageStart()));
        }
        if (claimAmount.compareTo(policy.getCoverageLimit()) > 0) {
            throw new BusinessRuleException(
                    ErrorCode.CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT,
                    "Claim amount %s exceeds policy %d coverage limit of %s.".formatted(
                            claimAmount, policy.getPolicyNumber(), policy.getCoverageLimit()));
        }
        
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

package com.mbecht.claims_api.service;

import com.mbecht.claims_api.dto.ClaimDetailResponse;
import com.mbecht.claims_api.dto.ClaimSummaryResponse;
import com.mbecht.claims_api.dto.PageResponse;
import com.mbecht.claims_api.dto.SubmitClaimRequest;
import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.exception.ErrorCode;
import com.mbecht.claims_api.exception.InvalidRequestException;
import com.mbecht.claims_api.exception.ResourceNotFoundException;
import com.mbecht.claims_api.repository.ClaimRepository;
import com.mbecht.claims_api.repository.ClaimSpecifications;
import com.mbecht.claims_api.repository.PolicyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    @Transactional(readOnly = true)
    public PageResponse<ClaimSummaryResponse> listClaims(ClaimFilter filter, Pageable pageable) {
        Page<Claim> page = claimRepository.findAll(ClaimSpecifications.matching(filter), toEntityPageable(pageable));

        return new PageResponse<>(
                page.map(this::toSummaryResponse).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * Translates the client's sort into entity properties, rejecting any field outside
     * {@link ClaimSortField}. An id tiebreaker is appended so claims with equal sort
     * values keep the same order from one page to the next.
     */
    private Pageable toEntityPageable(Pageable requested) {
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : requested.getSort()) {
            ClaimSortField field = ClaimSortField.fromApiName(order.getProperty())
                    .orElseThrow(() -> new InvalidRequestException(
                            "sort",
                            "Invalid sort field '" + order.getProperty() + "'. Allowed sort fields: "
                                    + ClaimSortField.allowedNames() + "."));
            orders.add(new Sort.Order(order.getDirection(), field.entityProperty()));
        }
        orders.add(Sort.Order.asc("id"));

        return PageRequest.of(requested.getPageNumber(), requested.getPageSize(), Sort.by(orders));
    }

    private ClaimSummaryResponse toSummaryResponse(Claim claim) {
        return new ClaimSummaryResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPolicy().getPolicyNumber(),
                claim.getIncidentDate(),
                claim.getAmount(),
                claim.getStatus(),
                claim.getCreatedAt());
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

package com.mbecht.claims_api.controller;

import com.mbecht.claims_api.dto.ClaimDetailResponse;
import com.mbecht.claims_api.dto.ClaimSummaryResponse;
import com.mbecht.claims_api.dto.PageResponse;
import com.mbecht.claims_api.dto.SubmitClaimRequest;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.exception.InvalidRequestException;
import com.mbecht.claims_api.service.ClaimFilter;
import com.mbecht.claims_api.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "submittedAt");

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<ClaimDetailResponse> submitClaim(@Valid @RequestBody SubmitClaimRequest request) {
        ClaimDetailResponse response = claimService.submitClaim(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimDetailResponse> getClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaim(id));
    }

    /**
     * Lists claims. Sizes above {@value #MAX_PAGE_SIZE} are reduced to {@value #MAX_PAGE_SIZE}.
     * {@code params} is read only for {@code sort}, because it can repeat
     * ({@code ?sort=amount,asc&sort=incidentDate,desc}) and Spring would split a single
     * {@code sort=amount,asc} value into two separate entries if it were a list parameter.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ClaimSummaryResponse>> listClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(required = false) Integer policyNumber,
            @RequestParam(required = false) LocalDate submittedFrom,
            @RequestParam(required = false) LocalDate submittedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam MultiValueMap<String, String> params) {

        if (page < 0) {
            throw new InvalidRequestException("page", "Invalid page " + page + ". Page must be 0 or greater.");
        }
        if (size < 1) {
            throw new InvalidRequestException("size", "Invalid size " + size + ". Size must be 1 or greater.");
        }

        ClaimFilter filter = new ClaimFilter(status, policyNumber, submittedFrom, submittedTo);
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), parseSort(params.get("sort")));

        return ResponseEntity.ok(claimService.listClaims(filter, pageable));
    }

    /**
     * Parses each sort value as {@code field} or {@code field,direction}, in the order given.
     * The field name itself is checked against the allowed list by the service.
     */
    private Sort parseSort(List<String> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return DEFAULT_SORT;
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String value : sortValues) {
            String[] parts = value.split(",", -1);
            if (parts.length > 2) {
                throw new InvalidRequestException("sort",
                        "Invalid sort value '" + value + "'. Use field or field,direction.");
            }
            Sort.Direction direction = parts.length == 2 ? parseDirection(parts[1]) : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, parts[0]));
        }
        return Sort.by(orders);
    }

    private Sort.Direction parseDirection(String value) {
        return Sort.Direction.fromOptionalString(value)
                .orElseThrow(() -> new InvalidRequestException("sort",
                        "Invalid sort direction '" + value + "'. Allowed directions: asc, desc."));
    }
}

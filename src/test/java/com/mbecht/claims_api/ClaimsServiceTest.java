package com.mbecht.claims_api;

import com.mbecht.claims_api.dto.ClaimDetailResponse;
import com.mbecht.claims_api.dto.SubmitClaimRequest;
import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.exception.InvalidRequestException;
import com.mbecht.claims_api.exception.ResourceNotFoundException;
import com.mbecht.claims_api.repository.ClaimRepository;
import com.mbecht.claims_api.repository.PolicyRepository;
import com.mbecht.claims_api.service.ClaimFilter;
import com.mbecht.claims_api.service.ClaimNumberGenerator;
import com.mbecht.claims_api.service.ClaimService;
import com.mbecht.claims_api.service.CoverageValidator;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Covers ClaimService's own orchestration (policy lookup, claim number
 * generation, persistence) - the coverage business rules it delegates to
 * CoverageValidator are tested in isolation by CoverageValidatorTest.
 */
public class ClaimsServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimNumberGenerator claimNumberGenerator;

    @Mock
    private PolicyRepository policyRepository;

    private ClaimService claimService;

    @BeforeEach
    public void setUp() {
        // Initialize mocks and any necessary setup
        MockitoAnnotations.openMocks(this);
        this.claimService = new ClaimService(claimRepository, policyRepository, claimNumberGenerator, new CoverageValidator());
    }

    // Private helper method to created a SubmitClaimRequest with default values
    private void createDefaultSubmitClaimRequest(int policyNumber, String claimNumber) {
        User user = new User(null, null, null);
        LocalDate coverageStart = LocalDate.now().minusMonths(6);
        LocalDate coverageEnd = LocalDate.now().plusMonths(6);
        Policy policy = new Policy(policyNumber, user, coverageStart, coverageEnd, new BigDecimal("1000.00"));
        when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(policy));
        when(claimNumberGenerator.next()).thenReturn(claimNumber);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testValidPostRequest() {
        // Implement test logic for a valid request that creates a claim with status "SUBMITTED"
        int policyNumber = 123;
        String claimNumber = "CLAIM-001";

        // Arrange
        createDefaultSubmitClaimRequest(policyNumber, claimNumber);
        SubmitClaimRequest request = new SubmitClaimRequest(policyNumber, LocalDateTime.now(), new BigDecimal("500.00"), "Test claim description");

        // Act
        ClaimDetailResponse response = claimService.submitClaim(request);

        // Assert
        assertNotNull(response.claimNumber());
        assertEquals(ClaimStatus.SUBMITTED, response.status());
        assertEquals(claimNumber, response.claimNumber());
        assertEquals(policyNumber, response.policy().policyNumber());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    public void testUnknownPolicyNumberPost() {
        // Does an unknown policy throw and skip save?
        // Arrange
        SubmitClaimRequest request = new SubmitClaimRequest(999, LocalDateTime.now(), new BigDecimal("500.00"), "Test claim description");
        when(policyRepository.findByPolicyNumber(999)).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(ResourceNotFoundException.class, () -> claimService.submitClaim(request));
        verify(claimRepository, never()).save(any(Claim.class));

    }

    @Test
    public void testValidGetRequest() {
        // Arrange
        Policy policy = new Policy(123, new User(null, null, null), LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6), new BigDecimal("1000.00"));
        Claim claim = new Claim("CLAIM-001", policy, LocalDateTime.now(), new BigDecimal("500.00"), "Test claim description");
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        // Act
        ClaimDetailResponse response = claimService.getClaim(1L);

        // Assert
        assertNotNull(response);
        assertEquals("CLAIM-001", response.claimNumber());
        assertEquals(ClaimStatus.SUBMITTED, response.status());
        assertEquals(123, response.policy().policyNumber());
    }

    @Test
    public void listClaims_translatesPublicSortNameAndAddsIdTiebreaker() {
        when(claimRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        claimService.listClaims(
                new ClaimFilter(null, null, null, null),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "submittedAt")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(claimRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")), captor.getValue().getSort());
    }

    @Test
    public void listClaims_rejectsSortFieldOutsideAllowedList() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                claimService.listClaims(
                        new ClaimFilter(null, null, null, null),
                        PageRequest.of(0, 20, Sort.by("createdAt"))));

        assertEquals("sort", ex.getField());
        assertEquals("Invalid sort field 'createdAt'. Allowed sort fields: submittedAt, amount, incidentDate.",
                ex.getMessage());
        verify(claimRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    public void testUnknownIdGet() {
        // Arrange
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(ResourceNotFoundException.class, () -> claimService.getClaim(999L));
    }
}

package com.mbecht.claims_api;

import com.mbecht.claims_api.dto.ClaimResponse;
import com.mbecht.claims_api.dto.SubmitClaimRequest;
import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.exception.ResourceNotFoundException;
import com.mbecht.claims_api.repository.ClaimRepository;
import com.mbecht.claims_api.repository.PolicyRepository;
import com.mbecht.claims_api.service.ClaimNumberGenerator;
import com.mbecht.claims_api.service.ClaimService;
import com.mbecht.claims_api.service.CoverageValidator;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    public void testValidRequest() {
        // Implement test logic for a valid request that creates a claim with status "SUBMITTED"
        int policyNumber = 123;
        String claimNumber = "CLAIM-001";

        // Arrange
        createDefaultSubmitClaimRequest(policyNumber, claimNumber);
        SubmitClaimRequest request = new SubmitClaimRequest(policyNumber, LocalDateTime.now(), new BigDecimal("500.00"), "Test claim description");

        // Act
        ClaimResponse response = claimService.submitClaim(request);

        // Assert
        assertNotNull(response.claimNumber());
        assertEquals(ClaimStatus.SUBMITTED, response.status());
        assertEquals(claimNumber, response.claimNumber());
        assertEquals(policyNumber, response.policyNumber());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    public void testUnknownPolicyNumber() {
        // Does an unknown policy throw and skip save?
        // Arrange
        SubmitClaimRequest request = new SubmitClaimRequest(999, LocalDateTime.now(), new BigDecimal("500.00"), "Test claim description");
        when(policyRepository.findByPolicyNumber(999)).thenReturn(Optional.empty());

        // Act and Assert
        assertThrows(ResourceNotFoundException.class, () -> claimService.submitClaim(request));
        verify(claimRepository, never()).save(any(Claim.class));

    }
}

package com.mbecht.claims_api;

import com.mbecht.claims_api.dto.ClaimResponse;
import com.mbecht.claims_api.dto.SubmitClaimRequest;
import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.exception.BusinessRuleException;
import com.mbecht.claims_api.exception.ErrorCode;
import com.mbecht.claims_api.exception.ResourceNotFoundException;
import com.mbecht.claims_api.repository.ClaimRepository;
import com.mbecht.claims_api.repository.PolicyRepository;
import com.mbecht.claims_api.service.ClaimNumberGenerator;
import com.mbecht.claims_api.service.ClaimService;
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
        this.claimService = new ClaimService(claimRepository, policyRepository, claimNumberGenerator);
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

    @Test
    public void testIncidentDateAfterPolicyCoverageEndIsRejected() {
        // An incident reported after the policy's coverage end date is a POLICY_EXPIRED business rule violation.
        // Arrange
        int policyNumber = 1002;
        User user = new User(null, null, null);
        LocalDate coverageStart = LocalDate.of(2025, 1, 1);
        LocalDate coverageEnd = LocalDate.of(2026, 6, 30);
        Policy policy = new Policy(policyNumber, user, coverageStart, coverageEnd, new BigDecimal("1000.00"));
        when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(policy));

        LocalDateTime incidentDate = LocalDateTime.of(2026, 8, 14, 9, 30);
        SubmitClaimRequest request = new SubmitClaimRequest(
                policyNumber, incidentDate, new BigDecimal("500.00"), "Test claim description");

        // Act
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class, () -> claimService.submitClaim(request));

        // Assert
        assertEquals(ErrorCode.POLICY_EXPIRED, ex.getErrorCode());
        assertEquals(
                "Incident date 2026-08-14 is after policy 1002 coverage ended on 2026-06-30.",
                ex.getMessage());
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test 
    public void testIncidentDateBeforePolicyCoverageStartIsRejected() {
        // An incident reported before the policy's coverage start date is a POLICY_NOT_YET_ACTIVE business rule violation.
        // Arrange
        int policyNumber = 1003;
        User user = new User(null, null, null);
        LocalDate coverageStart = LocalDate.of(2025, 1, 1);
        LocalDate coverageEnd = LocalDate.of(2026, 6, 30);
        Policy policy = new Policy(policyNumber, user, coverageStart, coverageEnd, new BigDecimal("1000.00"));
        when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(policy));

        LocalDateTime incidentDate = LocalDateTime.of(2024, 12, 31, 9, 30);
        SubmitClaimRequest request = new SubmitClaimRequest(
                policyNumber, incidentDate, new BigDecimal("500.00"), "Test claim description");

        // Act
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class, () -> claimService.submitClaim(request));

        // Assert
        assertEquals(ErrorCode.POLICY_NOT_YET_ACTIVE, ex.getErrorCode());
        assertEquals(
                "Incident date 2024-12-31 is before policy 1003 coverage starts on 2025-01-01.",
                ex.getMessage());
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test 
    public void testCoverageLimitExceededIsRejected() {
        // A claim amount that exceeds the policy's coverage limit is a CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT business rule violation.
        // Arrange
        int policyNumber = 1004;
        User user = new User(null, null, null);
        LocalDate coverageStart = LocalDate.of(2025, 1, 1);
        LocalDate coverageEnd = LocalDate.of(2026, 6, 30);
        BigDecimal coverageLimit = new BigDecimal("1000.00");
        Policy policy = new Policy(policyNumber, user, coverageStart, coverageEnd, coverageLimit);
        when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(policy));

        LocalDateTime incidentDate = LocalDateTime.of(2025, 3, 15, 10, 0);
        BigDecimal incidentAmount = new BigDecimal("1500.00"); // Exceeds coverage limit
        SubmitClaimRequest request = new SubmitClaimRequest(
                policyNumber, incidentDate, incidentAmount, "Test claim description");

        // Act
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class, () -> claimService.submitClaim(request));

        // Assert
        assertEquals(ErrorCode.CLAIM_AMOUNT_EXCEEDS_POLICY_LIMIT, ex.getErrorCode());
        assertEquals(
                "Claim amount 1500.00 exceeds policy 1004 coverage limit of 1000.00.",
                ex.getMessage());
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    public void testClaimAmountEqualsCoverageLimitIsAccepted() {
        // A claim amount that equals the policy's coverage limit should be accepted.
        // Arrange
        int policyNumber = 1005;
        User user = new User(null, null, null);
        LocalDate coverageStart = LocalDate.of(2025, 1, 1);
        LocalDate coverageEnd = LocalDate.of(2026, 6, 30);
        BigDecimal coverageLimit = new BigDecimal("1000.00");
        Policy policy = new Policy(policyNumber, user, coverageStart, coverageEnd, coverageLimit);
        when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(policy));
        when(claimNumberGenerator.next()).thenReturn("CLAIM-002");
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime incidentDate = LocalDateTime.of(2025, 3, 15, 10, 0);
        BigDecimal incidentAmount = new BigDecimal("1000.00"); // Equals coverage limit
        SubmitClaimRequest request = new SubmitClaimRequest(
                policyNumber, incidentDate, incidentAmount, "Test claim description");

        // Act
        ClaimResponse response = claimService.submitClaim(request);

        // Assert
        assertNotNull(response.claimNumber());
        assertEquals(ClaimStatus.SUBMITTED, response.status());
        assertEquals("CLAIM-002", response.claimNumber());
        assertEquals(policyNumber, response.policyNumber());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    public void testIncidentDateEqualsCoverageStartIsAccepted() {
        // An incident date that equals the policy's coverage start date should be accepted.
        // Arrange
        int policyNumber = 1006;
        User user = new User(null, null, null);
        LocalDate coverageStart = LocalDate.of(2025, 1, 1);
        LocalDate coverageEnd = LocalDate.of(2026, 6, 30);
        BigDecimal coverageLimit = new BigDecimal("1000.00");
        Policy policy = new Policy(policyNumber, user, coverageStart, coverageEnd, coverageLimit);
        when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(policy));
        when(claimNumberGenerator.next()).thenReturn("CLAIM-003");
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime incidentDate = LocalDateTime.of(2025, 1, 1, 10, 0); // Equals coverage start
        BigDecimal incidentAmount = new BigDecimal("500.00");
        SubmitClaimRequest request = new SubmitClaimRequest(
                policyNumber, incidentDate, incidentAmount, "Test claim description");

        // Act
        ClaimResponse response = claimService.submitClaim(request);

        // Assert
        assertNotNull(response.claimNumber());
        assertEquals(ClaimStatus.SUBMITTED, response.status());
        assertEquals("CLAIM-003", response.claimNumber());
        assertEquals(policyNumber, response.policyNumber());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test 
    public void testIncidentDateEqualsCoverageEndIsAccepted() {
        // An incident date that equals the policy's coverage end date should be accepted.
        // Arrange
        int policyNumber = 1007;
        User user = new User(null, null, null);
        LocalDate coverageStart = LocalDate.of(2025, 1, 1);
        LocalDate coverageEnd = LocalDate.of(2026, 6, 30);
        BigDecimal coverageLimit = new BigDecimal("1000.00");
        Policy policy = new Policy(policyNumber, user, coverageStart, coverageEnd, coverageLimit);
        when(policyRepository.findByPolicyNumber(policyNumber)).thenReturn(Optional.of(policy));
        when(claimNumberGenerator.next()).thenReturn("CLAIM-004");
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime incidentDate = LocalDateTime.of(2026, 6, 30, 10, 0); // Equals coverage end
        BigDecimal incidentAmount = new BigDecimal("500.00");
        SubmitClaimRequest request = new SubmitClaimRequest(
                policyNumber, incidentDate, incidentAmount, "Test claim description");

        // Act
        ClaimResponse response = claimService.submitClaim(request);

        // Assert
        assertNotNull(response.claimNumber());
        assertEquals(ClaimStatus.SUBMITTED, response.status());
        assertEquals("CLAIM-004", response.claimNumber());
        assertEquals(policyNumber, response.policyNumber());
        verify(claimRepository).save(any(Claim.class));
    }
}

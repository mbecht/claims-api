package com.mbecht.claims_api;

import com.mbecht.claims_api.dto.ClaimSummaryResponse;
import com.mbecht.claims_api.dto.PageResponse;
import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.entity.Policy;
import com.mbecht.claims_api.entity.Role;
import com.mbecht.claims_api.entity.User;
import com.mbecht.claims_api.repository.ClaimRepository;
import com.mbecht.claims_api.repository.ClaimSpecifications;
import com.mbecht.claims_api.service.ClaimFilter;
import com.mbecht.claims_api.service.ClaimService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the claim list filters against a real Postgres (via Testcontainers).
 * Each test is transactional and rolls back, so the seeded rows never leak between tests.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ClaimListIntegrationTest {

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    private static final ClaimFilter NO_FILTERS = new ClaimFilter(null, null, null, null);

    @Autowired
    private ClaimService claimService;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private EntityManager entityManager;

    /*
     * Seed layout (policy 9001 unless noted). Dates are the claim's createdAt.
     *   000001  SUBMITTED  2026-03-10 23:59:59  just before the range
     *   000002  SUBMITTED  2026-03-11 00:00:00  first instant of the range
     *   000003  APPROVED   2026-03-20 12:00:00  middle of the range
     *   000004  SUBMITTED  2026-03-31 23:59:59  last instant of the range
     *   000005  PAID       2026-04-01 00:00:00  just after the range
     *   000006  PAID       2026-03-15 09:00:00  in the range, on policy 9002
     */
    @BeforeEach
    void seedClaims() {
        User holder = new User("list.test.holder", "irrelevant-hash", Role.POLICYHOLDER);
        entityManager.persist(holder);

        Policy policy9001 = new Policy(
                9001, holder, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(10), new BigDecimal("10000.00"));
        Policy policy9002 = new Policy(
                9002, holder, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(10), new BigDecimal("10000.00"));
        entityManager.persist(policy9001);
        entityManager.persist(policy9002);

        persistClaim("CLM-TEST-000001", policy9001, ClaimStatus.SUBMITTED, LocalDateTime.parse("2026-03-10T23:59:59"));
        persistClaim("CLM-TEST-000002", policy9001, ClaimStatus.SUBMITTED, LocalDateTime.parse("2026-03-11T00:00:00"));
        persistClaim("CLM-TEST-000003", policy9001, ClaimStatus.APPROVED, LocalDateTime.parse("2026-03-20T12:00:00"));
        persistClaim("CLM-TEST-000004", policy9001, ClaimStatus.SUBMITTED, LocalDateTime.parse("2026-03-31T23:59:59"));
        persistClaim("CLM-TEST-000005", policy9001, ClaimStatus.PAID, LocalDateTime.parse("2026-04-01T00:00:00"));
        persistClaim("CLM-TEST-000006", policy9002, ClaimStatus.PAID, LocalDateTime.parse("2026-03-15T09:00:00"));

        // Clear the persistence context so queries read the values we just wrote to the database.
        entityManager.flush();
        entityManager.clear();
    }

    // --- Status ---

    @Test
    void noFilter_returnsEveryClaim() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(NO_FILTERS, FIRST_PAGE);

        assertThat(page.totalElements()).isEqualTo(6);
        assertThat(page.content()).hasSize(6);
    }

    @Test
    void statusFilter_returnsOnlyClaimsWithThatStatus() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(ClaimStatus.SUBMITTED, null, null, null), FIRST_PAGE);

        assertThat(page.content())
                .extracting(ClaimSummaryResponse::claimNumber)
                .containsExactlyInAnyOrder("CLM-TEST-000001", "CLM-TEST-000002", "CLM-TEST-000004");
    }

    @Test
    void statusFilter_withNoMatchingClaims_returnsEmptyPage() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(ClaimStatus.DENIED, null, null, null), FIRST_PAGE);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    // --- Policy number ---

    @Test
    void policyNumberFilter_returnsOnlyClaimsOnThatPolicy() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, 9002, null, null), FIRST_PAGE);

        assertThat(page.content())
                .extracting(ClaimSummaryResponse::claimNumber)
                .containsExactly("CLM-TEST-000006");
    }

    @Test
    void policyNumberFilter_forUnknownPolicy_returnsEmptyPage() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, 9999, null, null), FIRST_PAGE);

        assertThat(page.content()).isEmpty();
    }

    // --- Inclusive submission date range ---

    @Test
    void dateRange_includesBothEndDaysAndExcludesTheDaysOutside() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, null, LocalDate.parse("2026-03-11"), LocalDate.parse("2026-03-31")), FIRST_PAGE);

        assertThat(page.content())
                .extracting(ClaimSummaryResponse::claimNumber)
                .containsExactlyInAnyOrder("CLM-TEST-000002", "CLM-TEST-000003", "CLM-TEST-000004", "CLM-TEST-000006");
    }

    @Test
    void dateRange_fromAndToOnTheSameDay_includesThatWholeDay() {
        // 000004 was submitted at 23:59:59 on the 31st, so this proves the final day is not cut short.
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, null, LocalDate.parse("2026-03-31"), LocalDate.parse("2026-03-31")), FIRST_PAGE);

        assertThat(page.content())
                .extracting(ClaimSummaryResponse::claimNumber)
                .containsExactly("CLM-TEST-000004");
    }

    @Test
    void dateRange_onlyFrom_matchesEverythingOnOrAfterThatDay() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, null, LocalDate.parse("2026-03-11"), null), FIRST_PAGE);

        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    void dateRange_onlyTo_matchesEverythingOnOrBeforeThatDay() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, null, null, LocalDate.parse("2026-03-10")), FIRST_PAGE);

        assertThat(page.content())
                .extracting(ClaimSummaryResponse::claimNumber)
                .containsExactly("CLM-TEST-000001");
    }

    @Test
    void dateRange_withFromAfterTo_returnsEmptyPage() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, null, LocalDate.parse("2026-03-31"), LocalDate.parse("2026-03-11")), FIRST_PAGE);

        assertThat(page.content()).isEmpty();
    }

    // --- Combinations ---

    @Test
    void allFiltersTogether_returnsOnlyClaimsMatchingEveryFilter() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(ClaimStatus.SUBMITTED, 9001,
                        LocalDate.parse("2026-03-11"), LocalDate.parse("2026-03-31")),
                FIRST_PAGE);

        assertThat(page.content())
                .extracting(ClaimSummaryResponse::claimNumber)
                .containsExactlyInAnyOrder("CLM-TEST-000002", "CLM-TEST-000004");
    }

    @Test
    void filtersCombineWithPaging() {
        PageResponse<ClaimSummaryResponse> secondPage = claimService.listClaims(
                new ClaimFilter(null, 9001, LocalDate.parse("2026-03-11"), LocalDate.parse("2026-03-31")),
                PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        // Three claims match (000002, 000003, 000004), so page 2 of size 2 holds the last one.
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.content()).hasSize(1);
    }

    @Test
    void summaryIncludesPolicyNumberAndSubmittedAt() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(null, 9002, null, null), FIRST_PAGE);

        assertThat(page.content()).singleElement().satisfies(claim -> {
            assertThat(claim.policyNumber()).isEqualTo(9002);
            assertThat(claim.submittedAt()).isEqualTo(LocalDateTime.parse("2026-03-15T09:00:00"));
        });
    }

    // --- N+1 avoidance ---

    @Test
    void searchLoadsEachClaimsPolicyInTheSameQuery() {
        Page<Claim> page = claimRepository.findAll(ClaimSpecifications.matching(NO_FILTERS), FIRST_PAGE);
        PersistenceUnitUtil persistenceUnit = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        // If the policy were a lazy proxy here, it would cost one extra SELECT per claim when read.
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent())
                .allSatisfy(claim -> assertThat(persistenceUnit.isLoaded(claim, "policy")).isTrue());
    }

    private void persistClaim(String claimNumber, Policy policy, ClaimStatus status, LocalDateTime createdAt) {
        Claim claim = new Claim(claimNumber, policy, createdAt.minusDays(1), new BigDecimal("100.00"), "Test claim");
        entityManager.persist(claim);
        entityManager.flush();

        // The Claim entity has no status or createdAt setters, so set them directly for the test.
        entityManager.createNativeQuery(
                        "UPDATE claims SET status = :status, created_at = :createdAt WHERE claim_number = :claimNumber")
                .setParameter("status", status.name())
                .setParameter("createdAt", createdAt)
                .setParameter("claimNumber", claimNumber)
                .executeUpdate();
    }
}

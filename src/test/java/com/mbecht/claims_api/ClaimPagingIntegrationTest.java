package com.mbecht.claims_api;

import com.mbecht.claims_api.dto.ClaimSummaryResponse;
import com.mbecht.claims_api.dto.PageResponse;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.service.ClaimFilter;
import com.mbecht.claims_api.service.ClaimService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Paging, sorting and filtering over 50 claims on a real Postgres (via Testcontainers).
 * The fixture is loaded from src/test/resources before each test and rolled back afterwards.
 *
 * Fixture layout (claim n is CLM-2026-00000n):
 *   - Even n is on policy 1004, odd n on policy 1005; each policy has 25 claims.
 *   - Amount is n * 100, so amount order matches claim-number order.
 *   - Incident date is 2026-01-01 + 5n days, and submission is 2026-02-01 + n days.
 *   - Status: 1-25 SUBMITTED, 26-35 UNDER_REVIEW, 36-40 APPROVED, 41-43 DENIED, 44-50 PAID.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@Sql("classpath:db/test-data/claims-paging.sql")
class ClaimPagingIntegrationTest {

    private static final ClaimFilter NO_FILTERS = new ClaimFilter(null, null, null, null);
    private static final Sort BY_AMOUNT_ASC = Sort.by(Sort.Direction.ASC, "amount");

    @Autowired
    private ClaimService claimService;

    // --- Paging ---

    @Test
    void firstPageOfDefaultSize_holds20ClaimsAndReportsTheWholeSet() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(NO_FILTERS, page(0, 20, submittedDesc()));

        assertThat(page.content()).hasSize(20);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(50);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void lastPage_holdsTheRemainingClaims() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(NO_FILTERS, page(2, 20, submittedDesc()));

        // 50 claims at 20 per page leaves 10 for the third page.
        assertThat(page.content()).hasSize(10);
        assertThat(page.page()).isEqualTo(2);
    }

    @Test
    void pageBeyondTheLast_isEmptyButStillReportsTheTotals() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(NO_FILTERS, page(3, 20, submittedDesc()));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(50);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void consecutivePages_coverEveryClaimExactlyOnceInSortOrder() {
        // Size 7 does not divide 50, so the last page is short. Amounts are unique, so the order is fully determined.
        PageResponse<ClaimSummaryResponse> first = claimService.listClaims(NO_FILTERS, page(0, 7, BY_AMOUNT_ASC));

        List<String> collected = new ArrayList<>(claimNumbers(first));
        for (int p = 1; p < first.totalPages(); p++) {
            collected.addAll(claimNumbers(claimService.listClaims(NO_FILTERS, page(p, 7, BY_AMOUNT_ASC))));
        }

        assertThat(first.totalPages()).isEqualTo(8);
        assertThat(collected).containsExactlyElementsOf(claimNumbersOneTo(50));
    }

    @Test
    void policyFilter_pagesWithinOnePolicy() {
        // Each policy has 25 claims, so at the default size of 20 the policy needs two pages.
        ClaimFilter onPolicy1004 = new ClaimFilter(null, 1004, null, null);

        PageResponse<ClaimSummaryResponse> first = claimService.listClaims(onPolicy1004, page(0, 20, submittedDesc()));
        PageResponse<ClaimSummaryResponse> second = claimService.listClaims(onPolicy1004, page(1, 20, submittedDesc()));

        assertThat(first.totalElements()).isEqualTo(25);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.content()).hasSize(20);
        assertThat(second.content()).hasSize(5);
    }

    // --- Sorting ---

    @Test
    void defaultSortBySubmittedAtDescending_putsTheNewestClaimFirst() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(NO_FILTERS, page(0, 20, submittedDesc()));

        assertThat(claimNumbers(page)).startsWith("CLM-2026-000050", "CLM-2026-000049", "CLM-2026-000048");
        assertThat(claimNumbers(page)).endsWith("CLM-2026-000033", "CLM-2026-000032", "CLM-2026-000031");
    }

    @Test
    void sortByAmountDescending_putsTheLargestClaimFirst() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                NO_FILTERS, page(0, 20, Sort.by(Sort.Direction.DESC, "amount")));

        assertThat(page.content()).first().satisfies(claim -> {
            assertThat(claim.claimNumber()).isEqualTo("CLM-2026-000050");
            assertThat(claim.amount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        });
    }

    @Test
    void sortByIncidentDateAscending_putsTheEarliestIncidentFirst() {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                NO_FILTERS, page(0, 20, Sort.by(Sort.Direction.ASC, "incidentDate")));

        assertThat(page.content()).first().satisfies(claim -> {
            assertThat(claim.claimNumber()).isEqualTo("CLM-2026-000001");
            assertThat(claim.incidentDate()).isEqualTo(LocalDateTime.parse("2026-01-06T00:00:00"));
        });
    }

    @Test
    void sortingHoldsAcrossPageBoundaries() {
        // Page 0 and page 1 of size 25 should split the claims exactly at the midpoint of the sort.
        Sort byIncidentAsc = Sort.by(Sort.Direction.ASC, "incidentDate");

        PageResponse<ClaimSummaryResponse> first = claimService.listClaims(NO_FILTERS, page(0, 25, byIncidentAsc));
        PageResponse<ClaimSummaryResponse> second = claimService.listClaims(NO_FILTERS, page(1, 25, byIncidentAsc));

        assertThat(claimNumbers(first)).containsExactlyElementsOf(claimNumbersOneTo(25));
        assertThat(claimNumbers(second)).containsExactlyElementsOf(
                IntStream.rangeClosed(26, 50).mapToObj(ClaimPagingIntegrationTest::claimNumber).toList());
    }

    // --- Status filter ---

    @ParameterizedTest
    @CsvSource({
            "SUBMITTED, 25",
            "UNDER_REVIEW, 10",
            "APPROVED, 5",
            "DENIED, 3",
            "PAID, 7"
    })
    void statusFilter_countsEachStatusAcrossTheWholeSet(ClaimStatus status, long expectedCount) {
        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(
                new ClaimFilter(status, null, null, null), page(0, 20, submittedDesc()));

        assertThat(page.totalElements()).isEqualTo(expectedCount);
        assertThat(page.content()).allSatisfy(claim -> assertThat(claim.status()).isEqualTo(status));
    }

    @Test
    void statusFilter_pagesOnlyThroughMatchingClaims() {
        // 25 SUBMITTED claims at size 10 gives three pages, and the last one holds 5.
        ClaimFilter submitted = new ClaimFilter(ClaimStatus.SUBMITTED, null, null, null);

        PageResponse<ClaimSummaryResponse> last = claimService.listClaims(submitted, page(2, 10, BY_AMOUNT_ASC));

        assertThat(last.totalPages()).isEqualTo(3);
        assertThat(last.content()).hasSize(5);
        assertThat(claimNumbers(last)).containsExactly(
                "CLM-2026-000021", "CLM-2026-000022", "CLM-2026-000023", "CLM-2026-000024", "CLM-2026-000025");
    }

    // --- Combined filters with paging ---

    @Test
    void policyAndStatusFilters_combineAndPage() {
        // Policy 1004 has 12 SUBMITTED claims (the even numbers 2 to 24). At size 5 that is three pages.
        ClaimFilter submittedOn1004 = new ClaimFilter(ClaimStatus.SUBMITTED, 1004, null, null);

        PageResponse<ClaimSummaryResponse> last = claimService.listClaims(submittedOn1004, page(2, 5, BY_AMOUNT_ASC));

        assertThat(last.totalElements()).isEqualTo(12);
        assertThat(last.totalPages()).isEqualTo(3);
        assertThat(claimNumbers(last)).containsExactly("CLM-2026-000022", "CLM-2026-000024");
    }

    @Test
    void policyAndStatusFilters_returnOnlyTheMatchingClaims() {
        // On policy 1005, the DENIED claims are 41 and 43.
        ClaimFilter deniedOn1005 = new ClaimFilter(ClaimStatus.DENIED, 1005, null, null);

        PageResponse<ClaimSummaryResponse> page = claimService.listClaims(deniedOn1005, page(0, 20, BY_AMOUNT_ASC));

        assertThat(claimNumbers(page)).containsExactly("CLM-2026-000041", "CLM-2026-000043");
        assertThat(page.content()).allSatisfy(claim -> assertThat(claim.policyNumber()).isEqualTo(1005));
    }

    // --- Helpers ---

    private static Pageable page(int page, int size, Sort sort) {
        return PageRequest.of(page, size, sort);
    }

    private static Sort submittedDesc() {
        return Sort.by(Sort.Direction.DESC, "submittedAt");
    }

    private static List<String> claimNumbers(PageResponse<ClaimSummaryResponse> page) {
        return page.content().stream().map(ClaimSummaryResponse::claimNumber).toList();
    }

    private static List<String> claimNumbersOneTo(int last) {
        return IntStream.rangeClosed(1, last).mapToObj(ClaimPagingIntegrationTest::claimNumber).toList();
    }

    private static String claimNumber(int n) {
        return "CLM-2026-%06d".formatted(n);
    }
}

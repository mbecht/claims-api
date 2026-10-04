package com.mbecht.claims_api.repository;

import com.mbecht.claims_api.entity.Claim;
import com.mbecht.claims_api.entity.ClaimStatus;
import com.mbecht.claims_api.service.ClaimFilter;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One predicate per claim filter. Each returns null when its filter is not supplied,
 * which tells Spring Data to add no WHERE condition for it. Predicates are chained with
 * and(), so zero, one or several filters all go through the same code path.
 */
public final class ClaimSpecifications {

    private ClaimSpecifications() {
    }

    /** Combines every filter; unset filters contribute no condition. */
    public static Specification<Claim> matching(ClaimFilter filter) {
        return hasStatus(filter.status())
                .and(hasPolicyNumber(filter.policyNumber()))
                .and(submittedOnOrAfter(filter.submittedFrom()))
                .and(submittedOnOrBefore(filter.submittedTo()));
    }

    public static Specification<Claim> hasStatus(ClaimStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Claim> hasPolicyNumber(Integer policyNumber) {
        return (root, query, cb) -> policyNumber == null
                ? null
                : cb.equal(root.get("policy").get("policyNumber"), policyNumber);
    }

    /** Inclusive: a claim submitted at any time on {@code from} or later matches. */
    public static Specification<Claim> submittedOnOrAfter(LocalDate from) {
        return (root, query, cb) -> from == null
                ? null
                : cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), from.atStartOfDay());
    }

    /**
     * Inclusive: a claim submitted at any time on {@code to} matches. Compared as
     * "before the start of the following day" so the whole final day is included.
     */
    public static Specification<Claim> submittedOnOrBefore(LocalDate to) {
        return (root, query, cb) -> to == null
                ? null
                : cb.lessThan(root.<LocalDateTime>get("createdAt"), to.plusDays(1).atStartOfDay());
    }
}

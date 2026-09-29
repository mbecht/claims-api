package com.mbecht.claims_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true)
    private Integer policyNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holder_id", nullable = false)
    private User holder;

    @Column(name = "coverage_start", nullable = false)
    private LocalDate coverageStart;

    @Column(name = "coverage_end", nullable = false)
    private LocalDate coverageEnd;

    @Column(name = "coverage_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal coverageLimit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Policy() {
    }

    public Policy(Integer policyNumber, User holder, LocalDate coverageStart, LocalDate coverageEnd, BigDecimal coverageLimit) {
        this.policyNumber = policyNumber;
        this.holder = holder;
        this.coverageStart = coverageStart;
        this.coverageEnd = coverageEnd;
        this.coverageLimit = coverageLimit;
    }

    public Long getId() {
        return id;
    }

    public Integer getPolicyNumber() {
        return policyNumber;
    }

    public User getHolder() {
        return holder;
    }

    public LocalDate getCoverageStart() {
        return coverageStart;
    }

    public LocalDate getCoverageEnd() {
        return coverageEnd;
    }

    public BigDecimal getCoverageLimit() {
        return coverageLimit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

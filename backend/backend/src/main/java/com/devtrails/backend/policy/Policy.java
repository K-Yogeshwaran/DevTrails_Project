package com.devtrails.backend.policy;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique policy number — format: POL-WORKID-YYYYMMDD
    @Column(name = "policy_number", unique = true, nullable = false, length = 30)
    private String policyNumber;

    @Column(name = "worker_id", nullable = false, length = 20)
    private String workerId;

    // "basic" | "standard" | "premium"
    @Column(name = "tier", nullable = false, length = 20)
    private String tier;

    @Column(name = "weekly_premium", nullable = false, precision = 8, scale = 2)
    private BigDecimal weeklyPremium;

    @Column(name = "coverage_cap", nullable = false, precision = 10, scale = 2)
    private BigDecimal coverageCap;

    @Column(name = "coverage_used", precision = 10, scale = 2)
    private BigDecimal coverageUsed = BigDecimal.ZERO;

    @Column(name = "season", nullable = false, length = 20)
    private String season;

    @Column(name = "status", length = 20)
    private String status = "active";

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Policy() {}

    public Policy(Long id, String policyNumber, String workerId, String tier, BigDecimal weeklyPremium, BigDecimal coverageCap, BigDecimal coverageUsed, String season, String status, LocalDate weekStart, LocalDate weekEnd, LocalDateTime createdAt) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.workerId = workerId;
        this.tier = tier;
        this.weeklyPremium = weeklyPremium;
        this.coverageCap = coverageCap;
        this.coverageUsed = coverageUsed;
        this.season = season;
        this.status = status;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.coverageUsed == null) {
            this.coverageUsed = BigDecimal.ZERO;
        }
        if (this.status == null) {
            this.status = "active";
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public BigDecimal getWeeklyPremium() { return weeklyPremium; }
    public void setWeeklyPremium(BigDecimal weeklyPremium) { this.weeklyPremium = weeklyPremium; }
    public BigDecimal getCoverageCap() { return coverageCap; }
    public void setCoverageCap(BigDecimal coverageCap) { this.coverageCap = coverageCap; }
    public BigDecimal getCoverageUsed() { return coverageUsed; }
    public void setCoverageUsed(BigDecimal coverageUsed) { this.coverageUsed = coverageUsed; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }
    public LocalDate getWeekEnd() { return weekEnd; }
    public void setWeekEnd(LocalDate weekEnd) { this.weekEnd = weekEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
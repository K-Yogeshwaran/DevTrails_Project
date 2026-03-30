package com.devtrails.backend.policy;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique policy number — format: POL-WORKID-YYYYMMDD
    // e.g. POL-GS3F4A8B-20260318
    @Column(name = "policy_number", unique = true, nullable = false, length = 30)
    private String policyNumber;

    // Foreign key to workers table
    // References worker_id column in workers table
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
}
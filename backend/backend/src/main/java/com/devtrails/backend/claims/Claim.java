package com.devtrails.backend.claims;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique claim ID — format: CLM-WORKID-TIMESTAMP
    // e.g. CLM-GS3F4A8B-1710758400
    @Column(name = "claim_id", unique = true, nullable = false, length = 30)
    private String claimId;


    @Column(name = "worker_id", nullable = false, length = 20)
    private String workerId;

    @Column(name = "policy_number", nullable = false, length = 30)
    private String policyNumber;


    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;


    @Column(name = "trigger_value", precision = 8, scale = 2)
    private BigDecimal triggerValue;


    @Column(name = "zone_id", nullable = false, length = 60)
    private String zoneId;

    @Column(name = "disrupted_hours", nullable = false, precision = 4, scale = 1)
    private BigDecimal disruptedHours;

    @Column(name = "payout_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutAmount;


    @Column(name = "fraud_score", precision = 4, scale = 3)
    private BigDecimal fraudScore = BigDecimal.ZERO;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

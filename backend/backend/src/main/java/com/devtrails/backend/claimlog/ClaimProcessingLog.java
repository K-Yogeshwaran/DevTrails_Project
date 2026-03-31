package com.devtrails.backend.claimlog;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_processing_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false, length = 50)
    private String claimId;

    // Stage name: queued | policy_check | ml_calculation | fraud_check | coverage_cap | approved | flagged
    @Column(name = "stage", nullable = false, length = 30)
    private String stage;

    // Status of this stage: processing | done | failed
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // Human-readable detail shown in the UI
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static ClaimProcessingLog of(String claimId, String stage, String status, String detail) {
        ClaimProcessingLog log = new ClaimProcessingLog();
        log.setClaimId(claimId);
        log.setStage(stage);
        log.setStatus(status);
        log.setDetail(detail);
        return log;
    }
}

package com.devtrails.backend.claimlog;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_processing_logs")
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

    public ClaimProcessingLog() {}

    public ClaimProcessingLog(Long id, String claimId, String stage, String status, String detail, LocalDateTime createdAt) {
        this.id = id;
        this.claimId = claimId;
        this.stage = stage;
        this.status = status;
        this.detail = detail;
        this.createdAt = createdAt;
    }

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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

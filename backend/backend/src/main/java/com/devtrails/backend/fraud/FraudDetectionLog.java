package com.devtrails.backend.fraud;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_detection_logs")
public class FraudDetectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false, length = 60)
    private String claimId;

    @Column(name = "worker_id", nullable = false, length = 20)
    private String workerId;

    @Column(name = "detection_type", nullable = false, length = 50)
    private String detectionType;

    // ✅ FIXED: Changed Double → BigDecimal
    @Column(name = "risk_score", precision = 4, scale = 3)
    private BigDecimal riskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "detection_details", columnDefinition = "TEXT")
    private String detectionDetails;

    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 100)
    private String deviceFingerprint;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public FraudDetectionLog() {}

    public FraudDetectionLog(Long id, String claimId, String workerId, String detectionType,
                             BigDecimal riskScore, String riskLevel, String detectionDetails,
                             String actionTaken, String ipAddress, String userAgent,
                             String deviceFingerprint, LocalDateTime createdAt) {
        this.id = id;
        this.claimId = claimId;
        this.workerId = workerId;
        this.detectionType = detectionType;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.detectionDetails = detectionDetails;
        this.actionTaken = actionTaken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceFingerprint = deviceFingerprint;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ✅ Factory method
    public static FraudDetectionLog of(String claimId, String workerId, String detectionType,
                                       BigDecimal riskScore, String detectionDetails, String actionTaken) {
        FraudDetectionLog log = new FraudDetectionLog();
        log.setClaimId(claimId);
        log.setWorkerId(workerId);
        log.setDetectionType(detectionType);
        log.setRiskScore(riskScore);
        log.setRiskLevel(determineRiskLevel(riskScore));
        log.setDetectionDetails(detectionDetails);
        log.setActionTaken(actionTaken);
        return log;
    }

    // ✅ FIXED: BigDecimal comparison
    private static String determineRiskLevel(BigDecimal score) {
        if (score == null) return "LOW";

        if (score.compareTo(new BigDecimal("0.8")) >= 0) return "CRITICAL";
        if (score.compareTo(new BigDecimal("0.6")) >= 0) return "HIGH";
        if (score.compareTo(new BigDecimal("0.4")) >= 0) return "MEDIUM";

        return "LOW";
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getDetectionType() { return detectionType; }
    public void setDetectionType(String detectionType) { this.detectionType = detectionType; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getDetectionDetails() { return detectionDetails; }
    public void setDetectionDetails(String detectionDetails) { this.detectionDetails = detectionDetails; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
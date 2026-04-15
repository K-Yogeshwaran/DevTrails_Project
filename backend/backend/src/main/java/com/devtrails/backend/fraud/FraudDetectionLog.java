package com.devtrails.backend.fraud;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_detection_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false, length = 60)
    private String claimId;

    @Column(name = "worker_id", nullable = false, length = 20)
    private String workerId;

    @Column(name = "detection_type", nullable = false, length = 50)
    private String detectionType; // GPS_SPOOFING, WEATHER_ANOMALY, PATTERN_ANALYSIS, etc.

    @Column(name = "risk_score", precision = 4, scale = 3)
    private Double riskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "detection_details", columnDefinition = "TEXT")
    private String detectionDetails;

    @Column(name = "action_taken", length = 50)
    private String actionTaken; // FLAGGED, AUTO_REJECTED, MANUAL_REVIEW, etc.

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 100)
    private String deviceFingerprint;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static FraudDetectionLog of(String claimId, String workerId, String detectionType,
                                     Double riskScore, String detectionDetails, String actionTaken) {
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

    private static String determineRiskLevel(Double score) {
        if (score == null) return "LOW";
        if (score >= 0.8) return "CRITICAL";
        if (score >= 0.6) return "HIGH";
        if (score >= 0.4) return "MEDIUM";
        return "LOW";
    }
}

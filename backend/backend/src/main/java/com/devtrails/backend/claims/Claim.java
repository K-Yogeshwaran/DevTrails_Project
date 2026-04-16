package com.devtrails.backend.claims;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", unique = true, nullable = false, length = 60)
    private String claimId;

    @Column(name = "worker_id", nullable = false, length = 20)
    private String workerId;

    @Column(name = "policy_number", nullable = false, length = 60)
    private String policyNumber;

    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;

    @Column(name = "trigger_value", precision = 8, scale = 2)
    private BigDecimal triggerValue;

    @Column(name = "zone_id", nullable = false, length = 60)
    private String zoneId;

    @Column(name = "event_id", length = 120)
    private String eventId;

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

    @Column(name = "gps_latitude", precision = 10, scale = 7)
    private BigDecimal gpsLatitude;

    @Column(name = "gps_longitude", precision = 10, scale = 7)
    private BigDecimal gpsLongitude;

    @Column(name = "weather_actual")
    private String weatherActual;

    @Column(name = "weather_validated")
    private Boolean weatherValidated;

    public Claim() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public BigDecimal getTriggerValue() { return triggerValue; }
    public void setTriggerValue(BigDecimal triggerValue) { this.triggerValue = triggerValue; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public BigDecimal getDisruptedHours() { return disruptedHours; }
    public void setDisruptedHours(BigDecimal disruptedHours) { this.disruptedHours = disruptedHours; }
    public BigDecimal getPayoutAmount() { return payoutAmount; }
    public void setPayoutAmount(BigDecimal payoutAmount) { this.payoutAmount = payoutAmount; }
    public BigDecimal getFraudScore() { return fraudScore; }
    public void setFraudScore(BigDecimal fraudScore) { this.fraudScore = fraudScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public BigDecimal getGpsLatitude() { return gpsLatitude; }
    public void setGpsLatitude(BigDecimal gpsLatitude) { this.gpsLatitude = gpsLatitude; }
    public BigDecimal getGpsLongitude() { return gpsLongitude; }
    public void setGpsLongitude(BigDecimal gpsLongitude) { this.gpsLongitude = gpsLongitude; }
    public String getWeatherActual() { return weatherActual; }
    public void setWeatherActual(String weatherActual) { this.weatherActual = weatherActual; }
    public Boolean getWeatherValidated() { return weatherValidated; }
    public void setWeatherValidated(Boolean weatherValidated) { this.weatherValidated = weatherValidated; }
}

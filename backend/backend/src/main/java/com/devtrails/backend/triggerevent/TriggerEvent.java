package com.devtrails.backend.triggerevent;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trigger_events")
public class TriggerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true, nullable = false, length = 120)
    private String eventId;

    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;

    @Column(name = "zone_id", nullable = false, length = 60)
    private String zoneId;

    @Column(name = "zone_name", length = 60)
    private String zoneName;

    // Measured value at trigger time (e.g. 45mm rainfall, 250 AQI)
    @Column(name = "trigger_value", precision = 8, scale = 2)
    private BigDecimal triggerValue;

    // "active" while disruption ongoing, "resolved" when condition normalises
    @Column(name = "status", length = 20)
    private String status = "active";

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // Calculated on resolve: (ended_at - started_at) in hours
    @Column(name = "disrupted_hours", precision = 5, scale = 2)
    private BigDecimal disruptedHours;

    // Comma-separated worker_ids online when disruption ended
    @Column(name = "affected_worker_ids", columnDefinition = "TEXT")
    private String affectedWorkerIds;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public TriggerEvent() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public BigDecimal getTriggerValue() { return triggerValue; }
    public void setTriggerValue(BigDecimal triggerValue) { this.triggerValue = triggerValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public BigDecimal getDisruptedHours() { return disruptedHours; }
    public void setDisruptedHours(BigDecimal disruptedHours) { this.disruptedHours = disruptedHours; }
    public String getAffectedWorkerIds() { return affectedWorkerIds; }
    public void setAffectedWorkerIds(String affectedWorkerIds) { this.affectedWorkerIds = affectedWorkerIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

package com.devtrails.backend.triggerevent;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trigger_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

package com.devtrails.backend.triggerevent;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TriggerEventDTO {

    // ── POST /api/trigger-events  (called by Python trigger engine) ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String eventId;
        private String triggerType;
        private String zoneId;
        private String zoneName;
        private Double triggerValue;
    }

    // ── POST /api/trigger-events/resolve ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveRequest {
        private String eventId;
        private List<String> activeWorkerIds;
    }

    // ── Response sent to frontend ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventResponse {
        private String eventId;
        private String triggerType;
        private String zoneId;
        private String zoneName;
        private BigDecimal triggerValue;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private BigDecimal disruptedHours;
        private String affectedWorkerIds;
        // Computed field — seconds since startedAt, for the live timer on dashboard
        private Long elapsedSeconds;
        private String message;
    }
}

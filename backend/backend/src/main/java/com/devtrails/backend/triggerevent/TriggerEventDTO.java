package com.devtrails.backend.triggerevent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TriggerEventDTO {

    // ── POST /api/trigger-events  (called by Python trigger engine) ──
    public static class CreateRequest {
        private String eventId;
        private String triggerType;
        private String zoneId;
        private String zoneName;
        private Double triggerValue;

        public CreateRequest() {}

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public String getZoneId() { return zoneId; }
        public void setZoneId(String zoneId) { this.zoneId = zoneId; }
        public String getZoneName() { return zoneName; }
        public void setZoneName(String zoneName) { this.zoneName = zoneName; }
        public Double getTriggerValue() { return triggerValue; }
        public void setTriggerValue(Double triggerValue) { this.triggerValue = triggerValue; }
    }

    // ── POST /api/trigger-events/resolve ──
    public static class ResolveRequest {
        private String eventId;
        private List<String> activeWorkerIds;

        public ResolveRequest() {}

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public List<String> getActiveWorkerIds() { return activeWorkerIds; }
        public void setActiveWorkerIds(List<String> activeWorkerIds) { this.activeWorkerIds = activeWorkerIds; }
    }

    // ── Response sent to frontend ──
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
        private Long elapsedSeconds;
        private String message;

        public EventResponse() {}

        public EventResponse(String eventId, String triggerType, String zoneId, String zoneName,
                             BigDecimal triggerValue, String status, LocalDateTime startedAt,
                             LocalDateTime endedAt, BigDecimal disruptedHours, String affectedWorkerIds,
                             Long elapsedSeconds, String message) {
            this.eventId = eventId;
            this.triggerType = triggerType;
            this.zoneId = zoneId;
            this.zoneName = zoneName;
            this.triggerValue = triggerValue;
            this.status = status;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.disruptedHours = disruptedHours;
            this.affectedWorkerIds = affectedWorkerIds;
            this.elapsedSeconds = elapsedSeconds;
            this.message = message;
        }

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
        public Long getElapsedSeconds() { return elapsedSeconds; }
        public void setElapsedSeconds(Long elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}

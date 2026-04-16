package com.devtrails.backend.dashboard;

import java.time.LocalDateTime;

public class DashboardUpdate {
    private String updateType;
    private String targetId; // workerId or "insurer"
    private Object data;
    private LocalDateTime timestamp;
    private String source;

    public DashboardUpdate() {}

    public DashboardUpdate(String updateType, String targetId, Object data, LocalDateTime timestamp, String source) {
        this.updateType = updateType;
        this.targetId = targetId;
        this.data = data;
        this.timestamp = timestamp;
        this.source = source;
    }

    public static DashboardUpdate workerUpdate(String workerId, Object data) {
        return new DashboardUpdate("WORKER", workerId, data, LocalDateTime.now(), "CLAIM_SERVICE");
    }
    
    public static DashboardUpdate insurerUpdate(Object data) {
        return new DashboardUpdate("INSURER", "insurer", data, LocalDateTime.now(), "CLAIM_SERVICE");
    }

    // Getters and Setters
    public String getUpdateType() { return updateType; }
    public void setUpdateType(String updateType) { this.updateType = updateType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

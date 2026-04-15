package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUpdate {
    private String updateType;
    private String targetId; // workerId or "insurer"
    private Object data;
    private LocalDateTime timestamp;
    private String source;
    
    public static DashboardUpdate workerUpdate(String workerId, Object data) {
        return new DashboardUpdate("WORKER", workerId, data, LocalDateTime.now(), "CLAIM_SERVICE");
    }
    
    public static DashboardUpdate insurerUpdate(Object data) {
        return new DashboardUpdate("INSURER", "insurer", data, LocalDateTime.now(), "CLAIM_SERVICE");
    }
}

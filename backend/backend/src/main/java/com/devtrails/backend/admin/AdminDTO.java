package com.devtrails.backend.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlaggedClaim {
        private String        claimId;
        private String        workerId;
        private String        workerName;
        private String        workerPhone;
        private String        triggerType;
        private String        zoneId;
        private BigDecimal    payoutAmount;
        private BigDecimal    fraudScore;
        private LocalDateTime triggeredAt;
        private LocalDateTime processedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LossRatioInfo {
        private BigDecimal totalPremiums;
        private BigDecimal totalClaimsPaid;
        private double lossRatio;
        private java.util.List<MonthlyStats> history;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStats {
        private String month;
        private BigDecimal premiums;
        private BigDecimal claims;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictiveAnalytics {
        private java.util.List<ZoneRisk> zoneRisks;
        private BigDecimal estimatedNextWeekPayout;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneRisk {
        private String zoneId;
        private String zoneName;
        private String primaryRisk; // e.g., "Heavy Rain", "Heatwave"
        private double riskProbability; // 0.0 to 1.0
        private int affectedWorkers;
    }
}

package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveAnalytics {
    private String triggerType;
    private Long claimCount;
    private Double avgDisruptedHours;
    private BigDecimal totalPayout;
    private Integer dayOfWeek;
    private Integer hourOfDay;
    private Double riskScore;
    private String prediction;
    private LocalDateTime lastUpdated;
    
    // Risk calculation
    public Double calculateRiskScore() {
        if (claimCount == null) return 0.0;
        double baseRisk = claimCount > 10 ? 0.8 : claimCount > 5 ? 0.6 : claimCount > 2 ? 0.4 : 0.2;
        double timeRisk = (hourOfDay != null && (hourOfDay >= 22 || hourOfDay <= 6)) ? 0.3 : 0.1;
        double dayRisk = (dayOfWeek != null && (dayOfWeek == 0 || dayOfWeek == 6)) ? 0.2 : 0.1;
        return Math.min(baseRisk + timeRisk + dayRisk, 1.0);
    }
    
    public String generatePrediction() {
        double risk = calculateRiskScore();
        if (risk > 0.8) return "HIGH_RISK";
        if (risk > 0.6) return "MEDIUM_RISK";
        if (risk > 0.4) return "LOW_RISK";
        return "VERY_LOW_RISK";
    }
}

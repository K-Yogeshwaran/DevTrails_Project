package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PredictiveAnalytics(
    String triggerType,
    Long claimCount,
    Double avgDisruptedHours,
    BigDecimal totalPayout,
    Integer dayOfWeek,
    Integer hourOfDay,
    Double riskScore,
    String prediction,
    LocalDateTime lastUpdated
) {
    // Custom constructors or conversion logic
    public PredictiveAnalytics() {
        this(null, 0L, 0.0, BigDecimal.ZERO, 0, 0, 0.0, null, LocalDateTime.now());
    }

    public Double calculateRiskScore() {
        if (claimCount == null) return 0.2;
        double baseRisk = claimCount > 10 ? 0.8 : claimCount > 5 ? 0.6 : claimCount > 2 ? 0.4 : 0.2;
        double timeRisk = (hourOfDay != null && (hourOfDay >= 22 || hourOfDay <= 6)) ? 0.3 : 0.1;
        double dRisk = (dayOfWeek != null && (dayOfWeek == 0 || dayOfWeek == 6)) ? 0.2 : 0.1;
        return Math.min(baseRisk + timeRisk + dRisk, 1.0);
    }
    
    public String generatePrediction() {
        double r = calculateRiskScore();
        if (r > 0.8) return "HIGH_RISK";
        if (r > 0.6) return "MEDIUM_RISK";
        if (r > 0.4) return "LOW_RISK";
        return "VERY_LOW_RISK";
    }

    // Static conversion methods for services
    public static PredictiveAnalytics from(String type, Long count, Double hours, BigDecimal payout, Integer day, Integer hour) {
        PredictiveAnalytics pa = new PredictiveAnalytics(type, count, hours, payout, day, hour, 0.0, null, LocalDateTime.now());
        double s = pa.calculateRiskScore();
        return new PredictiveAnalytics(type, count, hours, payout, day, hour, s, pa.generatePrediction(), LocalDateTime.now());
    }
}

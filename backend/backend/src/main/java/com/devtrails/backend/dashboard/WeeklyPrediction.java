package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeeklyPrediction(
    String weekStartDate,
    String weekEndDate,
    Integer totalPredictedClaims,
    BigDecimal predictedPayoutAmount,
    String primaryRiskFactor,
    String secondaryRiskFactor,
    Double confidenceScore,
    LocalDateTime generatedAt
) {
    public String getConfidenceLevel() {
        if (confidenceScore == null) return "LOW";
        if (confidenceScore > 0.8) return "HIGH";
        if (confidenceScore > 0.6) return "MEDIUM";
        if (confidenceScore > 0.4) return "LOW";
        return "VERY_LOW";
    }
}

package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPrediction {
    private String weekStartDate;
    private String weekEndDate;
    private Integer totalPredictedClaims;
    private BigDecimal predictedPayoutAmount;
    private String primaryRiskFactor;
    private String secondaryRiskFactor;
    private Double confidenceScore;
    private LocalDateTime generatedAt;
    
    // Prediction confidence
    public String getConfidenceLevel() {
        if (confidenceScore == null) return "LOW";
        if (confidenceScore > 0.8) return "HIGH";
        if (confidenceScore > 0.6) return "MEDIUM";
        if (confidenceScore > 0.4) return "LOW";
        return "VERY_LOW";
    }
}

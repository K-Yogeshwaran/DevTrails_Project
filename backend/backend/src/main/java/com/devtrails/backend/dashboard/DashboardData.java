package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Worker Dashboard Data Classes
@Data
@NoArgsConstructor
@AllArgsConstructor
class WorkerDashboardData {
    private String workerId;
    private String workerName;
    private String zoneId;
    private String persona;
    private BigDecimal totalEarningsProtected;
    private Long totalClaims;
    private Long approvedClaims;
    private Long rejectedClaims;
    private BigDecimal totalPayoutsReceived;
    private BigDecimal dailyEarnings;
    private Integer activeHours;
    private Integer experienceMonths;
    private Integer daysPerWeek;
    private LocalDateTime lastUpdated;
    
    // Calculated fields
    public BigDecimal getCoveragePercentage() {
        if (totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(approvedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getClaimSuccessRate() {
        if (totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(approvedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyCoverageData {
    private String workerId;
    private String workerName;
    private LocalDateTime weekStart;
    private Integer weeklyClaims;
    private BigDecimal totalDisruptedHours;
    private BigDecimal weeklyPayouts;
    private Integer weeklyActiveHours;
    private LocalDateTime lastUpdated;
    
    // Calculated fields
    public BigDecimal getCoverageEfficiency() {
        if (weeklyActiveHours == 0) return BigDecimal.ZERO;
        return totalDisruptedHours
                .divide(BigDecimal.valueOf(weeklyActiveHours), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getWeeklyProtectionValue() {
        return weeklyPayouts;
    }
}

// Insurer Dashboard Data Classes
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsurerDashboardData {
    private Long totalClaims;
    private BigDecimal totalClaimValue;
    private BigDecimal totalPayoutsProcessed;
    private Long approvedClaims;
    private Long rejectedClaims;
    private Long flaggedClaims;
    private Integer activeWorkers;
    private Integer activeZones;
    private BigDecimal avgDailyEarnings;
    private LocalDateTime lastUpdated;
    
    // Calculated fields
    public BigDecimal getLossRatio() {
        if (totalClaimValue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return totalPayoutsProcessed
                .divide(totalClaimValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getApprovalRate() {
        if (totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(approvedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getRejectionRate() {
        if (totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(rejectedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getFlaggedRate() {
        if (totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(flaggedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}

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
        double baseRisk = claimCount > 10 ? 0.8 : claimCount > 5 ? 0.6 : claimCount > 2 ? 0.4 : 0.2;
        double timeRisk = (hourOfDay >= 22 || hourOfDay <= 6) ? 0.3 : 0.1;
        double dayRisk = (dayOfWeek == 0 || dayOfWeek == 6) ? 0.2 : 0.1;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LossRatioData {
    private String triggerType;
    private Long totalClaims;
    private BigDecimal totalPaid;
    private Long rejectedClaims;
    private Long flaggedClaims;
    private Double avgDisruptedHours;
    private LocalDateTime lastUpdated;
    
    // Calculated fields
    public BigDecimal getLossRatio() {
        if (totalClaims == 0) return BigDecimal.ZERO;
        BigDecimal totalValue = totalPaid.add(
                BigDecimal.valueOf(rejectedClaims * 100).add(BigDecimal.valueOf(flaggedClaims * 50))
        );
        return totalPaid.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getAverageClaimValue() {
        if (totalClaims == 0) return BigDecimal.ZERO;
        return totalPaid.divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneAnalyticsData {
    private String zoneId;
    private Integer activeWorkers;
    private Long totalClaims;
    private BigDecimal totalPayouts;
    private BigDecimal avgDailyEarnings;
    private LocalDateTime lastUpdated;
    
    // Calculated fields
    public BigDecimal getAverageClaimsPerWorker() {
        if (activeWorkers == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(totalClaims)
                .divide(BigDecimal.valueOf(activeWorkers), 2, java.math.RoundingMode.HALF_UP);
    }
    
    public BigDecimal getZoneRiskScore() {
        double claimDensity = totalClaims.doubleValue() / activeWorkers;
        if (claimDensity > 5) return BigDecimal.valueOf(0.8);
        if (claimDensity > 3) return BigDecimal.valueOf(0.6);
        if (claimDensity > 1) return BigDecimal.valueOf(0.4);
        return BigDecimal.valueOf(0.2);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDisruptionCorrelation {
    private String triggerType;
    private Integer dayOfWeek;
    private Integer hourOfDay;
    private Long claimCount;
    private Double avgDisruptedHours;
    private BigDecimal totalPayout;
    private LocalDateTime lastUpdated;
    
    // Pattern analysis
    public String getTimePattern() {
        if (hourOfDay >= 6 && hourOfDay <= 10) return "MORNING_PEAK";
        if (hourOfDay >= 11 && hourOfDay <= 15) return "AFTERNOON_PEAK";
        if (hourOfDay >= 16 && hourOfDay <= 20) return "EVENING_PEAK";
        return "NIGHT_LOW";
    }
    
    public String getDayPattern() {
        if (dayOfWeek == 1 || dayOfWeek == 2) return "WEEKDAY_START";
        if (dayOfWeek >= 3 && dayOfWeek <= 4) return "WEEKDAY_MID";
        if (dayOfWeek == 5) return "WEEKDAY_END";
        return "WEEKEND";
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentData {
    private String workerId;
    private String workerName;
    private String zoneId;
    private String persona;
    private Integer experienceMonths;
    private BigDecimal dailyEarnings;
    private Long totalClaims;
    private BigDecimal totalPaid;
    private Long rejectedClaims;
    private Long flaggedClaims;
    private Double avgDisruptedHours;
    private Double rejectionRate;
    private LocalDateTime lastUpdated;
    
    // Risk scoring
    public BigDecimal calculateOverallRiskScore() {
        double claimRisk = totalClaims > 10 ? 0.4 : totalClaims > 5 ? 0.3 : totalClaims > 2 ? 0.2 : 0.1;
        double rejectionRisk = rejectionRate > 30 ? 0.3 : rejectionRate > 15 ? 0.2 : rejectionRate > 5 ? 0.1 : 0.0;
        double experienceRisk = experienceMonths < 3 ? 0.3 : experienceMonths < 6 ? 0.2 : experienceMonths < 12 ? 0.1 : 0.0;
        double earningsRisk = dailyEarnings.compareTo(BigDecimal.valueOf(300)) < 0 ? 0.2 : 0.0;
        
        double totalRisk = claimRisk + rejectionRisk + experienceRisk + earningsRisk;
        return BigDecimal.valueOf(Math.min(totalRisk, 1.0));
    }
    
    public String getRiskCategory() {
        BigDecimal riskScore = calculateOverallRiskScore();
        if (riskScore.compareTo(BigDecimal.valueOf(0.8)) >= 0) return "HIGH_RISK";
        if (riskScore.compareTo(BigDecimal.valueOf(0.6)) >= 0) return "MEDIUM_RISK";
        if (riskScore.compareTo(BigDecimal.valueOf(0.4)) >= 0) return "LOW_RISK";
        return "VERY_LOW_RISK";
    }
    
    public String getRiskFactors() {
        StringBuilder factors = new StringBuilder();
        if (totalClaims > 10) factors.append("High claim frequency; ");
        if (rejectionRate > 30) factors.append("High rejection rate; ");
        if (experienceMonths < 3) factors.append("Low experience; ");
        if (dailyEarnings.compareTo(BigDecimal.valueOf(300)) < 0) factors.append("Low earnings; ");
        return factors.length() > 0 ? factors.toString() : "No significant risk factors";
    }
}

// Real-time update classes
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
        if (confidenceScore > 0.8) return "HIGH";
        if (confidenceScore > 0.6) return "MEDIUM";
        if (confidenceScore > 0.4) return "LOW";
        return "VERY_LOW";
    }
}

package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskAssessmentData(
    String workerId,
    String workerName,
    String zoneId,
    String persona,
    Integer experienceMonths,
    BigDecimal dailyEarnings,
    Long totalClaims,
    BigDecimal totalPaid,
    Long rejectedClaims,
    Long flaggedClaims,
    Double avgDisruptedHours,
    Double rejectionRate,
    LocalDateTime lastUpdated
) {
    // Constructor for JPQL query
    public RiskAssessmentData(String workerId, String workerName, String zoneId, String persona,
                             Object experienceMonths, Object dailyEarnings, Object totalClaims,
                             Object totalPaid, Object rejectedClaims, Object flaggedClaims,
                             Object avgDisruptedHours, Object rejectionRate) {
        this(
            workerId,
            workerName,
            zoneId,
            persona,
            experienceMonths != null ? ((Number) experienceMonths).intValue() : 0,
            dailyEarnings != null ? new BigDecimal(dailyEarnings.toString()) : BigDecimal.ZERO,
            totalClaims != null ? ((Number) totalClaims).longValue() : 0L,
            totalPaid != null ? new BigDecimal(totalPaid.toString()) : BigDecimal.ZERO,
            rejectedClaims != null ? ((Number) rejectedClaims).longValue() : 0L,
            flaggedClaims != null ? ((Number) flaggedClaims).longValue() : 0L,
            avgDisruptedHours != null ? ((Number) avgDisruptedHours).doubleValue() : 0.0,
            rejectionRate != null ? ((Number) rejectionRate).doubleValue() : 0.0,
            LocalDateTime.now()
        );
    }

    public BigDecimal calculateOverallRiskScore() {
        if (totalClaims == null) return BigDecimal.valueOf(0.1);
        double claimRisk = totalClaims > 10 ? 0.4 : totalClaims > 5 ? 0.3 : totalClaims > 2 ? 0.2 : 0.1;
        double rRisk = (rejectionRate != null && rejectionRate > 30) ? 0.3 : (rejectionRate != null && rejectionRate > 15) ? 0.2 : (rejectionRate != null && rejectionRate > 5) ? 0.1 : 0.0;
        double eRisk = (experienceMonths != null && experienceMonths < 3) ? 0.3 : (experienceMonths != null && experienceMonths < 6) ? 0.2 : (experienceMonths != null && experienceMonths < 12) ? 0.1 : 0.0;
        double dRisk = (dailyEarnings != null && dailyEarnings.compareTo(BigDecimal.valueOf(300)) < 0) ? 0.2 : 0.0;
        
        double totalRisk = claimRisk + rRisk + eRisk + dRisk;
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
        if (totalClaims != null && totalClaims > 10) factors.append("High claim frequency; ");
        if (rejectionRate != null && rejectionRate > 30) factors.append("High rejection rate; ");
        if (experienceMonths != null && experienceMonths < 3) factors.append("Low experience; ");
        if (dailyEarnings != null && dailyEarnings.compareTo(BigDecimal.valueOf(300)) < 0) factors.append("Low earnings; ");
        return factors.length() > 0 ? factors.toString() : "No significant risk factors";
    }
}

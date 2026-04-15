package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        if (totalClaims == null) return BigDecimal.valueOf(0.1);
        double claimRisk = totalClaims > 10 ? 0.4 : totalClaims > 5 ? 0.3 : totalClaims > 2 ? 0.2 : 0.1;
        double rejectionRisk = (rejectionRate != null && rejectionRate > 30) ? 0.3 : (rejectionRate != null && rejectionRate > 15) ? 0.2 : (rejectionRate != null && rejectionRate > 5) ? 0.1 : 0.0;
        double experienceRisk = (experienceMonths != null && experienceMonths < 3) ? 0.3 : (experienceMonths != null && experienceMonths < 6) ? 0.2 : (experienceMonths != null && experienceMonths < 12) ? 0.1 : 0.0;
        double earningsRisk = (dailyEarnings != null && dailyEarnings.compareTo(BigDecimal.valueOf(300)) < 0) ? 0.2 : 0.0;
        
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
        if (totalClaims != null && totalClaims > 10) factors.append("High claim frequency; ");
        if (rejectionRate != null && rejectionRate > 30) factors.append("High rejection rate; ");
        if (experienceMonths != null && experienceMonths < 3) factors.append("Low experience; ");
        if (dailyEarnings != null && dailyEarnings.compareTo(BigDecimal.valueOf(300)) < 0) factors.append("Low earnings; ");
        return factors.length() > 0 ? factors.toString() : "No significant risk factors";
    }
}

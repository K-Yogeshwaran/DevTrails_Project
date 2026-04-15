package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerDashboardData {
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
        if (totalClaims == null || totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(approvedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getClaimSuccessRate() {
        if (totalClaims == null || totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(approvedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}

package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        if (totalClaimValue == null || totalClaimValue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return totalPayoutsProcessed
                .divide(totalClaimValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getApprovalRate() {
        if (totalClaims == null || totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(approvedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getRejectionRate() {
        if (totalClaims == null || totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(rejectedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getFlaggedRate() {
        if (totalClaims == null || totalClaims == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(flaggedClaims)
                .divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}

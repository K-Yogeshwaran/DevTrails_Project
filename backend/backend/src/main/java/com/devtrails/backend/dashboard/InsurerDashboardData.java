package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InsurerDashboardData(
    Long totalClaims,
    BigDecimal totalClaimValue,
    BigDecimal totalPayoutsProcessed,
    Long approvedClaims,
    Long rejectedClaims,
    Long flaggedClaims,
    Integer activeWorkers,
    Integer activeZones,
    BigDecimal avgDailyEarnings,
    LocalDateTime lastUpdated
) {
    // Constructor for JPQL query
    public InsurerDashboardData(Object totalClaims, Object totalClaimValue, Object totalPayoutsProcessed,
                              Object approvedClaims, Object rejectedClaims, Object flaggedClaims,
                              Object activeWorkers, Object activeZones, Object avgDailyEarnings) {
        this(
            totalClaims != null ? ((Number) totalClaims).longValue() : 0L,
            totalClaimValue != null ? new BigDecimal(totalClaimValue.toString()) : BigDecimal.ZERO,
            totalPayoutsProcessed != null ? new BigDecimal(totalPayoutsProcessed.toString()) : BigDecimal.ZERO,
            approvedClaims != null ? ((Number) approvedClaims).longValue() : 0L,
            rejectedClaims != null ? ((Number) rejectedClaims).longValue() : 0L,
            flaggedClaims != null ? ((Number) flaggedClaims).longValue() : 0L,
            activeWorkers != null ? ((Number) activeWorkers).intValue() : 0,
            activeZones != null ? ((Number) activeZones).intValue() : 0,
            avgDailyEarnings != null ? new BigDecimal(avgDailyEarnings.toString()) : BigDecimal.ZERO,
            LocalDateTime.now()
        );
    }

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

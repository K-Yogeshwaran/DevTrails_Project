package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ZoneAnalyticsData(
    String zoneId,
    Integer activeWorkers,
    Long totalClaims,
    BigDecimal totalPayouts,
    BigDecimal avgDailyEarnings,
    LocalDateTime lastUpdated
) {
    // Constructor for JPQL query
    public ZoneAnalyticsData(String zoneId, Object activeWorkers, Object totalClaims,
                            Object totalPayouts, Object avgDailyEarnings) {
        this(
            zoneId,
            activeWorkers != null ? ((Number) activeWorkers).intValue() : 0,
            totalClaims != null ? ((Number) totalClaims).longValue() : 0L,
            totalPayouts != null ? new BigDecimal(totalPayouts.toString()) : BigDecimal.ZERO,
            avgDailyEarnings != null ? new BigDecimal(avgDailyEarnings.toString()) : BigDecimal.ZERO,
            LocalDateTime.now()
        );
    }

    public BigDecimal getAverageClaimsPerWorker() {
        if (activeWorkers == null || activeWorkers == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(totalClaims)
                .divide(BigDecimal.valueOf(activeWorkers), 2, java.math.RoundingMode.HALF_UP);
    }
    
    public BigDecimal getZoneRiskScore() {
        if (activeWorkers == null || activeWorkers == 0) return BigDecimal.valueOf(0.2);
        double claimDensity = totalClaims.doubleValue() / activeWorkers;
        if (claimDensity > 5) return BigDecimal.valueOf(0.8);
        if (claimDensity > 3) return BigDecimal.valueOf(0.6);
        if (claimDensity > 1) return BigDecimal.valueOf(0.4);
        return BigDecimal.valueOf(0.2);
    }
}

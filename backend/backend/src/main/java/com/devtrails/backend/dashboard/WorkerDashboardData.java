package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkerDashboardData(
    String workerId,
    String workerName,
    String zoneId,
    String persona,
    BigDecimal totalEarningsProtected,
    Long totalClaims,
    Long approvedClaims,
    Long rejectedClaims,
    BigDecimal totalPayoutsReceived,
    BigDecimal dailyEarnings,
    Integer activeHours,
    Integer experienceMonths,
    Integer daysPerWeek,
    LocalDateTime lastUpdated
) {
    // Constructor for JPQL query (handles Object/Number conversion)
    public WorkerDashboardData(String workerId, String workerName, String zoneId, String persona,
                             Object totalEarningsProtected, Object totalClaims, Object approvedClaims,
                             Object rejectedClaims, Object totalPayoutsReceived, Object dailyEarnings,
                             Object activeHours, Object experienceMonths, Object daysPerWeek) {
        this(
            workerId,
            workerName,
            zoneId,
            persona,
            totalEarningsProtected != null ? new BigDecimal(totalEarningsProtected.toString()) : BigDecimal.ZERO,
            totalClaims != null ? ((Number) totalClaims).longValue() : 0L,
            approvedClaims != null ? ((Number) approvedClaims).longValue() : 0L,
            rejectedClaims != null ? ((Number) rejectedClaims).longValue() : 0L,
            totalPayoutsReceived != null ? new BigDecimal(totalPayoutsReceived.toString()) : BigDecimal.ZERO,
            dailyEarnings != null ? new BigDecimal(dailyEarnings.toString()) : BigDecimal.ZERO,
            activeHours != null ? ((Number) activeHours).intValue() : 0,
            experienceMonths != null ? ((Number) experienceMonths).intValue() : 0,
            daysPerWeek != null ? ((Number) daysPerWeek).intValue() : 0,
            LocalDateTime.now()
        );
    }

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

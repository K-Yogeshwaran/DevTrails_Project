package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeeklyCoverageData(
    String workerId,
    String workerName,
    LocalDateTime weekStart,
    Integer weeklyClaims,
    BigDecimal totalDisruptedHours,
    BigDecimal weeklyPayouts,
    Integer weeklyActiveHours,
    LocalDateTime lastUpdated
) {
    // Constructor for JPQL query
    public WeeklyCoverageData(String workerId, String workerName, Object weekStart, Object weeklyClaims,
                             Object totalDisruptedHours, Object weeklyPayouts, Object weeklyActiveHours) {
        this(
            workerId,
            workerName,
            weekStart != null ? (LocalDateTime) weekStart : LocalDateTime.now(),
            weeklyClaims != null ? ((Number) weeklyClaims).intValue() : 0,
            totalDisruptedHours != null ? new BigDecimal(totalDisruptedHours.toString()) : BigDecimal.ZERO,
            weeklyPayouts != null ? new BigDecimal(weeklyPayouts.toString()) : BigDecimal.ZERO,
            weeklyActiveHours != null ? ((Number) weeklyActiveHours).intValue() : 0,
            LocalDateTime.now()
        );
    }

    public BigDecimal getCoverageEfficiency() {
        if (weeklyActiveHours == null || weeklyActiveHours == 0) return BigDecimal.ZERO;
        return (totalDisruptedHours != null ? totalDisruptedHours : BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(weeklyActiveHours), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}

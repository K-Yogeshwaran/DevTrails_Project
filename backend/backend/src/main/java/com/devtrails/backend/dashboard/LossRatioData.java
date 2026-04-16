package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LossRatioData(
    String triggerType,
    Long totalClaims,
    BigDecimal totalPaid,
    Long rejectedClaims,
    Long flaggedClaims,
    Double avgDisruptedHours,
    LocalDateTime lastUpdated
) {
    // Constructor for JPQL query
    public LossRatioData(String triggerType, Object totalClaims, Object totalPaid,
                        Object rejectedClaims, Object flaggedClaims, Object avgDisruptedHours) {
        this(
            triggerType,
            totalClaims != null ? ((Number) totalClaims).longValue() : 0L,
            totalPaid != null ? new BigDecimal(totalPaid.toString()) : BigDecimal.ZERO,
            rejectedClaims != null ? ((Number) rejectedClaims).longValue() : 0L,
            flaggedClaims != null ? ((Number) flaggedClaims).longValue() : 0L,
            avgDisruptedHours != null ? ((Number) avgDisruptedHours).doubleValue() : 0.0,
            LocalDateTime.now()
        );
    }

    public BigDecimal getLossRatio() {
        if (totalClaims == null || totalClaims == 0) return BigDecimal.ZERO;
        BigDecimal totalValue = totalPaid.add(
                BigDecimal.valueOf(rejectedClaims * 100).add(BigDecimal.valueOf(flaggedClaims * 50))
        );
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return totalPaid.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getAverageClaimValue() {
        if (totalClaims == null || totalClaims == 0) return BigDecimal.ZERO;
        return totalPaid.divide(BigDecimal.valueOf(totalClaims), 2, java.math.RoundingMode.HALF_UP);
    }
}

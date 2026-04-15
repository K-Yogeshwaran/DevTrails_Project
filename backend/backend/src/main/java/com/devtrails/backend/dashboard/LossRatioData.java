package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

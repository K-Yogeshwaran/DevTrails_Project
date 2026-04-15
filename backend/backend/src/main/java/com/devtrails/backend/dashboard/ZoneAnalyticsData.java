package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneAnalyticsData {
    private String zoneId;
    private Integer activeWorkers;
    private Long totalClaims;
    private BigDecimal totalPayouts;
    private BigDecimal avgDailyEarnings;
    private LocalDateTime lastUpdated;
    
    // Calculated fields
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

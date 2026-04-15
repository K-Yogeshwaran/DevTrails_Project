package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyCoverageData {
    private String workerId;
    private String workerName;
    private LocalDateTime weekStart;
    private Integer weeklyClaims;
    private BigDecimal totalDisruptedHours;
    private BigDecimal weeklyPayouts;
    private Integer weeklyActiveHours;
    private LocalDateTime lastUpdated;
    
    // Calculated fields
    public BigDecimal getCoverageEfficiency() {
        if (weeklyActiveHours == null || weeklyActiveHours == 0) return BigDecimal.ZERO;
        return totalDisruptedHours
                .divide(BigDecimal.valueOf(weeklyActiveHours), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    public BigDecimal getWeeklyProtectionValue() {
        return weeklyPayouts;
    }
}

package com.devtrails.backend.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDisruptionCorrelation {
    private String triggerType;
    private Integer dayOfWeek;
    private Integer hourOfDay;
    private Long claimCount;
    private Double avgDisruptedHours;
    private BigDecimal totalPayout;
    private LocalDateTime lastUpdated;
    
    // Pattern analysis
    public String getTimePattern() {
        if (hourOfDay == null) return "NIGHT_LOW";
        if (hourOfDay >= 6 && hourOfDay <= 10) return "MORNING_PEAK";
        if (hourOfDay >= 11 && hourOfDay <= 15) return "AFTERNOON_PEAK";
        if (hourOfDay >= 16 && hourOfDay <= 20) return "EVENING_PEAK";
        return "NIGHT_LOW";
    }
    
    public String getDayPattern() {
        if (dayOfWeek == null) return "WEEKEND";
        if (dayOfWeek == 1 || dayOfWeek == 2) return "WEEKDAY_START";
        if (dayOfWeek >= 3 && dayOfWeek <= 4) return "WEEKDAY_MID";
        if (dayOfWeek == 5) return "WEEKDAY_END";
        return "WEEKEND";
    }
}

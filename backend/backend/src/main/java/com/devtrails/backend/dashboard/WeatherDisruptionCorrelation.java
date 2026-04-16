package com.devtrails.backend.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WeatherDisruptionCorrelation(
    String triggerType,
    Integer dayOfWeek,
    Integer hourOfDay,
    Long claimCount,
    Double avgDisruptedHours,
    BigDecimal totalPayout,
    LocalDateTime lastUpdated
) {
    // Constructor for JPQL query
    public WeatherDisruptionCorrelation(String triggerType, Object dayOfWeek, Object hourOfDay,
                                       Object claimCount, Object avgDisruptedHours, Object totalPayouts) {
        this(
            triggerType,
            dayOfWeek != null ? ((Number) dayOfWeek).intValue() : 0,
            hourOfDay != null ? ((Number) hourOfDay).intValue() : 0,
            claimCount != null ? ((Number) claimCount).longValue() : 0L,
            avgDisruptedHours != null ? ((Number) avgDisruptedHours).doubleValue() : 0.0,
            totalPayouts != null ? new BigDecimal(totalPayouts.toString()) : BigDecimal.ZERO,
            LocalDateTime.now()
        );
    }

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

package com.devtrails.backend.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDTO {

    public record FlaggedClaim(
        String        claimId,
        String        workerId,
        String        workerName,
        String        workerPhone,
        String        triggerType,
        String        zoneId,
        BigDecimal    payoutAmount,
        BigDecimal    fraudScore,
        LocalDateTime triggeredAt,
        LocalDateTime processedAt
    ) {}

    public record LossRatioInfo(
        BigDecimal totalPremiums,
        BigDecimal totalClaimsPaid,
        double lossRatio,
        List<MonthlyStats> history
    ) {}

    public record MonthlyStats(
        String month,
        BigDecimal premiums,
        BigDecimal claims
    ) {}

    public record PredictiveAnalytics(
        List<ZoneRisk> zoneRisks,
        BigDecimal estimatedNextWeekPayout
    ) {}

    public record ZoneRisk(
        String zoneId,
        String zoneName,
        String primaryRisk, // e.g., "Heavy Rain", "Heatwave"
        double riskProbability, // 0.0 to 1.0
        int affectedWorkers
    ) {}
}

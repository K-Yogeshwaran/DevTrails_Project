package com.devtrails.backend.dashboard;

import com.devtrails.backend.worker.Worker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardAnalyticsRepository analyticsRepository;
    private final DashboardWebSocketService webSocketService;

    // Worker Dashboard Methods
    public WorkerDashboardData getWorkerDashboard(String workerId) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return analyticsRepository.getWorkerDashboardData(workerId)
                .orElseThrow(() -> new RuntimeException("Worker data not found: " + workerId));
    }

    public List<WeeklyCoverageData> getWorkerWeeklyCoverage(String workerId) {
        LocalDateTime fourWeeksAgo = LocalDateTime.now().minusWeeks(4);
        
        return analyticsRepository.getWorkerWeeklyCoverage(workerId, fourWeeksAgo);
    }

    public BigDecimal calculateEarningsProtection(String workerId) {
        WorkerDashboardData data = getWorkerDashboard(workerId);
        
        // Calculate protection based on approved claims vs potential earnings
        BigDecimal potentialEarnings = data.getDailyEarnings()
                .multiply(BigDecimal.valueOf(data.getActiveHours()))
                .multiply(BigDecimal.valueOf(data.getDaysPerWeek() * 4)); // 4 weeks
        
        BigDecimal actualEarnings = potentialEarnings.add(data.getTotalPayoutsReceived());
        
        return actualEarnings.compareTo(potentialEarnings) >= 0 ? 
                actualEarnings : potentialEarnings;
    }

    // Insurer Dashboard Methods
    public InsurerDashboardData getInsurerDashboard() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return analyticsRepository.getInsurerDashboardData(oneMonthAgo)
                .orElseThrow(() -> new RuntimeException("Insurer data not available"));
    }

    public List<LossRatioData> getLossRatios() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return analyticsRepository.getLossRatioByTriggerType(oneMonthAgo);
    }

    public BigDecimal calculateOverallLossRatio() {
        List<LossRatioData> lossRatios = getLossRatios();
        
        BigDecimal totalPaid = lossRatios.stream()
                .map(LossRatioData::getTotalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalValue = lossRatios.stream()
                .map(lr -> lr.getTotalPaid().add(
                        BigDecimal.valueOf(lr.getRejectedClaims() * 100)
                                .add(BigDecimal.valueOf(lr.getFlaggedClaims() * 50))
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalValue.compareTo(BigDecimal.ZERO) > 0 ? 
                totalPaid.divide(totalValue, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    // Predictive Analytics Methods
    public List<PredictiveAnalytics> getPredictiveAnalytics() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Object[]> rawData = analyticsRepository.getPredictiveAnalytics(oneMonthAgo);
        
        return rawData.stream()
                .map(this::convertToPredictiveAnalytics)
                .collect(Collectors.toList());
    }

    public WeeklyPrediction generateWeeklyPrediction() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Object[]> analytics = analyticsRepository.getPredictiveAnalytics(oneMonthAgo);
        
        // Analyze patterns for prediction
        Map<String, Long> triggerTypeCounts = analytics.stream()
                .collect(Collectors.groupingBy(
                        arr -> arr[0].toString(),
                        Collectors.counting()
                ));
        
        // Calculate predicted claims for next week
        int totalPredictedClaims = calculatePredictedClaims(analytics);
        BigDecimal predictedPayoutAmount = calculatePredictedPayout(analytics);
        
        // Identify risk factors
        String primaryRiskFactor = identifyPrimaryRiskFactor(analytics);
        String secondaryRiskFactor = identifySecondaryRiskFactor(analytics);
        
        // Calculate confidence score
        double confidenceScore = calculatePredictionConfidence(analytics);
        
        LocalDateTime weekStart = LocalDateTime.now().plusWeeks(1).withDayOfWeek(1).withHour(0).withMinute(0);
        LocalDateTime weekEnd = weekStart.plusDays(6).withHour(23).withMinute(59);
        
        return new WeeklyPrediction(
                weekStart.toString(),
                weekEnd.toString(),
                totalPredictedClaims,
                predictedPayoutAmount,
                primaryRiskFactor,
                secondaryRiskFactor,
                confidenceScore,
                LocalDateTime.now()
        );
    }

    public List<ZoneAnalyticsData> getZoneAnalytics() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return analyticsRepository.getZoneAnalytics(oneMonthAgo);
    }

    public List<WeatherDisruptionCorrelation> getWeatherDisruptionCorrelation() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return analyticsRepository.getWeatherDisruptionCorrelation(oneMonthAgo);
    }

    public List<RiskAssessmentData> getWorkerRiskAssessment() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return analyticsRepository.getWorkerRiskAssessment(oneMonthAgo);
    }

    // Real-time Update Methods
    public void updateWorkerDashboard(String workerId) {
        WorkerDashboardData updatedData = getWorkerDashboard(workerId);
        DashboardUpdate update = DashboardUpdate.workerUpdate(workerId, updatedData);
        
        webSocketService.sendUpdate(update);
        log.info("Worker dashboard updated for: {}", workerId);
    }

    public void updateInsurerDashboard() {
        InsurerDashboardData updatedData = getInsurerDashboard();
        DashboardUpdate update = DashboardUpdate.insurerUpdate(updatedData);
        
        webSocketService.sendUpdate(update);
        log.info("Insurer dashboard updated");
    }

    // Advanced Analytics Methods
    public Map<String, Object> getAdvancedAnalytics() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        // Get all analytics data
        List<PredictiveAnalytics> predictiveData = getPredictiveAnalytics();
        List<ZoneAnalyticsData> zoneData = getZoneAnalytics();
        List<WeatherDisruptionCorrelation> weatherData = getWeatherDisruptionCorrelation();
        List<RiskAssessmentData> riskData = getWorkerRiskAssessment();
        
        // Calculate advanced metrics
        Map<String, Object> analytics = Map.of(
                "predictiveInsights", generatePredictiveInsights(predictiveData),
                "zonePerformance", generateZonePerformanceInsights(zoneData),
                "weatherCorrelations", generateWeatherInsights(weatherData),
                "riskAssessments", generateRiskInsights(riskData),
                "trendAnalysis", generateTrendAnalysis(),
                "recommendations", generateRecommendations()
        );
        
        return analytics;
    }

    // Helper Methods
    private PredictiveAnalytics convertToPredictiveAnalytics(Object[] arr) {
        PredictiveAnalytics analytics = new PredictiveAnalytics();
        analytics.setTriggerType(arr[0].toString());
        analytics.setClaimCount(((Number) arr[1]).longValue());
        analytics.setAvgDisruptedHours(((Number) arr[2]).doubleValue());
        analytics.setTotalPayout(new BigDecimal(arr[3].toString()));
        analytics.setDayOfWeek(((Number) arr[4]).intValue());
        analytics.setHourOfDay(((Number) arr[5]).intValue());
        
        // Calculate risk and prediction
        double riskScore = analytics.calculateRiskScore();
        analytics.setRiskScore(riskScore);
        analytics.setPrediction(analytics.generatePrediction());
        analytics.setLastUpdated(LocalDateTime.now());
        
        return analytics;
    }

    private int calculatePredictedClaims(List<Object[]> analytics) {
        // Simple prediction based on historical patterns
        double avgClaimsPerDay = analytics.stream()
                .mapToDouble(arr -> ((Number) arr[1]).doubleValue())
                .average()
                .orElse(0.0);
        
        // Factor in seasonality and trends
        double seasonalFactor = getSeasonalFactor();
        double trendFactor = getTrendFactor(analytics);
        
        return (int) Math.round(avgClaimsPerDay * 7 * seasonalFactor * trendFactor);
    }

    private BigDecimal calculatePredictedPayout(List<Object[]> analytics) {
        double avgPayoutPerClaim = analytics.stream()
                .mapToDouble(arr -> new BigDecimal(arr[3].toString()).doubleValue())
                .average()
                .orElse(0.0);
        
        int predictedClaims = calculatePredictedClaims(analytics);
        return BigDecimal.valueOf(avgPayoutPerClaim * predictedClaims);
    }

    private String identifyPrimaryRiskFactor(List<Object[]> analytics) {
        // Find the most common trigger type with highest claim count
        return analytics.stream()
                .collect(Collectors.groupingBy(
                        arr -> arr[0].toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    private String identifySecondaryRiskFactor(List<Object[]> analytics) {
        // Find time-based patterns
        Map<Integer, Long> hourDistribution = analytics.stream()
                .collect(Collectors.groupingBy(
                        arr -> ((Number) arr[5]).intValue(),
                        Collectors.counting()
                ));
        
        return hourDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> "HIGH_CLAIM_PERIOD_" + entry.getKey() + ":00")
                .orElse("NO_CLEAR_PATTERN");
    }

    private double calculatePredictionConfidence(List<Object[]> analytics) {
        if (analytics.isEmpty()) return 0.5;
        
        // Calculate confidence based on data consistency
        double variance = analytics.stream()
                .mapToDouble(arr -> ((Number) arr[1]).doubleValue())
                .map(count -> Math.pow(count - analytics.stream()
                        .mapToDouble(arr2 -> ((Number) arr2[1]).doubleValue())
                        .average().orElse(0.0), 2))
                .average()
                .orElse(Double.MAX_VALUE);
        
        // Lower variance = higher confidence
        double normalizedVariance = Math.min(variance / 100, 1.0);
        return Math.max(0.3, 1.0 - normalizedVariance);
    }

    private double getSeasonalFactor() {
        int month = LocalDateTime.now().getMonthValue();
        // Monsoon season (June-September) has higher claim rates
        if (month >= 6 && month <= 9) return 1.3;
        // Winter months (November-February) have moderate rates
        if (month >= 11 || month <= 2) return 1.1;
        // Summer months have lower rates
        return 0.8;
    }

    private double getTrendFactor(List<Object[]> analytics) {
        if (analytics.size() < 4) return 1.0;
        
        // Calculate trend from recent data
        double recentAvg = analytics.stream()
                .limit(7)
                .mapToDouble(arr -> ((Number) arr[1]).doubleValue())
                .average()
                .orElse(0.0);
        
        double olderAvg = analytics.stream()
                .skip(7)
                .limit(7)
                .mapToDouble(arr -> ((Number) arr[1]).doubleValue())
                .average()
                .orElse(0.0);
        
        if (olderAvg == 0) return 1.0;
        return Math.max(0.5, Math.min(1.5, recentAvg / olderAvg));
    }

    private Map<String, Object> generatePredictiveInsights(List<PredictiveAnalytics> data) {
        return Map.of(
                "highRiskPeriods", data.stream()
                        .filter(p -> p.getRiskScore() > 0.7)
                        .map(p -> p.getHourOfDay() + ":00 - " + (p.getHourOfDay() + 1) + ":00")
                        .collect(Collectors.toList()),
                "mostCommonTriggers", data.stream()
                        .collect(Collectors.groupingBy(
                                PredictiveAnalytics::getTriggerType,
                                Collectors.counting()
                        ))
                        .entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(5)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()),
                "averageClaimValues", data.stream()
                        .mapToDouble(PredictiveAnalytics::getTotalPayout)
                        .average()
                        .orElse(0.0)
        );
    }

    private Map<String, Object> generateZonePerformanceInsights(List<ZoneAnalyticsData> data) {
        return Map.of(
                "topPerformingZones", data.stream()
                        .sorted((z1, z2) -> z2.getTotalPayouts().compareTo(z1.getTotalPayouts()))
                        .limit(5)
                        .map(ZoneAnalyticsData::getZoneId)
                        .collect(Collectors.toList()),
                "highRiskZones", data.stream()
                        .filter(z -> z.getZoneRiskScore().compareTo(BigDecimal.valueOf(0.6)) >= 0)
                        .map(ZoneAnalyticsData::getZoneId)
                        .collect(Collectors.toList()),
                "averageClaimsPerWorker", data.stream()
                        .mapToDouble(z -> z.getAverageClaimsPerWorker().doubleValue())
                        .average()
                        .orElse(0.0)
        );
    }

    private Map<String, Object> generateWeatherInsights(List<WeatherDisruptionCorrelation> data) {
        return Map.of(
                "peakClaimTimes", data.stream()
                        .collect(Collectors.groupingBy(
                                WeatherDisruptionCorrelation::getTimePattern,
                                Collectors.counting()
                        )),
                "weatherTriggerPatterns", data.stream()
                        .collect(Collectors.groupingBy(
                                WeatherDisruptionCorrelation::getTriggerType,
                                Collectors.counting()
                        )),
                "dayOfWeekPatterns", data.stream()
                        .collect(Collectors.groupingBy(
                                WeatherDisruptionCorrelation::getDayPattern,
                                Collectors.counting()
                        ))
        );
    }

    private Map<String, Object> generateRiskInsights(List<RiskAssessmentData> data) {
        return Map.of(
                "highRiskWorkers", data.stream()
                        .filter(w -> "HIGH_RISK".equals(w.getRiskCategory()))
                        .map(RiskAssessmentData::getWorkerId)
                        .collect(Collectors.toList()),
                "riskDistribution", data.stream()
                        .collect(Collectors.groupingBy(
                                RiskAssessmentData::getRiskCategory,
                                Collectors.counting()
                        )),
                "commonRiskFactors", data.stream()
                        .flatMap(w -> java.util.Arrays.stream(w.getRiskFactors().split("; ")))
                        .collect(Collectors.groupingBy(
                                factor -> factor.trim(),
                                Collectors.counting()
                        ))
        );
    }

    private Map<String, Object> generateTrendAnalysis() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        // This would typically involve more complex queries
        // For now, return basic trend data
        return Map.of(
                "claimTrend", "INCREASING", // Simplified
                "payoutTrend", "STABLE",
                "riskTrend", "DECREASING",
                "period", "3_MONTHS"
        );
    }

    private Map<String, Object> generateRecommendations() {
        return Map.of(
                "forWorkers", List.of(
                        "Consider increasing coverage during monsoon season",
                        "Maintain consistent delivery patterns",
                        "Update payment information for faster payouts"
                ),
                "forInsurers", List.of(
                        "Monitor high-risk zones during peak hours",
                        "Implement dynamic pricing based on weather patterns",
                        "Consider automated fraud detection for high-frequency claimers"
                )
        );
    }
}

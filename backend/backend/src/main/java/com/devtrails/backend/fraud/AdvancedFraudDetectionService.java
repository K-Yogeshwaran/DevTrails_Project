package com.devtrails.backend.fraud;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.worker.Worker;
import com.devtrails.backend.worker.WorkerRepository;

@Service
public class AdvancedFraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AdvancedFraudDetectionService.class);

    private final GPSValidationService gpsValidationService;
    private final WeatherValidationService weatherValidationService;
    private final DeliveryRouteAnalysisService routeAnalysisService;
    private final FraudDetectionLogRepository fraudLogRepository;
    private final WorkerRepository workerRepository;

    public AdvancedFraudDetectionService(GPSValidationService gpsValidationService,
                                         WeatherValidationService weatherValidationService,
                                         DeliveryRouteAnalysisService routeAnalysisService,
                                         FraudDetectionLogRepository fraudLogRepository,
                                         WorkerRepository workerRepository) {
        this.gpsValidationService = gpsValidationService;
        this.weatherValidationService = weatherValidationService;
        this.routeAnalysisService = routeAnalysisService;
        this.fraudLogRepository = fraudLogRepository;
        this.workerRepository = workerRepository;
    }

    // Advanced fraud detection thresholds
    private static final double ADVANCED_FRAUD_THRESHOLD = 0.65;
    private static final double CRITICAL_FRAUD_THRESHOLD = 0.85;

    public AdvancedFraudResult performAdvancedFraudDetection(Claim claim, List<Claim> recentClaims) {
        AdvancedFraudResult result = new AdvancedFraudResult();
        result.setClaimId(claim.getClaimId());
        result.setWorkerId(claim.getWorkerId());
        result.setOverallRiskScore(0.0);
        result.setRecommendation("APPROVE");

        try {
            // Get worker details
            Worker worker = workerRepository.findByWorkerId(claim.getWorkerId()).orElse(null);
            if (worker == null) {
                result.setRecommendation("REJECT");
                result.addRiskFactor("Worker not found", 1.0);
                return result;
            }

            // 1. GPS Validation
            GPSValidationService.GPSValidationResult gpsResult = 
                gpsValidationService.validateGPS(claim, worker, recentClaims);
            if (!gpsResult.isValid()) {
                result.addRiskFactor("GPS validation failed: " + gpsResult.getDetails(), 
                                   gpsResult.getRiskScore());
                logFraudDetection(claim, "GPS_SPOOFING", gpsResult.getRiskScore(), 
                                gpsResult.getDetails(), "FLAGGED");
            }

            // 2. Weather Validation (if GPS coordinates are available)
            if (claim.getGpsLatitude() != null && claim.getGpsLongitude() != null) {
                WeatherValidationService.WeatherValidationResult weatherResult = 
                    weatherValidationService.validateWeather(claim);
                if (!weatherResult.isValid()) {
                    result.addRiskFactor("Weather anomaly: " + weatherResult.getDetails(), 
                                       weatherResult.getRiskScore());
                    logFraudDetection(claim, "WEATHER_ANOMALY", weatherResult.getRiskScore(), 
                                    weatherResult.getDetails(), "FLAGGED");
                }
            }

            // 3. Delivery Pattern Analysis
            double patternRiskScore = analyzeDeliveryPatterns(claim, worker, recentClaims);
            if (patternRiskScore > 0.3) {
                result.addRiskFactor("Suspicious delivery pattern", patternRiskScore);
                logFraudDetection(claim, "PATTERN_ANALYSIS", patternRiskScore, 
                                "Unusual delivery pattern detected", "FLAGGED");
            }

            // 4. Temporal Analysis
            double temporalRiskScore = analyzeTemporalPatterns(claim, recentClaims);
            if (temporalRiskScore > 0.4) {
                result.addRiskFactor("Suspicious timing pattern", temporalRiskScore);
                logFraudDetection(claim, "TEMPORAL_ANALYSIS", temporalRiskScore, 
                                "Unusual claim timing", "FLAGGED");
            }

            // 5. Delivery Route Analysis
            DeliveryRouteAnalysisService.RouteAnalysisResult routeResult = 
                routeAnalysisService.analyzeDeliveryRoute(claim, worker, recentClaims);
            if (!routeResult.isValid()) {
                result.addRiskFactor("Route analysis failed: " + routeResult.getDetails(), 
                                   routeResult.getOverallRiskScore());
                logFraudDetection(claim, "ROUTE_ANALYSIS", routeResult.getOverallRiskScore(), 
                                routeResult.getDetails(), "FLAGGED");
            }

            // 6. Cross-validation with historical data
            double historicalRiskScore = analyzeHistoricalConsistency(claim, worker);
            if (historicalRiskScore > 0.5) {
                result.addRiskFactor("Historical inconsistency", historicalRiskScore);
                logFraudDetection(claim, "HISTORICAL_ANALYSIS", historicalRiskScore, 
                                "Inconsistent with historical data", "FLAGGED");
            }

            // Calculate overall risk score (weighted average)
            result.calculateOverallRiskScore();

            // Determine recommendation based on overall risk
            if (result.getOverallRiskScore() >= CRITICAL_FRAUD_THRESHOLD) {
                result.setRecommendation("AUTO_REJECT");
                logFraudDetection(claim, "CRITICAL_RISK", result.getOverallRiskScore(), 
                                "Critical fraud risk - auto rejected", "AUTO_REJECTED");
            } else if (result.getOverallRiskScore() >= ADVANCED_FRAUD_THRESHOLD) {
                result.setRecommendation("MANUAL_REVIEW");
            } else {
                result.setRecommendation("APPROVE");
            }

            log.info("Advanced fraud detection completed for claim {}: {} (risk: {:.0f}%)", 
                    claim.getClaimId(), result.getRecommendation(), 
                    result.getOverallRiskScore() * 100);

        } catch (Exception e) {
            log.error("Error in advanced fraud detection for claim {}: {}", 
                     claim.getClaimId(), e.getMessage(), e);
            result.addRiskFactor("Detection system error", 0.5);
            result.setRecommendation("MANUAL_REVIEW");
        }

        return result;
    }

    private double analyzeDeliveryPatterns(Claim claim, Worker worker, List<Claim> recentClaims) {
        double riskScore = 0.0;

        // Check if claims are made outside typical delivery hours
        int claimHour = claim.getTriggeredAt().getHour();
        if (claimHour < 6 || claimHour > 22) {
            riskScore += 0.3; // Claims outside typical delivery hours
        }

        // Check frequency of claims in the same zone
        long sameZoneClaims = recentClaims.stream()
            .filter(c -> c.getZoneId().equals(claim.getZoneId()))
            .count();
        if (sameZoneClaims > 3) {
            riskScore += 0.2; // Too many claims in same zone
        }

        // Check for consistent claim amounts (potential automation)
        if (recentClaims.size() >= 3) {
            double avgAmount = recentClaims.stream()
                .mapToDouble(c -> c.getPayoutAmount().doubleValue())
                .average()
                .orElse(0.0);
            
            double variance = recentClaims.stream()
                .mapToDouble(c -> Math.pow(c.getPayoutAmount().doubleValue() - avgAmount, 2))
                .average()
                .orElse(0.0);
            
            if (variance < 100) { // Low variance suggests automation
                riskScore += 0.25;
            }
        }

        return Math.min(riskScore, 1.0);
    }

    private double analyzeTemporalPatterns(Claim claim, List<Claim> recentClaims) {
        double riskScore = 0.0;

        // Check for rapid succession claims
        if (!recentClaims.isEmpty()) {
            Claim lastClaim = recentClaims.get(0);
            long hoursSinceLastClaim = java.time.Duration.between(
                lastClaim.getTriggeredAt(), claim.getTriggeredAt()
            ).toHours();

            if (hoursSinceLastClaim < 2) {
                riskScore += 0.4; // Claims in rapid succession
            } else if (hoursSinceLastClaim < 6) {
                riskScore += 0.2; // Claims in short succession
            }
        }

        // Check for claims at regular intervals (potential automation)
        if (recentClaims.size() >= 3) {
            long[] intervals = new long[recentClaims.size() - 1];
            for (int i = 0; i < recentClaims.size() - 1; i++) {
                intervals[i] = java.time.Duration.between(
                    recentClaims.get(i + 1).getTriggeredAt(),
                    recentClaims.get(i).getTriggeredAt()
                ).toHours();
            }

            // Check if intervals are very similar (automation indicator)
            double avgInterval = java.util.Arrays.stream(intervals).average().orElse(0.0);
            double variance = java.util.Arrays.stream(intervals)
                .mapToDouble(interval -> Math.pow((double) interval - avgInterval, 2))
                .average()
                .orElse(0.0);

            if (variance < 4 && avgInterval > 0) { // Low variance in intervals
                riskScore += 0.3;
            }
        }

        return Math.min(riskScore, 1.0);
    }

    private double analyzeHistoricalConsistency(Claim claim, Worker worker) {
        double riskScore = 0.0;

        // Check if claim amount is consistent with worker's earnings
        double dailyEarnings = worker.getDailyEarnings();
        double claimAmount = claim.getPayoutAmount().doubleValue();

        if (claimAmount > dailyEarnings * 0.8) {
            riskScore += 0.4; // Claim amount too high relative to daily earnings
        }

        // Check worker's experience vs claim complexity
        if (worker.getExperienceMonths() < 3 && 
            (claim.getTriggerType().equals("air_quality") || 
             claim.getTriggerType().equals("temperature"))) {
            riskScore += 0.3; // New worker making complex claims
        }

        // Check if worker has been flagged before
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long recentFlags = fraudLogRepository.countRecentFraudLogs(
            worker.getWorkerId(), oneMonthAgo);
        
        if (recentFlags > 2) {
            riskScore += 0.5; // Repeat offender
        } else if (recentFlags > 0) {
            riskScore += 0.2; // Previous flags
        }

        return Math.min(riskScore, 1.0);
    }

    private void logFraudDetection(Claim claim, String detectionType, double riskScore,
                              String details, String actionTaken) {

    FraudDetectionLog log = FraudDetectionLog.of(
        claim.getClaimId(),
        claim.getWorkerId(),
        detectionType,
        java.math.BigDecimal.valueOf(riskScore), // ✅ FIXED
        details,
        actionTaken
    );

    fraudLogRepository.save(log); // assuming this line continues
}

    public static class AdvancedFraudResult {
        private String claimId;
        private String workerId;
        private double overallRiskScore;
        private String recommendation;
        private java.util.List<RiskFactor> riskFactors = new java.util.ArrayList<>();

        public void addRiskFactor(String description, double score) {
            riskFactors.add(new RiskFactor(description, score));
        }

        public void calculateOverallRiskScore() {
            if (riskFactors.isEmpty()) {
                overallRiskScore = 0.0;
                return;
            }

            // Weighted average - higher weights for more critical factors
            double weightedSum = 0.0;
            double totalWeight = 0.0;

            for (RiskFactor factor : riskFactors) {
                double weight = getWeightForFactor(factor.getDescription());
                weightedSum += factor.getScore() * weight;
                totalWeight += weight;
            }

            overallRiskScore = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
            overallRiskScore = Math.min(overallRiskScore, 1.0);
        }

        private double getWeightForFactor(String description) {
            if (description.contains("GPS") || description.contains("Weather")) {
                return 2.0; // High weight for GPS and weather
            } else if (description.contains("Critical") || description.contains("Historical")) {
                return 1.5; // Medium-high weight
            } else {
                return 1.0; // Normal weight
            }
        }

        // Getters and setters
        public String getClaimId() { return claimId; }
        public void setClaimId(String claimId) { this.claimId = claimId; }
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public double getOverallRiskScore() { return overallRiskScore; }
        public void setOverallRiskScore(double overallRiskScore) { this.overallRiskScore = overallRiskScore; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
        public java.util.List<RiskFactor> getRiskFactors() { return riskFactors; }
        public void setRiskFactors(java.util.List<RiskFactor> riskFactors) { this.riskFactors = riskFactors; }

        public static class RiskFactor {
            private String description;
            private double score;

            public RiskFactor(String description, double score) {
                this.description = description;
                this.score = score;
            }

            // Getters and setters
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public double getScore() { return score; }
            public void setScore(double score) { this.score = score; }
        }
    }
}

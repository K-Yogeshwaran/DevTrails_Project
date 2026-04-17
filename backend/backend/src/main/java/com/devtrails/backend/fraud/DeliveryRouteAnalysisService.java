package com.devtrails.backend.fraud;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.worker.Worker;

@Service
public class DeliveryRouteAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryRouteAnalysisService.class);

    private static final double MAX_REASONABLE_DELIVERY_RADIUS_KM = 15.0; // Maximum delivery radius
    private static final double MIN_REASONABLE_DELIVERY_SPEED_KMH = 5.0; // Minimum reasonable speed
    private static final double MAX_REASONABLE_DELIVERY_SPEED_KMH = 40.0; // Maximum delivery speed in city

    public RouteAnalysisResult analyzeDeliveryRoute(Claim claim, Worker worker, List<Claim> recentClaims) {
        RouteAnalysisResult result = new RouteAnalysisResult();
        result.setValid(true);
      /*   result.setRiskScore(0.0); */
        result.setDetails("Route analysis passed");

        try {
            // 1. Analyze delivery radius consistency
            double radiusRisk = analyzeDeliveryRadius(claim, worker);
            if (radiusRisk > 0.5) {
                result.addRiskFactor("Suspicious delivery radius", radiusRisk);
            }

            // 2. Analyze delivery speed patterns
            double speedRisk = analyzeDeliverySpeedPatterns(claim, recentClaims);
            if (speedRisk > 0.4) {
                result.addRiskFactor("Unusual delivery speed pattern", speedRisk);
            }

            // 3. Analyze route efficiency
            double efficiencyRisk = analyzeRouteEfficiency(claim, recentClaims);
            if (efficiencyRisk > 0.6) {
                result.addRiskFactor("Inefficient route pattern", efficiencyRisk);
            }

            // 4. Analyze temporal delivery patterns
            double temporalRisk = analyzeTemporalDeliveryPatterns(claim, recentClaims);
            if (temporalRisk > 0.5) {
                result.addRiskFactor("Suspicious temporal pattern", temporalRisk);
            }

            // 5. Analyze zone hopping behavior
            double zoneHoppingRisk = analyzeZoneHopping(claim, recentClaims);
            if (zoneHoppingRisk > 0.7) {
                result.addRiskFactor("Excessive zone hopping", zoneHoppingRisk);
            }

            // 6. Analyze delivery clustering
            double clusteringRisk = analyzeDeliveryClustering(claim, recentClaims);
            if (clusteringRisk > 0.4) {
                result.addRiskFactor("Suspicious delivery clustering", clusteringRisk);
            }

            // Calculate overall risk score
            result.calculateOverallRiskScore();
            result.setValid(result.getOverallRiskScore() < 0.7);

            if (!result.isValid()) {
                result.setDetails("Route analysis failed: " +
                        result.getRiskFactors().stream()
                                .map(f -> f.getDescription())
                                .collect(Collectors.joining(", ")));
            }

        } catch (Exception e) {
            log.error("Error in delivery route analysis for claim {}: {}",
                    claim.getClaimId(), e.getMessage(), e);
            result.addRiskFactor("Route analysis error", 0.5);
            result.setValid(false);
        }

        return result;
    }

    private double analyzeDeliveryRadius(Claim claim, Worker worker) {
        if (claim.getGpsLatitude() == null || claim.getGpsLongitude() == null) {
            return 0.3; // Medium risk for missing GPS
        }

        // Calculate distance from worker's typical zone center
        double distanceFromZone = calculateDistanceFromZoneCenter(
                claim.getGpsLatitude().doubleValue(),
                claim.getGpsLongitude().doubleValue(),
                worker.getZoneId());

        if (distanceFromZone > MAX_REASONABLE_DELIVERY_RADIUS_KM) {
            return Math.min(distanceFromZone / MAX_REASONABLE_DELIVERY_RADIUS_KM, 1.0);
        }

        return 0.0;
    }

    private double analyzeDeliverySpeedPatterns(Claim claim, List<Claim> recentClaims) {
        if (recentClaims.isEmpty()) {
            return 0.0;
        }

        double totalRisk = 0.0;
        int validSegments = 0;

        Claim previousClaim = claim;
        for (Claim currentClaim : recentClaims) {
            if (hasValidGPS(currentClaim) && hasValidGPS(previousClaim)) {
                double distance = calculateDistance(
                        previousClaim.getGpsLatitude().doubleValue(),
                        previousClaim.getGpsLongitude().doubleValue(),
                        currentClaim.getGpsLatitude().doubleValue(),
                        currentClaim.getGpsLongitude().doubleValue());

                long timeMinutes = ChronoUnit.MINUTES.between(
                        previousClaim.getCreatedAt(), currentClaim.getCreatedAt());

                if (timeMinutes > 0) {
                    double speedKmh = (distance / timeMinutes) * 60;

                    if (speedKmh > MAX_REASONABLE_DELIVERY_SPEED_KMH) {
                        totalRisk += 0.8; // Very suspicious speed
                    } else if (speedKmh < MIN_REASONABLE_DELIVERY_SPEED_KMH && distance > 1.0) {
                        totalRisk += 0.4; // Suspiciously slow for distance
                    }
                    validSegments++;
                }
            }
            previousClaim = currentClaim;
        }

        return validSegments > 0 ? Math.min(totalRisk / validSegments, 1.0) : 0.0;
    }

    private double analyzeRouteEfficiency(Claim claim, List<Claim> recentClaims) {
        if (recentClaims.size() < 2) {
            return 0.0;
        }

        // Calculate route efficiency score
        double totalDistance = 0.0;
        double directDistance = 0.0;

        List<Claim> allClaims = new java.util.ArrayList<>(recentClaims);
        allClaims.add(0, claim); // Include current claim

        // Calculate total path distance
        for (int i = 0; i < allClaims.size() - 1; i++) {
            Claim current = allClaims.get(i);
            Claim next = allClaims.get(i + 1);

            if (hasValidGPS(current) && hasValidGPS(next)) {
                totalDistance += calculateDistance(
                        current.getGpsLatitude().doubleValue(),
                        current.getGpsLongitude().doubleValue(),
                        next.getGpsLatitude().doubleValue(),
                        next.getGpsLongitude().doubleValue());
            }
        }

        // Calculate direct distance from start to end
        if (hasValidGPS(allClaims.get(0)) && hasValidGPS(allClaims.get(allClaims.size() - 1))) {
            Claim first = allClaims.get(0);
            Claim last = allClaims.get(allClaims.size() - 1);

            directDistance = calculateDistance(
                    first.getGpsLatitude().doubleValue(),
                    first.getGpsLongitude().doubleValue(),
                    last.getGpsLatitude().doubleValue(),
                    last.getGpsLongitude().doubleValue());
        }

        if (directDistance > 0) {
            double efficiency = directDistance / totalDistance;
            // Low efficiency suggests suspicious routing
            return 1.0 - efficiency;
        }

        return 0.0;
    }

    private double analyzeTemporalDeliveryPatterns(Claim claim, List<Claim> recentClaims) {
        double riskScore = 0.0;

        // Check for deliveries outside reasonable hours
        int hour = claim.getTriggeredAt().getHour();
        if (hour < 6 || hour > 22) {
            riskScore += 0.3; // Late night/early morning deliveries
        }

        // Check for clustering of deliveries in short time periods
        if (!recentClaims.isEmpty()) {
            long deliveriesInLastHour = recentClaims.stream()
                    .filter(c -> ChronoUnit.HOURS.between(c.getCreatedAt(), claim.getCreatedAt()) <= 1)
                    .count();

            if (deliveriesInLastHour > 5) {
                riskScore += 0.5; // Too many deliveries in short time
            }
        }

        // Check for regular intervals (potential automation)
        if (recentClaims.size() >= 3) {
            List<Long> intervals = new java.util.ArrayList<>();
            for (int i = 0; i < recentClaims.size() - 1; i++) {
                long interval = ChronoUnit.MINUTES.between(
                        recentClaims.get(i + 1).getCreatedAt(),
                        recentClaims.get(i).getCreatedAt());
                intervals.add(interval);
            }

            // Calculate variance in intervals
            double avgInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double variance = intervals.stream()
                    .mapToDouble(interval -> Math.pow(interval - avgInterval, 2))
                    .average()
                    .orElse(0.0);

            if (variance < 25 && avgInterval > 0) { // Low variance suggests automation
                riskScore += 0.4;
            }
        }

        return Math.min(riskScore, 1.0);
    }

    private double analyzeZoneHopping(Claim claim, List<Claim> recentClaims) {
        if (recentClaims.isEmpty()) {
            return 0.0;
        }

        // Count unique zones in recent claims
        List<String> recentZones = new java.util.ArrayList<>();
        recentZones.add(claim.getZoneId());
        recentZones.addAll(recentClaims.stream()
                .map(Claim::getZoneId)
                .collect(Collectors.toList()));

        long uniqueZones = recentZones.stream().distinct().count();
        long totalClaims = recentZones.size();

        // If worker is hopping between too many zones
        if (uniqueZones > 3 && totalClaims >= 5) {
            return (uniqueZones - 3.0) / 5.0; // Risk increases with more zones
        }

        return 0.0;
    }

    private double analyzeDeliveryClustering(Claim claim, List<Claim> recentClaims) {
        if (claim.getGpsLatitude() == null || claim.getGpsLongitude() == null) {
            return 0.0;
        }

        // Check for multiple claims in very close proximity
        long nearbyClaims = recentClaims.stream()
                .filter(c -> hasValidGPS(c))
                .filter(c -> {
                    double distance = calculateDistance(
                            claim.getGpsLatitude().doubleValue(),
                            claim.getGpsLongitude().doubleValue(),
                            c.getGpsLatitude().doubleValue(),
                            c.getGpsLongitude().doubleValue());
                    return distance < 0.5; // Within 500 meters
                })
                .count();

        if (nearbyClaims > 3) {
            return Math.min(nearbyClaims / 10.0, 1.0);
        }

        return 0.0;
    }

    private boolean hasValidGPS(Claim claim) {
        return claim.getGpsLatitude() != null && claim.getGpsLongitude() != null;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c; // Earth's radius in km
    }

    private double calculateDistanceFromZoneCenter(double latitude, double longitude, String zoneId) {
        // Simplified zone center calculation
        // In production, this would use actual zone boundaries
        double[][] zoneCenters = {
                { 12.9866, 77.6096 }, // zone-001 center
                { 12.9666, 77.5896 }, // zone-002 center
                { 13.0166, 77.6296 } // zone-003 center
        };

        try {
            int zoneIndex = Integer.parseInt(zoneId.split("-")[1]) - 1;
            if (zoneIndex >= 0 && zoneIndex < zoneCenters.length) {
                double[] center = zoneCenters[zoneIndex];
                return calculateDistance(latitude, longitude, center[0], center[1]);
            }
        } catch (Exception e) {
            log.warn("Invalid zone ID format: {}", zoneId);
        }

        return 0.0;
    }

    public static class RouteAnalysisResult {
        private boolean valid;
        private double overallRiskScore;
        private String details;
        private java.util.List<RiskFactor> riskFactors = new java.util.ArrayList<>();

        public void addRiskFactor(String description, double score) {
            riskFactors.add(new RiskFactor(description, score));
        }

        public void calculateOverallRiskScore() {
            if (riskFactors.isEmpty()) {
                overallRiskScore = 0.0;
                return;
            }

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
            if (description.contains("zone hopping") || description.contains("speed")) {
                return 2.0; // High weight for critical factors
            } else if (description.contains("radius") || description.contains("efficiency")) {
                return 1.5; // Medium-high weight
            } else {
                return 1.0; // Normal weight
            }
        }

        // Getters and setters
        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public double getOverallRiskScore() {
            return overallRiskScore;
        }

        public void setOverallRiskScore(double overallRiskScore) {
            this.overallRiskScore = overallRiskScore;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public java.util.List<RiskFactor> getRiskFactors() {
            return riskFactors;
        }

        public void setRiskFactors(java.util.List<RiskFactor> riskFactors) {
            this.riskFactors = riskFactors;
        }

        public static class RiskFactor {
            private String description;
            private double score;

            public RiskFactor(String description, double score) {
                this.description = description;
                this.score = score;
            }

            // Getters and setters
            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public double getScore() {
                return score;
            }

            public void setScore(double score) {
                this.score = score;
            }
        }
    }
}

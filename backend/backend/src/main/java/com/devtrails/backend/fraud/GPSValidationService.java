package com.devtrails.backend.fraud;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GPSValidationService {

    private static final Logger log = LoggerFactory.getLogger(GPSValidationService.class);

    // Zone boundaries (simplified for demo - in production, use proper geofencing)
    private static final double[][] ZONE_BOUNDARIES = {
        // zone-001: Downtown area (approximate coordinates)
        {12.9716, 77.5946, 13.0016, 77.6246}, // minLat, minLon, maxLat, maxLon
        // zone-002: Industrial area
        {12.9516, 77.5746, 12.9816, 77.6046},
        // zone-003: Residential area
        {13.0016, 77.6146, 13.0316, 77.6446}
    };

    private static final double MAX_REASONABLE_SPEED_KMH = 120.0; // Maximum reasonable speed for delivery
    private static final double EARTH_RADIUS_KM = 6371.0;

    public GPSValidationResult validateGPS(Claim claim, Worker worker, List<Claim> recentClaims) {
        GPSValidationResult result = new GPSValidationResult();
        result.setValid(true);
        result.setRiskScore(0.0);
        result.setDetails("GPS validation passed");

        if (claim.getGpsLatitude() == null || claim.getGpsLongitude() == null) {
            result.setValid(false);
            result.setRiskScore(0.6);
            result.setDetails("GPS coordinates missing");
            return result;
        }

        // Check 1: Zone validation
        if (!isLocationInValidZone(claim.getGpsLatitude().doubleValue(), 
                                   claim.getGpsLongitude().doubleValue(), 
                                   claim.getZoneId())) {
            result.setValid(false);
            result.setRiskScore(Math.max(result.getRiskScore(), 0.7));
            result.addDetail("GPS coordinates outside declared zone");
        }

        // Check 2: Advanced Movement pattern analysis
        if (!recentClaims.isEmpty()) {
            Claim lastClaim = recentClaims.get(0);
            if (lastClaim.getGpsLatitude() != null && lastClaim.getGpsLongitude() != null) {
                double distance = calculateDistance(
                    lastClaim.getGpsLatitude().doubleValue(),
                    lastClaim.getGpsLongitude().doubleValue(),
                    claim.getGpsLatitude().doubleValue(),
                    claim.getGpsLongitude().doubleValue()
                );

                long timeDiffHours = ChronoUnit.HOURS.between(
                    lastClaim.getCreatedAt(), claim.getCreatedAt()
                );

                if (timeDiffHours > 0) {
                    double speed = distance / timeDiffHours;
                    if (speed > MAX_REASONABLE_SPEED_KMH) {
                        result.setValid(false);
                        result.setRiskScore(Math.max(result.getRiskScore(), 0.8));
                        result.addDetail(String.format(
                            "Impossible movement: %.1f km in %d hours (%.1f km/h)",
                            distance, timeDiffHours, speed
                        ));
                    }
                }

                // Advanced: Check for teleportation (instantaneous location changes)
                if (timeDiffHours == 0 && distance > 0.1) {
                    result.setValid(false);
                    result.setRiskScore(Math.max(result.getRiskScore(), 0.9));
                    result.addDetail(String.format(
                        "Teleportation detected: %.1f km movement in 0 hours",
                        distance
                    ));
                }

                // Advanced: Check for unrealistic acceleration/deceleration patterns
                if (recentClaims.size() >= 2) {
                    Claim previousClaim = recentClaims.get(1);
                    if (previousClaim.getGpsLatitude() != null && previousClaim.getGpsLongitude() != null) {
                        double accelerationRisk = analyzeAccelerationPattern(
                            previousClaim, lastClaim, claim
                        );
                        if (accelerationRisk > 0.7) {
                            result.setRiskScore(Math.max(result.getRiskScore(), accelerationRisk * 0.6));
                            result.addDetail("Unrealistic acceleration/deceleration pattern detected");
                        }
                    }
                }

                // Advanced: Route feasibility analysis
                double routeRisk = analyzeRouteFeasibility(claim, recentClaims);
                if (routeRisk > 0.5) {
                    result.setRiskScore(Math.max(result.getRiskScore(), routeRisk * 0.4));
                    result.addDetail("Suspicious route pattern detected");
                }
            }
        }

        // Check 3: GPS spoofing indicators
        if (isLikelySpoofedGPS(claim.getGpsLatitude().doubleValue(), 
                               claim.getGpsLongitude().doubleValue())) {
            result.setValid(false);
            result.setRiskScore(Math.max(result.getRiskScore(), 0.9));
            result.addDetail("GPS coordinates show spoofing patterns");
        }

        // Check 4: Consistency with worker's typical locations
        if (!isLocationConsistentWithWorkerPattern(claim, worker)) {
            result.setRiskScore(result.getRiskScore() + 0.3);
            result.addDetail("Location inconsistent with worker's typical pattern");
        }

        return result;
    }

    private boolean isLocationInValidZone(double latitude, double longitude, String zoneId) {
        try {
            int zoneIndex = Integer.parseInt(zoneId.split("-")[1]) - 1;
            if (zoneIndex >= 0 && zoneIndex < ZONE_BOUNDARIES.length) {
                double[] bounds = ZONE_BOUNDARIES[zoneIndex];
                return latitude >= bounds[0] && latitude <= bounds[2] &&
                       longitude >= bounds[1] && longitude <= bounds[3];
            }
        } catch (Exception e) {
            log.warn("Invalid zone ID format: {}", zoneId);
        }
        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private boolean isLikelySpoofedGPS(double latitude, double longitude) {
        // Check for common GPS spoofing patterns
        // 1. Coordinates with too many decimal places (precision beyond GPS capability)
        BigDecimal lat = BigDecimal.valueOf(latitude).setScale(10, RoundingMode.HALF_UP);
        BigDecimal lon = BigDecimal.valueOf(longitude).setScale(10, RoundingMode.HALF_UP);
        
        // 2. Coordinates at exact 0,0 or other suspicious locations
        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            return true;
        }

        // 3. Coordinates in oceans or impossible locations (simplified check)
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return true;
        }

        // 4. Check for unrealistic precision (more than 6 decimal places is suspicious)
        String latStr = lat.toString();
        String lonStr = lon.toString();
        if (latStr.split("\\.")[1].length() > 6 || lonStr.split("\\.")[1].length() > 6) {
            return true;
        }

        // 5. Advanced: Check for round number patterns (common in spoofed coordinates)
        if (isRoundNumberPattern(latitude, longitude)) {
            return true;
        }

        // 6. Advanced: Check for coordinate clustering (same coordinates used multiple times)
        if (isSuspiciousCoordinatePattern(latitude, longitude)) {
            return true;
        }

        return false;
    }

    private boolean isRoundNumberPattern(double latitude, double longitude) {
        // Check if coordinates end with too many zeros or have suspicious patterns
        String latStr = String.format("%.6f", latitude);
        String lonStr = String.format("%.6f", longitude);
        
        // Count trailing zeros
        long latZeros = latStr.chars().filter(ch -> ch == '0').count();
        long lonZeros = lonStr.chars().filter(ch -> ch == '0').count();
        
        // If more than 30% of digits are zeros, it's suspicious
        int totalDigits = latStr.length() + lonStr.length();
        long totalZeros = latZeros + lonZeros;
        
        return (totalZeros * 100.0 / totalDigits) > 30;
    }

    private boolean isSuspiciousCoordinatePattern(double latitude, double longitude) {
        // Check for common spoofing patterns
        // 1. Coordinates that are multiples of 0.001 or 0.0001
        double latMod = latitude % 0.001;
        double lonMod = longitude % 0.001;
        
        if (Math.abs(latMod) < 0.00001 || Math.abs(lonMod) < 0.00001) {
            return true;
        }

        // 2. Check for coordinates that form geometric patterns
        if (Math.abs(latitude - longitude) < 0.001) {
            return true; // lat ≈ lon is suspicious in most real-world scenarios
        }

        // 3. Check for coordinates at cardinal points
        if (Math.abs(latitude % 1) < 0.001 || Math.abs(longitude % 1) < 0.001) {
            return true; // Integer degrees are suspicious
        }

        return false;
    }

    private double analyzeAccelerationPattern(Claim previousClaim, Claim lastClaim, Claim currentClaim) {
        try {
            // Calculate speeds for each segment
            double dist1 = calculateDistance(
                previousClaim.getGpsLatitude().doubleValue(),
                previousClaim.getGpsLongitude().doubleValue(),
                lastClaim.getGpsLatitude().doubleValue(),
                lastClaim.getGpsLongitude().doubleValue()
            );
            
            double dist2 = calculateDistance(
                lastClaim.getGpsLatitude().doubleValue(),
                lastClaim.getGpsLongitude().doubleValue(),
                currentClaim.getGpsLatitude().doubleValue(),
                currentClaim.getGpsLongitude().doubleValue()
            );

            long time1 = ChronoUnit.MINUTES.between(previousClaim.getCreatedAt(), lastClaim.getCreatedAt());
            long time2 = ChronoUnit.MINUTES.between(lastClaim.getCreatedAt(), currentClaim.getCreatedAt());

            if (time1 > 0 && time2 > 0) {
                double speed1 = (dist1 / time1) * 60; // km/h
                double speed2 = (dist2 / time2) * 60; // km/h
                
                // Check for unrealistic speed changes
                double speedChange = Math.abs(speed2 - speed1);
                double avgSpeed = (speed1 + speed2) / 2;
                
                if (avgSpeed > 0) {
                    double accelerationRatio = speedChange / avgSpeed;
                    if (accelerationRatio > 3.0) { // More than 3x speed change is suspicious
                        return Math.min(accelerationRatio / 5.0, 1.0);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error analyzing acceleration pattern: {}", e.getMessage());
        }
        
        return 0.0;
    }

    private double analyzeRouteFeasibility(Claim currentClaim, List<Claim> recentClaims) {
        double riskScore = 0.0;
        
        try {
            // Check for backtracking (visiting same location in short time)
            for (Claim claim : recentClaims) {
                if (claim.getGpsLatitude() != null && claim.getGpsLongitude() != null) {
                    double distance = calculateDistance(
                        claim.getGpsLatitude().doubleValue(),
                        claim.getGpsLongitude().doubleValue(),
                        currentClaim.getGpsLatitude().doubleValue(),
                        currentClaim.getGpsLongitude().doubleValue()
                    );
                    
                    // If very close to previous location but different zone
                    if (distance < 0.1 && !claim.getZoneId().equals(currentClaim.getZoneId())) {
                        riskScore += 0.3;
                    }
                }
            }
            
            // Check for zigzag patterns (inefficient routing)
            if (recentClaims.size() >= 2) {
                double routeEfficiency = calculateRouteEfficiency(currentClaim, recentClaims);
                if (routeEfficiency < 0.3) { // Very inefficient route
                    riskScore += 0.4;
                }
            }
            
        } catch (Exception e) {
            log.warn("Error analyzing route feasibility: {}", e.getMessage());
        }
        
        return Math.min(riskScore, 1.0);
    }

    private double calculateRouteEfficiency(Claim currentClaim, List<Claim> recentClaims) {
        // Calculate efficiency based on distance vs time
        double totalDistance = 0.0;
        long totalTime = 0;
        
        Claim previousClaim = currentClaim;
        for (Claim claim : recentClaims) {
            if (claim.getGpsLatitude() != null && claim.getGpsLongitude() != null) {
                totalDistance += calculateDistance(
                    previousClaim.getGpsLatitude().doubleValue(),
                    previousClaim.getGpsLongitude().doubleValue(),
                    claim.getGpsLatitude().doubleValue(),
                    claim.getGpsLongitude().doubleValue()
                );
                totalTime += ChronoUnit.MINUTES.between(claim.getCreatedAt(), previousClaim.getCreatedAt());
                previousClaim = claim;
            }
        }
        
        if (totalTime > 0) {
            double avgSpeed = (totalDistance / totalTime) * 60; // km/h
            // Normalize efficiency (0-1, where 1 is most efficient)
            return Math.min(avgSpeed / MAX_REASONABLE_SPEED_KMH, 1.0);
        }
        
        return 0.5; // Neutral efficiency if can't calculate
    }

    private boolean isLocationConsistentWithWorkerPattern(Claim claim, Worker worker) {
        // In a real implementation, this would analyze the worker's historical location patterns
        // For now, we'll do a basic check based on the worker's declared zone
        return worker.getZoneId().equals(claim.getZoneId());
    }

    public static class GPSValidationResult {
        private boolean valid;
        private double riskScore;
        private String details;
        private StringBuilder detailBuilder = new StringBuilder();

        public void addDetail(String detail) {
            if (detailBuilder.length() > 0) {
                detailBuilder.append("; ");
            }
            detailBuilder.append(detail);
            this.details = detailBuilder.toString();
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
        public String getDetails() { return details; }
        public void setDetails(String details) { 
            this.details = details; 
            this.detailBuilder = new StringBuilder(details != null ? details : "");
        }
    }
}

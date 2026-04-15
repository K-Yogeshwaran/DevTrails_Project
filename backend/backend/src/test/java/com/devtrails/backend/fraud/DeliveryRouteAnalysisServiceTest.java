package com.devtrails.backend.fraud;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.worker.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeliveryRouteAnalysisServiceTest {

    private DeliveryRouteAnalysisService routeAnalysisService;
    private Claim testClaim;
    private Worker testWorker;
    private List<Claim> recentClaims;

    @BeforeEach
    void setUp() {
        routeAnalysisService = new DeliveryRouteAnalysisService();
        testClaim = createTestClaim();
        testWorker = createTestWorker();
        recentClaims = Arrays.asList(createRecentClaim(1), createRecentClaim(2));
    }

    @Test
    void testAnalyzeDeliveryRoute_ValidRoute() {
        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, recentClaims);

        assertTrue(result.isValid());
        assertTrue(result.getOverallRiskScore() < 0.7);
        assertNotNull(result.getDetails());
    }

    @Test
    void testAnalyzeDeliveryRoute_MissingGPS() {
        testClaim.setGpsLatitude(null);
        testClaim.setGpsLongitude(null);

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.3);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Suspicious delivery radius")));
    }

    @Test
    void testAnalyzeDeliveryRoute_ExcessiveDeliveryRadius() {
        // Set coordinates far from zone center
        testClaim.setGpsLatitude(new BigDecimal("25.0000"));
        testClaim.setGpsLongitude(new BigDecimal("85.0000"));

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.7);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Suspicious delivery radius")));
    }

    @Test
    void testAnalyzeDeliveryRoute_ExcessiveSpeed() {
        // Create recent claims with impossible speed
        Claim fastClaim = createTestClaim();
        fastClaim.setGpsLatitude(new BigDecimal("20.0000")); // Far away
        fastClaim.setGpsLongitude(new BigDecimal("80.0000"));
        fastClaim.setCreatedAt(LocalDateTime.now().minusMinutes(10)); // Only 10 minutes ago

        List<Claim> fastClaims = Arrays.asList(fastClaim);

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, fastClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.7);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Unusual delivery speed pattern")));
    }

    @Test
    void testAnalyzeDeliveryRoute_SuspiciouslySlowSpeed() {
        // Create recent claims with suspiciously slow speed for distance
        Claim slowClaim = createTestClaim();
        slowClaim.setGpsLatitude(new BigDecimal("13.5000")); // 50+ km away
        slowClaim.setGpsLongitude(new BigDecimal("78.5000"));
        slowClaim.setCreatedAt(LocalDateTime.now().minusHours(10)); // 10 hours ago

        List<Claim> slowClaims = Arrays.asList(slowClaim);

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, slowClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.4);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Unusual delivery speed pattern")));
    }

    @Test
    void testAnalyzeDeliveryRoute_InefficientRoute() {
        // Create claims that form an inefficient route
        Claim claim1 = createTestClaim();
        claim1.setGpsLatitude(new BigDecimal("12.9000"));
        claim1.setGpsLongitude(new BigDecimal("77.5000"));

        Claim claim2 = createTestClaim();
        claim2.setGpsLatitude(new BigDecimal("13.1000"));
        claim2.setGpsLongitude(new BigDecimal("77.7000"));

        Claim claim3 = createTestClaim();
        claim3.setGpsLatitude(new BigDecimal("12.8000"));
        claim3.setGpsLongitude(new BigDecimal("77.4000"));

        List<Claim> inefficientClaims = Arrays.asList(claim1, claim2, claim3);

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, inefficientClaims);

        // Should detect inefficient routing
        assertNotNull(result);
        assertNotNull(result.getOverallRiskScore());
    }

    @Test
    void testAnalyzeDeliveryRoute_LateNightDelivery() {
        testClaim.setTriggeredAt(LocalDateTime.now().withHour(3)); // 3 AM

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, recentClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.3);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Suspicious temporal pattern")));
    }

    @Test
    void testAnalyzeDeliveryRoute_TooManyDeliveriesInShortTime() {
        // Create many claims within 1 hour
        List<Claim> clusteredClaims = Arrays.asList(
            createRecentClaim(0), createRecentClaim(0), createRecentClaim(0),
            createRecentClaim(0), createRecentClaim(0), createRecentClaim(0)
        );

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, clusteredClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.5);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Suspicious temporal pattern")));
    }

    @Test
    void testAnalyzeDeliveryRoute_RegularIntervals() {
        // Create claims with regular intervals (potential automation)
        List<Claim> regularClaims = Arrays.asList(
            createClaimAtTime(3), createClaimAtTime(2), createClaimAtTime(1)
        );

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, regularClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.4);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Suspicious temporal pattern")));
    }

    @Test
    void testAnalyzeDeliveryRoute_ExcessiveZoneHopping() {
        // Create claims in multiple zones
        Claim zone1Claim = createTestClaim();
        zone1Claim.setZoneId("zone-001");

        Claim zone2Claim = createTestClaim();
        zone2Claim.setZoneId("zone-002");

        Claim zone3Claim = createTestClaim();
        zone3Claim.setZoneId("zone-003");

        Claim zone4Claim = createTestClaim();
        zone4Claim.setZoneId("zone-001");

        Claim zone5Claim = createTestClaim();
        zone5Claim.setZoneId("zone-002");

        List<Claim> zoneHoppingClaims = Arrays.asList(zone1Claim, zone2Claim, zone3Claim, zone4Claim, zone5Claim);

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, zoneHoppingClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.7);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Excessive zone hopping")));
    }

    @Test
    void testAnalyzeDeliveryRoute_DeliveryClustering() {
        // Create claims very close to each other
        Claim clusteredClaim1 = createTestClaim();
        clusteredClaim1.setGpsLatitude(new BigDecimal("12.9716"));
        clusteredClaim1.setGpsLongitude(new BigDecimal("77.5946"));

        Claim clusteredClaim2 = createTestClaim();
        clusteredClaim2.setGpsLatitude(new BigDecimal("12.9717"));
        clusteredClaim2.setGpsLongitude(new BigDecimal("77.5947"));

        Claim clusteredClaim3 = createTestClaim();
        clusteredClaim3.setGpsLatitude(new BigDecimal("12.9718"));
        clusteredClaim3.setGpsLongitude(new BigDecimal("77.5948"));

        Claim clusteredClaim4 = createTestClaim();
        clusteredClaim4.setGpsLatitude(new BigDecimal("12.9719"));
        clusteredClaim4.setGpsLongitude(new BigDecimal("77.5949"));

        List<Claim> clusteredClaims = Arrays.asList(clusteredClaim1, clusteredClaim2, clusteredClaim3, clusteredClaim4);

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, clusteredClaims);

        assertFalse(result.isValid());
        assertTrue(result.getOverallRiskScore() >= 0.4);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Suspicious delivery clustering")));
    }

    @Test
    void testAnalyzeDeliveryRoute_NoRecentClaims() {
        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, Collections.emptyList());

        assertTrue(result.isValid());
        assertTrue(result.getOverallRiskScore() < 0.7);
    }

    @Test
    void testAnalyzeDeliveryRoute_RecentClaimsWithoutGPS() {
        List<Claim> claimsWithoutGPS = Arrays.asList(
            createClaimWithoutGPS(1),
            createClaimWithoutGPS(2)
        );

        DeliveryRouteAnalysisService.RouteAnalysisResult result = 
            routeAnalysisService.analyzeDeliveryRoute(testClaim, testWorker, claimsWithoutGPS);

        assertTrue(result.isValid());
        assertTrue(result.getOverallRiskScore() < 0.7);
    }

    @Test
    void testRouteAnalysisResultClass() {
        DeliveryRouteAnalysisService.RouteAnalysisResult result = new DeliveryRouteAnalysisService.RouteAnalysisResult();
        
        result.setValid(true);
        result.setOverallRiskScore(0.3);
        result.setDetails("Route analysis passed");
        
        result.addRiskFactor("Test factor", 0.5);
        result.calculateOverallRiskScore();

        assertTrue(result.isValid());
        assertEquals(0.3, result.getOverallRiskScore());
        assertEquals("Route analysis passed", result.getDetails());
        assertEquals(1, result.getRiskFactors().size());
        assertEquals("Test factor", result.getRiskFactors().get(0).getDescription());
        assertEquals(0.5, result.getRiskFactors().get(0).getScore());
    }

    @Test
    void testRiskFactorClass() {
        DeliveryRouteAnalysisService.RouteAnalysisResult.RiskFactor riskFactor = 
            new DeliveryRouteAnalysisService.RouteAnalysisResult.RiskFactor("Test description", 0.7);

        assertEquals("Test description", riskFactor.getDescription());
        assertEquals(0.7, riskFactor.getScore());
    }

    private Claim createTestClaim() {
        Claim claim = new Claim();
        claim.setClaimId("CLM-TEST001-1234567890");
        claim.setWorkerId("WORKER001");
        claim.setZoneId("zone-001");
        claim.setGpsLatitude(new BigDecimal("12.9716"));
        claim.setGpsLongitude(new BigDecimal("77.5946"));
        claim.setCreatedAt(LocalDateTime.now());
        claim.setTriggeredAt(LocalDateTime.now());
        return claim;
    }

    private Claim createRecentClaim(int hoursAgo) {
        Claim claim = createTestClaim();
        claim.setClaimId("CLM-TEST00" + hoursAgo + "-1234567890");
        claim.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setTriggeredAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setGpsLatitude(new BigDecimal("12.9716").add(new BigDecimal(hoursAgo * 0.01)));
        claim.setGpsLongitude(new BigDecimal("77.5946").add(new BigDecimal(hoursAgo * 0.01)));
        return claim;
    }

    private Claim createClaimAtTime(int hoursAgo) {
        Claim claim = createTestClaim();
        claim.setClaimId("CLM-TIME" + hoursAgo + "-1234567890");
        claim.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setTriggeredAt(LocalDateTime.now().minusHours(hoursAgo));
        return claim;
    }

    private Claim createClaimWithoutGPS(int hoursAgo) {
        Claim claim = createTestClaim();
        claim.setClaimId("CLM-NOGPS" + hoursAgo + "-1234567890");
        claim.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setTriggeredAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setGpsLatitude(null);
        claim.setGpsLongitude(null);
        return claim;
    }

    private Worker createTestWorker() {
        Worker worker = new Worker();
        worker.setWorkerId("WORKER001");
        worker.setZoneId("zone-001");
        return worker;
    }
}

package com.devtrails.backend.fraud;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.worker.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancedFraudDetectionServiceTest {

    @Mock
    private GPSValidationService gpsValidationService;

    @Mock
    private WeatherValidationService weatherValidationService;

    @Mock
    private DeliveryRouteAnalysisService routeAnalysisService;

    @Mock
    private FraudDetectionLogRepository fraudLogRepository;

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private AdvancedFraudDetectionService fraudDetectionService;

    private Claim testClaim;
    private Worker testWorker;
    private List<Claim> recentClaims;

    @BeforeEach
    void setUp() {
        testClaim = createTestClaim();
        testWorker = createTestWorker();
        recentClaims = Arrays.asList(createRecentClaim(1), createRecentClaim(2));
        
        when(workerRepository.findByWorkerId(anyString())).thenReturn(java.util.Optional.of(testWorker));
    }

    @Test
    void testPerformAdvancedFraudDetection_AllValidationsPass() {
        // Mock all validations to pass
        GPSValidationService.GPSValidationResult gpsResult = new GPSValidationService.GPSValidationResult();
        gpsResult.setValid(true);
        gpsResult.setRiskScore(0.1);
        when(gpsValidationService.validateGPS(any(), any(), any())).thenReturn(gpsResult);

        WeatherValidationService.WeatherValidationResult weatherResult = new WeatherValidationService.WeatherValidationResult();
        weatherResult.setValid(true);
        weatherResult.setRiskScore(0.1);
        when(weatherValidationService.validateWeather(any())).thenReturn(weatherResult);

        DeliveryRouteAnalysisService.RouteAnalysisResult routeResult = new DeliveryRouteAnalysisService.RouteAnalysisResult();
        routeResult.setValid(true);
        routeResult.setOverallRiskScore(0.1);
        when(routeAnalysisService.analyzeDeliveryRoute(any(), any(), any())).thenReturn(routeResult);

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(testClaim, recentClaims);

        // Verify
        assertNotNull(result);
        assertEquals("APPROVE", result.getRecommendation());
        assertTrue(result.getOverallRiskScore() < 0.65);
        verify(fraudLogRepository, never()).save(any());
    }

    @Test
    void testPerformAdvancedFraudDetection_GPSSpoofingDetected() {
        // Mock GPS validation to fail
        GPSValidationService.GPSValidationResult gpsResult = new GPSValidationService.GPSValidationResult();
        gpsResult.setValid(false);
        gpsResult.setRiskScore(0.8);
        gpsResult.setDetails("GPS spoofing detected");
        when(gpsValidationService.validateGPS(any(), any(), any())).thenReturn(gpsResult);

        // Mock other validations to pass
        WeatherValidationService.WeatherValidationResult weatherResult = new WeatherValidationService.WeatherValidationResult();
        weatherResult.setValid(true);
        weatherResult.setRiskScore(0.1);
        when(weatherValidationService.validateWeather(any())).thenReturn(weatherResult);

        DeliveryRouteAnalysisService.RouteAnalysisResult routeResult = new DeliveryRouteAnalysisService.RouteAnalysisResult();
        routeResult.setValid(true);
        routeResult.setOverallRiskScore(0.1);
        when(routeAnalysisService.analyzeDeliveryRoute(any(), any(), any())).thenReturn(routeResult);

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(testClaim, recentClaims);

        // Verify
        assertNotNull(result);
        assertTrue(result.getOverallRiskScore() >= 0.65);
        assertNotEquals("APPROVE", result.getRecommendation());
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("GPS validation failed")));
        verify(fraudLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void testPerformAdvancedFraudDetection_WeatherAnomalyDetected() {
        // Mock GPS validation to pass
        GPSValidationService.GPSValidationResult gpsResult = new GPSValidationService.GPSValidationResult();
        gpsResult.setValid(true);
        gpsResult.setRiskScore(0.1);
        when(gpsValidationService.validateGPS(any(), any(), any())).thenReturn(gpsResult);

        // Mock weather validation to fail
        WeatherValidationService.WeatherValidationResult weatherResult = new WeatherValidationService.WeatherValidationResult();
        weatherResult.setValid(false);
        weatherResult.setRiskScore(0.7);
        weatherResult.setDetails("Weather anomaly detected");
        when(weatherValidationService.validateWeather(any())).thenReturn(weatherResult);

        DeliveryRouteAnalysisService.RouteAnalysisResult routeResult = new DeliveryRouteAnalysisService.RouteAnalysisResult();
        routeResult.setValid(true);
        routeResult.setOverallRiskScore(0.1);
        when(routeAnalysisService.analyzeDeliveryRoute(any(), any(), any())).thenReturn(routeResult);

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(testClaim, recentClaims);

        // Verify
        assertNotNull(result);
        assertTrue(result.getOverallRiskScore() >= 0.65);
        assertNotEquals("APPROVE", result.getRecommendation());
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Weather anomaly")));
        verify(fraudLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void testPerformAdvancedFraudDetection_RouteAnalysisFails() {
        // Mock GPS and weather validations to pass
        GPSValidationService.GPSValidationResult gpsResult = new GPSValidationService.GPSValidationResult();
        gpsResult.setValid(true);
        gpsResult.setRiskScore(0.1);
        when(gpsValidationService.validateGPS(any(), any(), any())).thenReturn(gpsResult);

        WeatherValidationService.WeatherValidationResult weatherResult = new WeatherValidationService.WeatherValidationResult();
        weatherResult.setValid(true);
        weatherResult.setRiskScore(0.1);
        when(weatherValidationService.validateWeather(any())).thenReturn(weatherResult);

        // Mock route analysis to fail
        DeliveryRouteAnalysisService.RouteAnalysisResult routeResult = new DeliveryRouteAnalysisService.RouteAnalysisResult();
        routeResult.setValid(false);
        routeResult.setOverallRiskScore(0.8);
        routeResult.setDetails("Suspicious route pattern");
        when(routeAnalysisService.analyzeDeliveryRoute(any(), any(), any())).thenReturn(routeResult);

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(testClaim, recentClaims);

        // Verify
        assertNotNull(result);
        assertTrue(result.getOverallRiskScore() >= 0.65);
        assertNotEquals("APPROVE", result.getRecommendation());
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Route analysis failed")));
        verify(fraudLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void testPerformAdvancedFraudDetection_CriticalRiskAutoReject() {
        // Mock all validations to fail with high risk scores
        GPSValidationService.GPSValidationResult gpsResult = new GPSValidationService.GPSValidationResult();
        gpsResult.setValid(false);
        gpsResult.setRiskScore(0.9);
        gpsResult.setDetails("Critical GPS spoofing");
        when(gpsValidationService.validateGPS(any(), any(), any())).thenReturn(gpsResult);

        WeatherValidationService.WeatherValidationResult weatherResult = new WeatherValidationService.WeatherValidationResult();
        weatherResult.setValid(false);
        weatherResult.setRiskScore(0.9);
        weatherResult.setDetails("Critical weather anomaly");
        when(weatherValidationService.validateWeather(any())).thenReturn(weatherResult);

        DeliveryRouteAnalysisService.RouteAnalysisResult routeResult = new DeliveryRouteAnalysisService.RouteAnalysisResult();
        routeResult.setValid(false);
        routeResult.setOverallRiskScore(0.9);
        routeResult.setDetails("Critical route anomaly");
        when(routeAnalysisService.analyzeDeliveryRoute(any(), any(), any())).thenReturn(routeResult);

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(testClaim, recentClaims);

        // Verify
        assertNotNull(result);
        assertEquals("AUTO_REJECT", result.getRecommendation());
        assertTrue(result.getOverallRiskScore() >= 0.85);
        verify(fraudLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void testPerformAdvancedFraudDetection_WorkerNotFound() {
        // Mock worker not found
        when(workerRepository.findByWorkerId(anyString())).thenReturn(java.util.Optional.empty());

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(testClaim, recentClaims);

        // Verify
        assertNotNull(result);
        assertEquals("REJECT", result.getRecommendation());
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Worker not found")));
        assertEquals(1.0, result.getOverallRiskScore());
    }

    @Test
    void testPerformAdvancedFraudDetection_SystemError() {
        // Mock GPS validation to throw exception
        when(gpsValidationService.validateGPS(any(), any(), any()))
            .thenThrow(new RuntimeException("System error"));

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(testClaim, recentClaims);

        // Verify
        assertNotNull(result);
        assertEquals("MANUAL_REVIEW", result.getRecommendation());
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Detection system error")));
    }

    @Test
    void testAnalyzeDeliveryPatterns_SuspiciousPatterns() {
        // Create claims with suspicious patterns
        Claim suspiciousClaim = createSuspiciousClaim();
        List<Claim> suspiciousRecentClaims = Arrays.asList(
            createSuspiciousClaim(), createSuspiciousClaim(), createSuspiciousClaim()
        );

        // Execute
        AdvancedFraudDetectionService.AdvancedFraudResult result = 
            fraudDetectionService.performAdvancedFraudDetection(suspiciousClaim, suspiciousRecentClaims);

        // Verify
        assertNotNull(result);
        assertTrue(result.getRiskFactors().stream()
            .anyMatch(f -> f.getDescription().contains("Suspicious delivery pattern") ||
                         f.getDescription().contains("Suspicious timing pattern")));
    }

    private Claim createTestClaim() {
        Claim claim = new Claim();
        claim.setClaimId("CLM-TEST001-1234567890");
        claim.setWorkerId("WORKER001");
        claim.setPolicyNumber("POL001");
        claim.setTriggerType("rainfall");
        claim.setTriggerValue(new BigDecimal("50.0"));
        claim.setZoneId("zone-001");
        claim.setDisruptedHours(new BigDecimal("2.5"));
        claim.setPayoutAmount(new BigDecimal("150.00"));
        claim.setFraudScore(BigDecimal.ZERO);
        claim.setStatus("pending");
        claim.setTriggeredAt(LocalDateTime.now());
        claim.setGpsLatitude(new BigDecimal("12.9716"));
        claim.setGpsLongitude(new BigDecimal("77.5946"));
        return claim;
    }

    private Claim createRecentClaim(int hoursAgo) {
        Claim claim = createTestClaim();
        claim.setClaimId("CLM-TEST00" + hoursAgo + "-1234567890");
        claim.setTriggeredAt(LocalDateTime.now().minusHours(hoursAgo));
        claim.setGpsLatitude(new BigDecimal("12.9716").add(new BigDecimal(hoursAgo * 0.01)));
        claim.setGpsLongitude(new BigDecimal("77.5946").add(new BigDecimal(hoursAgo * 0.01)));
        return claim;
    }

    private Claim createSuspiciousClaim() {
        Claim claim = createTestClaim();
        // Set consistent amount (potential automation)
        claim.setPayoutAmount(new BigDecimal("100.00"));
        // Set claim outside typical hours
        claim.setTriggeredAt(LocalDateTime.now().withHour(3));
        return claim;
    }

    private Worker createTestWorker() {
        Worker worker = new Worker();
        worker.setWorkerId("WORKER001");
        worker.setName("Test Worker");
        worker.setPhone("9876543210");
        worker.setEmail("test@example.com");
        worker.setZoneId("zone-001");
        worker.setPersona("delivery");
        worker.setDailyEarnings(500);
        worker.setActiveHours(8);
        worker.setExperienceMonths(12);
        worker.setDaysPerWeek(6);
        return worker;
    }
}

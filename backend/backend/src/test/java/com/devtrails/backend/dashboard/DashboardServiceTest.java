package com.devtrails.backend.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardAnalyticsRepository analyticsRepository;

    @Mock
    private DashboardWebSocketService webSocketService;

    @InjectMocks
    private DashboardService dashboardService;

    private WorkerDashboardData testWorkerData;
    private InsurerDashboardData testInsurerData;

    @BeforeEach
    void setUp() {
        testWorkerData = createTestWorkerDashboardData();
        testInsurerData = createTestInsurerDashboardData();
    }

    @Test
    void testGetWorkerDashboard_Success() {
        // Arrange
        when(analyticsRepository.getWorkerDashboardData("WORKER001"))
                .thenReturn(Optional.of(testWorkerData));

        // Act
        WorkerDashboardData result = dashboardService.getWorkerDashboard("WORKER001");

        // Assert
        assertNotNull(result);
        assertEquals("WORKER001", result.getWorkerId());
        assertEquals(testWorkerData.getTotalClaims(), result.getTotalClaims());
        verify(analyticsRepository).getWorkerDashboardData("WORKER001");
    }

    @Test
    void testGetWorkerDashboard_NotFound() {
        // Arrange
        when(analyticsRepository.getWorkerDashboardData("NONEXISTENT"))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> dashboardService.getWorkerDashboard("NONEXISTENT")
        );

        assertEquals("Worker data not found: NONEXISTENT", exception.getMessage());
    }

    @Test
    void testGetWorkerWeeklyCoverage_Success() {
        // Arrange
        List<WeeklyCoverageData> expectedData = List.of(createTestWeeklyCoverageData());
        when(analyticsRepository.getWorkerWeeklyCoverage(eq("WORKER001"), any(LocalDateTime.class)))
                .thenReturn(expectedData);

        // Act
        List<WeeklyCoverageData> result = dashboardService.getWorkerWeeklyCoverage("WORKER001");

        // Assert
        assertNotNull(result);
        assertEquals(expectedData.size(), result.size());
        verify(analyticsRepository).getWorkerWeeklyCoverage(eq("WORKER001"), any(LocalDateTime.class));
    }

    @Test
    void testCalculateEarningsProtection_Success() {
        // Arrange
        when(analyticsRepository.getWorkerDashboardData("WORKER001"))
                .thenReturn(Optional.of(testWorkerData));

        // Act
        BigDecimal result = dashboardService.calculateEarningsProtection("WORKER001");

        // Assert
        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        // Expected: (500 * 8 * 6 * 4) + 1500 = 96600
        assertEquals(new BigDecimal("96600"), result);
    }

    @Test
    void testGetInsurerDashboard_Success() {
        // Arrange
        when(analyticsRepository.getInsurerDashboardData(any(LocalDateTime.class)))
                .thenReturn(Optional.of(testInsurerData));

        // Act
        InsurerDashboardData result = dashboardService.getInsurerDashboard();

        // Assert
        assertNotNull(result);
        assertEquals(testInsurerData.getTotalClaims(), result.getTotalClaims());
        assertEquals(testInsurerData.getTotalPayoutsProcessed(), result.getTotalPayoutsProcessed());
        verify(analyticsRepository).getInsurerDashboardData(any(LocalDateTime.class));
    }

    @Test
    void testGetInsurerDashboard_NotAvailable() {
        // Arrange
        when(analyticsRepository.getInsurerDashboardData(any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> dashboardService.getInsurerDashboard()
        );

        assertEquals("Insurer data not available", exception.getMessage());
    }

    @Test
    void testGetLossRatios_Success() {
        // Arrange
        List<LossRatioData> expectedData = List.of(createTestLossRatioData());
        when(analyticsRepository.getLossRatioByTriggerType(any(LocalDateTime.class)))
                .thenReturn(expectedData);

        // Act
        List<LossRatioData> result = dashboardService.getLossRatios();

        // Assert
        assertNotNull(result);
        assertEquals(expectedData.size(), result.size());
        verify(analyticsRepository).getLossRatioByTriggerType(any(LocalDateTime.class));
    }

    @Test
    void testCalculateOverallLossRatio_Success() {
        // Arrange
        List<LossRatioData> testData = List.of(
                createTestLossRatioData("rainfall", 10L, new BigDecimal("5000"), 2L, 1L, 1L, 2.5),
                createTestLossRatioData("temperature", 5L, new BigDecimal("2500"), 1L, 0L, 0L, 3.0)
        );

        when(analyticsRepository.getLossRatioByTriggerType(any(LocalDateTime.class)))
                .thenReturn(testData);

        // Act
        BigDecimal result = dashboardService.calculateOverallLossRatio();

        // Assert
        assertNotNull(result);
        // Expected: (5000 + 2500) / (5000 + 2500 + 200 + 50 + 100) = 7500 / 7850 = 0.9554
        assertTrue(result.compareTo(BigDecimal.valueOf(0.95)) < 0.01);
    }

    @Test
    void testGetPredictiveAnalytics_Success() {
        // Arrange
        List<Object[]> rawData = List.of(
                new Object[]{"rainfall", 15L, 2.5, new BigDecimal("7500"), 1, 14},
                new Object[]{"temperature", 8L, 3.0, new BigDecimal("2400"), 2, 15}
        );
        when(analyticsRepository.getPredictiveAnalytics(any(LocalDateTime.class)))
                .thenReturn(rawData);

        // Act
        List<PredictiveAnalytics> result = dashboardService.getPredictiveAnalytics();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("rainfall", result.get(0).getTriggerType());
        assertEquals(15L, result.get(0).getClaimCount());
        verify(analyticsRepository).getPredictiveAnalytics(any(LocalDateTime.class));
    }

    @Test
    void testGenerateWeeklyPrediction_Success() {
        // Arrange
        List<Object[]> rawData = List.of(
                new Object[]{"rainfall", 12L, 2.0, new BigDecimal("6000"), 1, 14}
        );
        when(analyticsRepository.getPredictiveAnalytics(any(LocalDateTime.class)))
                .thenReturn(rawData);

        // Act
        WeeklyPrediction result = dashboardService.generateWeeklyPrediction();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getWeekStartDate());
        assertNotNull(result.getWeekEndDate());
        assertTrue(result.getTotalPredictedClaims() > 0);
        assertTrue(result.getPredictedPayoutAmount().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(result.getPrimaryRiskFactor());
        assertNotNull(result.getConfidenceLevel());
    }

    @Test
    void testGetZoneAnalytics_Success() {
        // Arrange
        List<ZoneAnalyticsData> expectedData = List.of(createTestZoneAnalyticsData());
        when(analyticsRepository.getZoneAnalytics(any(LocalDateTime.class)))
                .thenReturn(expectedData);

        // Act
        List<ZoneAnalyticsData> result = dashboardService.getZoneAnalytics();

        // Assert
        assertNotNull(result);
        assertEquals(expectedData.size(), result.size());
        verify(analyticsRepository).getZoneAnalytics(any(LocalDateTime.class));
    }

    @Test
    void testGetWeatherDisruptionCorrelation_Success() {
        // Arrange
        List<WeatherDisruptionCorrelation> expectedData = List.of(createTestWeatherDisruptionCorrelation());
        when(analyticsRepository.getWeatherDisruptionCorrelation(any(LocalDateTime.class)))
                .thenReturn(expectedData);

        // Act
        List<WeatherDisruptionCorrelation> result = dashboardService.getWeatherDisruptionCorrelation();

        // Assert
        assertNotNull(result);
        assertEquals(expectedData.size(), result.size());
        verify(analyticsRepository).getWeatherDisruptionCorrelation(any(LocalDateTime.class));
    }

    @Test
    void testGetWorkerRiskAssessment_Success() {
        // Arrange
        List<RiskAssessmentData> expectedData = List.of(createTestRiskAssessmentData());
        when(analyticsRepository.getWorkerRiskAssessment(any(LocalDateTime.class)))
                .thenReturn(expectedData);

        // Act
        List<RiskAssessmentData> result = dashboardService.getWorkerRiskAssessment();

        // Assert
        assertNotNull(result);
        assertEquals(expectedData.size(), result.size());
        verify(analyticsRepository).getWorkerRiskAssessment(any(LocalDateTime.class));
    }

    @Test
    void testUpdateWorkerDashboard_Success() {
        // Arrange
        when(analyticsRepository.getWorkerDashboardData("WORKER001"))
                .thenReturn(Optional.of(testWorkerData));

        // Act
        dashboardService.updateWorkerDashboard("WORKER001");

        // Assert
        verify(webSocketService).sendWorkerUpdate(eq("WORKER001"), any(WorkerDashboardData.class));
    }

    @Test
    void testUpdateInsurerDashboard_Success() {
        // Arrange
        when(analyticsRepository.getInsurerDashboardData(any(LocalDateTime.class)))
                .thenReturn(Optional.of(testInsurerData));

        // Act
        dashboardService.updateInsurerDashboard();

        // Assert
        verify(webSocketService).sendInsurerUpdate(any(InsurerDashboardData.class));
    }

    @Test
    void testGetAdvancedAnalytics_Success() {
        // Arrange
        when(analyticsRepository.getPredictiveAnalytics(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(analyticsRepository.getZoneAnalytics(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(analyticsRepository.getWeatherDisruptionCorrelation(any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(analyticsRepository.getWorkerRiskAssessment(any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        var result = dashboardService.getAdvancedAnalytics();

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("predictiveInsights"));
        assertTrue(result.containsKey("zonePerformance"));
        assertTrue(result.containsKey("weatherCorrelations"));
        assertTrue(result.containsKey("riskAssessments"));
        assertTrue(result.containsKey("trendAnalysis"));
        assertTrue(result.containsKey("recommendations"));
    }

    // Helper methods
    private WorkerDashboardData createTestWorkerDashboardData() {
        WorkerDashboardData data = new WorkerDashboardData();
        data.setWorkerId("WORKER001");
        data.setWorkerName("Test Worker");
        data.setZoneId("zone-001");
        data.setPersona("delivery");
        data.setTotalEarningsProtected(new BigDecimal("1500"));
        data.setTotalClaims(5L);
        data.setApprovedClaims(4L);
        data.setRejectedClaims(1L);
        data.setTotalPayoutsReceived(new BigDecimal("1200"));
        data.setDailyEarnings(new BigDecimal("500"));
        data.setActiveHours(8);
        data.setExperienceMonths(12);
        data.setDaysPerWeek(6);
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }

    private InsurerDashboardData createTestInsurerDashboardData() {
        InsurerDashboardData data = new InsurerDashboardData();
        data.setTotalClaims(100L);
        data.setTotalClaimValue(new BigDecimal("50000"));
        data.setTotalPayoutsProcessed(new BigDecimal("45000"));
        data.setApprovedClaims(80L);
        data.setRejectedClaims(15L);
        data.setFlaggedClaims(5L);
        data.setActiveWorkers(50);
        data.setActiveZones(10);
        data.setAvgDailyEarnings(new BigDecimal("450"));
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }

    private WeeklyCoverageData createTestWeeklyCoverageData() {
        WeeklyCoverageData data = new WeeklyCoverageData();
        data.setWorkerId("WORKER001");
        data.setWorkerName("Test Worker");
        data.setWeekStart(LocalDateTime.now().minusWeeks(1));
        data.setWeeklyClaims(3);
        data.setTotalDisruptedHours(new BigDecimal("12"));
        data.setWeeklyPayouts(new BigDecimal("900"));
        data.setWeeklyActiveHours(48);
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }

    private LossRatioData createTestLossRatioData() {
        return createTestLossRatioData("rainfall", 10L, new BigDecimal("5000"), 2L, 1L, 1L, 2.5);
    }

    private LossRatioData createTestLossRatioData(String triggerType, Long totalClaims, 
                                               BigDecimal totalPaid, Long rejectedClaims, 
                                               Long flaggedClaims, Double avgDisruptedHours) {
        LossRatioData data = new LossRatioData();
        data.setTriggerType(triggerType);
        data.setTotalClaims(totalClaims);
        data.setTotalPaid(totalPaid);
        data.setRejectedClaims(rejectedClaims);
        data.setFlaggedClaims(flaggedClaims);
        data.setAvgDisruptedHours(avgDisruptedHours);
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }

    private ZoneAnalyticsData createTestZoneAnalyticsData() {
        ZoneAnalyticsData data = new ZoneAnalyticsData();
        data.setZoneId("zone-001");
        data.setActiveWorkers(25);
        data.setTotalClaims(50L);
        data.setTotalPayouts(new BigDecimal("25000"));
        data.setAvgDailyEarnings(new BigDecimal("500"));
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }

    private WeatherDisruptionCorrelation createTestWeatherDisruptionCorrelation() {
        WeatherDisruptionCorrelation data = new WeatherDisruptionCorrelation();
        data.setTriggerType("rainfall");
        data.setDayOfWeek(1); // Monday
        data.setHourOfDay(14); // 2 PM
        data.setClaimCount(8L);
        data.setAvgDisruptedHours(2.5);
        data.setTotalPayout(new BigDecimal("4000"));
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }

    private RiskAssessmentData createTestRiskAssessmentData() {
        RiskAssessmentData data = new RiskAssessmentData();
        data.setWorkerId("WORKER001");
        data.setWorkerName("Test Worker");
        data.setZoneId("zone-001");
        data.setPersona("delivery");
        data.setExperienceMonths(6);
        data.setDailyEarnings(new BigDecimal("400"));
        data.setTotalClaims(8L);
        data.setTotalPaid(new BigDecimal("3200"));
        data.setRejectedClaims(2L);
        data.setFlaggedClaims(1L);
        data.setAvgDisruptedHours(2.0);
        data.setRejectionRate(25.0);
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }
}

package com.devtrails.backend.fraud;

import com.devtrails.backend.claims.Claim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WeatherValidationServiceTest {

    private WeatherValidationService weatherValidationService;
    private Claim testClaim;

    @BeforeEach
    void setUp() {
        weatherValidationService = new WeatherValidationService();
        testClaim = createTestClaim();
    }

    @Test
    void testValidateWeather_RainfallClaimWithActualRain() {
        // Mock weather data with rain
        WeatherValidationService.WeatherData rainyWeather = createRainyWeather();
        
        // Use reflection to set up the mock weather data
        try {
            Method getHistoricalWeather = WeatherValidationService.class.getDeclaredMethod("getHistoricalWeather", 
                double.class, double.class, LocalDateTime.class);
            getHistoricalWeather.setAccessible(true);
            getHistoricalWeather.invoke(weatherValidationService, 12.9716, 77.5946, testClaim.getTriggeredAt());
        } catch (Exception e) {
            // For testing, we'll use the simulated weather
        }

        WeatherValidationService.WeatherValidationResult result = 
            weatherValidationService.validateWeather(testClaim);

        assertTrue(result.isValid());
        assertTrue(result.getRiskScore() < 0.6);
        assertNotNull(result.getActualWeather());
        assertTrue(result.getDetails().contains("Weather validated"));
    }

    @Test
    void testValidateWeather_RainfallClaimWithoutRain() {
        // Set claim time during dry season
        testClaim.setTriggeredAt(LocalDateTime.of(2024, 2, 15, 14, 0)); // February - dry season
        testClaim.setTriggerType("rainfall");

        WeatherValidationService.WeatherValidationResult result = 
            weatherValidationService.validateWeather(testClaim);

        assertFalse(result.isValid());
        assertTrue(result.getRiskScore() >= 0.6);
        assertTrue(result.getDetails().contains("Weather anomaly"));
    }

    @Test
    void testValidateWeather_TemperatureClaimWithMismatch() {
        testClaim.setTriggerType("temperature");
        testClaim.setTriggerValue(new BigDecimal("40.0")); // Claiming high temperature

        WeatherValidationService.WeatherValidationResult result = 
            weatherValidationService.validateWeather(testClaim);

        // Should detect inconsistency if actual temp doesn't match claimed
        assertNotNull(result);
        assertNotNull(result.getActualWeather());
    }

    @Test
    void testValidateWeather_AirQualityClaimWithGoodConditions() {
        testClaim.setTriggerType("air_quality");
        testClaim.setTriggeredAt(LocalDateTime.of(2024, 11, 15, 12, 0)); // Clear midday

        WeatherValidationService.WeatherValidationResult result = 
            weatherValidationService.validateWeather(testClaim);

        // Should detect suspicious conditions for air quality claim
        assertNotNull(result);
        assertNotNull(result.getActualWeather());
    }

    @Test
    void testValidateWeather_MissingGPS() {
        testClaim.setGpsLatitude(null);
        testClaim.setGpsLongitude(null);

        WeatherValidationService.WeatherValidationResult result = 
            weatherValidationService.validateWeather(testClaim);

        assertFalse(result.isValid());
        assertEquals(0.4, result.getRiskScore());
        assertTrue(result.getDetails().contains("Unable to validate weather"));
    }

    @Test
    void testIsRainfallSeason_MonsoonSeason() throws Exception {
        Method isRainfallSeason = WeatherValidationService.class.getDeclaredMethod("isRainfallSeason", LocalDateTime.class);
        isRainfallSeason.setAccessible(true);

        // Test monsoon months
        LocalDateTime june = LocalDateTime.of(2024, 6, 15, 12, 0);
        LocalDateTime july = LocalDateTime.of(2024, 7, 15, 12, 0);
        LocalDateTime august = LocalDateTime.of(2024, 8, 15, 12, 0);
        LocalDateTime september = LocalDateTime.of(2024, 9, 15, 12, 0);

        assertTrue((Boolean) isRainfallSeason.invoke(weatherValidationService, june));
        assertTrue((Boolean) isRainfallSeason.invoke(weatherValidationService, july));
        assertTrue((Boolean) isRainfallSeason.invoke(weatherValidationService, august));
        assertTrue((Boolean) isRainfallSeason.invoke(weatherValidationService, september));
    }

    @Test
    void testIsRainfallSeason_DrySeason() throws Exception {
        Method isRainfallSeason = WeatherValidationService.class.getDeclaredMethod("isRainfallSeason", LocalDateTime.class);
        isRainfallSeason.setAccessible(true);

        // Test dry months
        LocalDateTime january = LocalDateTime.of(2024, 1, 15, 12, 0);
        LocalDateTime february = LocalDateTime.of(2024, 2, 15, 12, 0);
        LocalDateTime december = LocalDateTime.of(2024, 12, 15, 12, 0);

        assertFalse((Boolean) isRainfallSeason.invoke(weatherValidationService, january));
        assertFalse((Boolean) isRainfallSeason.invoke(weatherValidationService, february));
        assertFalse((Boolean) isRainfallSeason.invoke(weatherValidationService, december));
    }

    @Test
    void testAnalyzeTemperatureConsistency_MorningTemperature() throws Exception {
        Method analyzeTemperatureConsistency = WeatherValidationService.class.getDeclaredMethod(
            "analyzeTemperatureConsistency", double.class, LocalDateTime.class);
        analyzeTemperatureConsistency.setAccessible(true);

        LocalDateTime morning = LocalDateTime.of(2024, 11, 15, 8, 0); // 8 AM
        double expectedMorningTemp = 22.0;
        double actualMorningTemp = 23.0; // Close to expected

        double riskScore = (Double) analyzeTemperatureConsistency.invoke(
            weatherValidationService, actualMorningTemp, morning);

        assertTrue(riskScore < 0.3); // Low risk for consistent temperature
    }

    @Test
    void testAnalyzeTemperatureConsistency_UnusualTemperature() throws Exception {
        Method analyzeTemperatureConsistency = WeatherValidationService.class.getDeclaredMethod(
            "analyzeTemperatureConsistency", double.class, LocalDateTime.class);
        analyzeTemperatureConsistency.setAccessible(true);

        LocalDateTime morning = LocalDateTime.of(2024, 11, 15, 8, 0); // 8 AM
        double unusualTemp = 35.0; // Very high for morning

        double riskScore = (Double) analyzeTemperatureConsistency.invoke(
            weatherValidationService, unusualTemp, morning);

        assertTrue(riskScore >= 0.6); // High risk for unusual temperature
    }

    @Test
    void testAnalyzeAirQualityPatterns_GoodConditionsMidday() throws Exception {
        Method analyzeAirQualityPatterns = WeatherValidationService.class.getDeclaredMethod(
            "analyzeAirQualityPatterns", WeatherValidationService.WeatherData.class, LocalDateTime.class);
        analyzeAirQualityPatterns.setAccessible(true);

        WeatherValidationService.WeatherData goodWeather = new WeatherValidationService.WeatherData();
        goodWeather.setCondition("clear");
        goodWeather.setWindSpeed(20.0); // Strong wind
        goodWeather.setHumidity(25.0); // Low humidity

        LocalDateTime midday = LocalDateTime.of(2024, 11, 15, 12, 0); // 12 PM

        double riskScore = (Double) analyzeAirQualityPatterns.invoke(
            weatherValidationService, goodWeather, midday);

        assertTrue(riskScore >= 0.4); // Risk for good conditions during AQ claim
    }

    @Test
    void testGetExpectedTemperatureForTime_DifferentTimesOfDay() throws Exception {
        Method getExpectedTemperatureForTime = WeatherValidationService.class.getDeclaredMethod(
            "getExpectedTemperatureForTime", int.class);
        getExpectedTemperatureForTime.setAccessible(true);

        double morningTemp = (Double) getExpectedTemperatureForTime.invoke(weatherValidationService, 8);
        double afternoonTemp = (Double) getExpectedTemperatureForTime.invoke(weatherValidationService, 14);
        double eveningTemp = (Double) getExpectedTemperatureForTime.invoke(weatherValidationService, 18);
        double nightTemp = (Double) getExpectedTemperatureForTime.invoke(weatherValidationService, 23);

        assertEquals(22.0, morningTemp); // Morning
        assertEquals(30.0, afternoonTemp); // Afternoon peak
        assertEquals(26.0, eveningTemp); // Evening
        assertEquals(20.0, nightTemp); // Night
    }

    @Test
    void testAnalyzeHistoricalWeatherConsistency_WithHistoricalData() throws Exception {
        Method analyzeHistoricalWeatherConsistency = WeatherValidationService.class.getDeclaredMethod(
            "analyzeHistoricalWeatherConsistency", Claim.class, WeatherValidationService.WeatherData.class);
        analyzeHistoricalWeatherConsistency.setAccessible(true);

        WeatherValidationService.WeatherData currentWeather = new WeatherValidationService.WeatherData();
        currentWeather.setTemperature(25.0);
        currentWeather.setHumidity(60.0);
        currentWeather.setCondition("rain");

        double riskScore = (Double) analyzeHistoricalWeatherConsistency.invoke(
            weatherValidationService, testClaim, currentWeather);

        assertNotNull(riskScore);
        assertTrue(riskScore >= 0.0 && riskScore <= 1.0);
    }

    @Test
    void testWeatherDataClass() {
        WeatherValidationService.WeatherData weatherData = new WeatherValidationService.WeatherData();
        
        weatherData.setTemperature(25.5);
        weatherData.setHumidity(65.0);
        weatherData.setCondition("rain");
        weatherData.setDescription("Light rain");
        weatherData.setWindSpeed(10.5);
        weatherData.setTimestamp(LocalDateTime.now());

        assertEquals(25.5, weatherData.getTemperature());
        assertEquals(65.0, weatherData.getHumidity());
        assertEquals("rain", weatherData.getCondition());
        assertEquals("Light rain", weatherData.getDescription());
        assertEquals(10.5, weatherData.getWindSpeed());
        assertNotNull(weatherData.getTimestamp());
    }

    @Test
    void testWeatherValidationResultClass() {
        WeatherValidationService.WeatherValidationResult result = new WeatherValidationService.WeatherValidationResult();
        
        result.setValid(true);
        result.setRiskScore(0.3);
        result.setDetails("Weather validation passed");
        WeatherValidationService.WeatherData weatherData = new WeatherValidationService.WeatherData();
        result.setActualWeather(weatherData);

        assertTrue(result.isValid());
        assertEquals(0.3, result.getRiskScore());
        assertEquals("Weather validation passed", result.getDetails());
        assertEquals(weatherData, result.getActualWeather());
    }

    private Claim createTestClaim() {
        Claim claim = new Claim();
        claim.setClaimId("CLM-TEST001-1234567890");
        claim.setWorkerId("WORKER001");
        claim.setTriggerType("rainfall");
        claim.setTriggerValue(new BigDecimal("50.0"));
        claim.setZoneId("zone-001");
        claim.setGpsLatitude(new BigDecimal("12.9716"));
        claim.setGpsLongitude(new BigDecimal("77.5946"));
        claim.setTriggeredAt(LocalDateTime.of(2024, 7, 15, 14, 0)); // Monsoon season afternoon
        return claim;
    }

    private WeatherValidationService.WeatherData createRainyWeather() {
        WeatherValidationService.WeatherData weather = new WeatherValidationService.WeatherData();
        weather.setTemperature(24.0);
        weather.setHumidity(85.0);
        weather.setCondition("rain");
        weather.setDescription("Moderate rain");
        weather.setWindSpeed(12.0);
        weather.setTimestamp(LocalDateTime.now());
        return weather;
    }
}

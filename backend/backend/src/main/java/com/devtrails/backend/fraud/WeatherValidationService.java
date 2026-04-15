package com.devtrails.backend.fraud;

import com.devtrails.backend.claims.Claim;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherValidationService {

    @Value("${weather.api.key:demo_key}")
    private String weatherApiKey;

    @Value("${weather.api.url:http://api.openweathermap.org/data/2.5}")
    private String weatherApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Historical weather data cache (in production, use proper caching)
    private final WeatherDataCache weatherCache = new WeatherDataCache();

    public WeatherValidationResult validateWeather(Claim claim) {
        WeatherValidationResult result = new WeatherValidationResult();
        result.setValid(true);
        result.setRiskScore(0.0);
        result.setDetails("Weather validation passed");

        try {
            // Get actual weather data for the claim time and location
            WeatherData actualWeather = getHistoricalWeather(
                claim.getGpsLatitude().doubleValue(),
                claim.getGpsLongitude().doubleValue(),
                claim.getTriggeredAt()
            );

            if (actualWeather == null) {
                result.setValid(false);
                result.setRiskScore(0.4);
                result.setDetails("Unable to validate weather - service unavailable");
                return result;
            }

            // Store actual weather for audit
            claim.setWeatherActual(actualWeather.getCondition());
            claim.setWeatherValidated(true);

            // Validate claim against actual weather
            double riskScore = calculateWeatherRiskScore(claim, actualWeather);
            result.setRiskScore(riskScore);
            result.setActualWeather(actualWeather);
            result.setValid(riskScore < 0.6);

            if (riskScore >= 0.6) {
                result.setDetails(buildWeatherFailureMessage(claim, actualWeather, riskScore));
            } else {
                result.setDetails(String.format(
                    "Weather validated: %s at %.1f°C, humidity %.0f%%",
                    actualWeather.getCondition(),
                    actualWeather.getTemperature(),
                    actualWeather.getHumidity()
                ));
            }

        } catch (Exception e) {
            log.error("Weather validation failed for claim {}: {}", claim.getClaimId(), e.getMessage());
            result.setValid(false);
            result.setRiskScore(0.3);
            result.setDetails("Weather validation error: " + e.getMessage());
        }

        return result;
    }

    private WeatherData getHistoricalWeather(double latitude, double longitude, LocalDateTime dateTime) {
        // Check cache first
        String cacheKey = String.format("%.4f,%.4f,%s", latitude, longitude, 
                                       dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH")));
        
        WeatherData cached = weatherCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            // For historical weather, we'd use a different endpoint in production
            // For demo, we'll simulate with current weather
            String url = String.format(
                "%s/weather?lat=%.4f&lon=%.4f&appid=%s&units=metric",
                weatherApiUrl, latitude, longitude, weatherApiKey
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode weatherJson = objectMapper.readTree(response);

            WeatherData data = new WeatherData();
            data.setTemperature(weatherJson.get("main").get("temp").asDouble());
            data.setHumidity(weatherJson.get("main").get("humidity").asDouble());
            data.setCondition(weatherJson.get("weather").get(0).get("main").asText().toLowerCase());
            data.setDescription(weatherJson.get("weather").get(0).get("description").asText());
            data.setWindSpeed(weatherJson.get("wind").get("speed").asDouble());
            data.setTimestamp(LocalDateTime.now());

            // Cache the result
            weatherCache.put(cacheKey, data);
            return data;

        } catch (Exception e) {
            log.warn("Failed to fetch weather data: {}", e.getMessage());
            // Return simulated data for demo purposes
            return getSimulatedWeatherData(dateTime);
        }
    }

    private WeatherData getSimulatedWeatherData(LocalDateTime dateTime) {
        WeatherData data = new WeatherData();
        // Simulate weather based on time of day and season
        int hour = dateTime.getHour();
        int month = dateTime.getMonthValue();
        
        if (month >= 6 && month <= 9) { // Monsoon season
            if (hour >= 14 && hour <= 18) {
                data.setCondition("rain");
                data.setTemperature(25.0 + Math.random() * 5);
                data.setHumidity(80.0 + Math.random() * 15);
                data.setWindSpeed(15.0 + Math.random() * 10);
            } else {
                data.setCondition("clouds");
                data.setTemperature(28.0 + Math.random() * 4);
                data.setHumidity(70.0 + Math.random() * 10);
                data.setWindSpeed(10.0 + Math.random() * 5);
            }
        } else { // Other seasons
            data.setCondition(hour >= 6 && hour <= 18 ? "clear" : "clouds");
            data.setTemperature(30.0 + Math.random() * 5);
            data.setHumidity(50.0 + Math.random() * 20);
            data.setWindSpeed(5.0 + Math.random() * 10);
        }
        
        data.setDescription("Simulated weather for demo");
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    private double calculateWeatherRiskScore(Claim claim, WeatherData actualWeather) {
        double riskScore = 0.0;

        switch (claim.getTriggerType().toLowerCase()) {
            case "rainfall":
                // For rainfall claims, check if it actually rained
                if (!actualWeather.getCondition().contains("rain") && 
                    !actualWeather.getCondition().contains("drizzle") &&
                    !actualWeather.getCondition().contains("thunderstorm")) {
                    riskScore += 0.8; // High risk - no rain during rainfall claim
                } else if (actualWeather.getHumidity() < 60) {
                    riskScore += 0.4; // Medium risk - low humidity during rain claim
                }
                
                // Advanced: Check rainfall seasonality
                if (!isRainfallSeason(claim.getTriggeredAt())) {
                    riskScore += 0.3; // Suspicious - rainfall claim outside rainy season
                }
                break;

            case "temperature":
                // For temperature claims, check if temperature was extreme
                double temp = actualWeather.getTemperature();
                if (temp > 35 && claim.getTriggerValue().doubleValue() < 35) {
                    riskScore += 0.6; // Risk - claiming low temp during high temp
                } else if (temp < 20 && claim.getTriggerValue().doubleValue() > 30) {
                    riskScore += 0.6; // Risk - claiming high temp during low temp
                }
                
                // Advanced: Check temperature consistency with time of day
                double tempRisk = analyzeTemperatureConsistency(temp, claim.getTriggeredAt());
                riskScore += tempRisk * 0.3;
                break;

            case "air_quality":
                // For air quality claims, check conditions
                if (actualWeather.getCondition().contains("clear") && 
                    actualWeather.getWindSpeed() > 15) {
                    riskScore += 0.5; // Medium risk - good conditions during AQ claim
                }
                
                // Advanced: Check air quality patterns
                double aqRisk = analyzeAirQualityPatterns(actualWeather, claim.getTriggeredAt());
                riskScore += aqRisk * 0.4;
                break;

            default:
                // For unknown trigger types, apply basic validation
                if (actualWeather.getCondition().contains("clear") && 
                    actualWeather.getWindSpeed() < 5) {
                    riskScore += 0.3; // Low risk - good conditions
                }
                break;
        }

        // Advanced: Historical consistency check
        double historicalRisk = analyzeHistoricalWeatherConsistency(claim, actualWeather);
        riskScore += historicalRisk * 0.5;

        // Additional risk factors
        if (actualWeather.getHumidity() > 95) {
            riskScore += 0.1; // Slightly suspicious - extremely high humidity
        }

        return Math.min(riskScore, 1.0);
    }

    private boolean isRainfallSeason(LocalDateTime dateTime) {
        int month = dateTime.getMonthValue();
        // Monsoon season in Bangalore area: June to September
        return month >= 6 && month <= 9;
    }

    private double analyzeTemperatureConsistency(double actualTemp, LocalDateTime claimTime) {
        int hour = claimTime.getHour();
        double expectedTemp = getExpectedTemperatureForTime(hour);
        
        double tempDifference = Math.abs(actualTemp - expectedTemp);
        
        // If actual temperature deviates significantly from expected pattern
        if (tempDifference > 8) {
            return 0.6; // High risk - unusual temperature pattern
        } else if (tempDifference > 5) {
            return 0.3; // Medium risk
        }
        
        return 0.0;
    }

    private double getExpectedTemperatureForTime(int hour) {
        // Simplified temperature pattern for Bangalore area
        if (hour >= 6 && hour <= 9) {
            return 22.0; // Morning
        } else if (hour >= 10 && hour <= 15) {
            return 30.0; // Afternoon peak
        } else if (hour >= 16 && hour <= 19) {
            return 26.0; // Evening
        } else {
            return 20.0; // Night
        }
    }

    private double analyzeAirQualityPatterns(WeatherData weather, LocalDateTime claimTime) {
        double riskScore = 0.0;
        
        // Air quality is typically worse in mornings and evenings
        int hour = claimTime.getHour();
        if (hour >= 10 && hour <= 15) {
            // Midday with clear skies and good wind suggests good air quality
            if (weather.getCondition().contains("clear") && weather.getWindSpeed() > 10) {
                riskScore += 0.4; // Suspicious - good conditions during AQ claim
            }
        }
        
        // Check for unrealistic air quality conditions
        if (weather.getHumidity() < 30 && weather.getWindSpeed() > 20) {
            riskScore += 0.3; // Very dry and windy suggests good air quality
        }
        
        return Math.min(riskScore, 1.0);
    }

    private double analyzeHistoricalWeatherConsistency(Claim claim, WeatherData actualWeather) {
        double riskScore = 0.0;
        
        try {
            // Get historical weather patterns for this location and time
            List<WeatherData> historicalData = getHistoricalWeatherPatterns(
                claim.getGpsLatitude().doubleValue(),
                claim.getGpsLongitude().doubleValue(),
                claim.getTriggeredAt()
            );
            
            if (!historicalData.isEmpty()) {
                // Calculate average historical conditions
                double avgTemp = historicalData.stream()
                    .mapToDouble(WeatherData::getTemperature)
                    .average()
                    .orElse(actualWeather.getTemperature());
                
                double avgHumidity = historicalData.stream()
                    .mapToDouble(WeatherData::getHumidity)
                    .average()
                    .orElse(actualWeather.getHumidity());
                
                // Check if current weather deviates significantly from historical patterns
                double tempDeviation = Math.abs(actualWeather.getTemperature() - avgTemp);
                double humidityDeviation = Math.abs(actualWeather.getHumidity() - avgHumidity);
                
                if (tempDeviation > 10) {
                    riskScore += 0.3; // Significant temperature deviation
                }
                
                if (humidityDeviation > 25) {
                    riskScore += 0.2; // Significant humidity deviation
                }
                
                // Check for unusual weather combinations
                if (actualWeather.getCondition().contains("rain") && avgHumidity < 40) {
                    riskScore += 0.4; // Rain in historically dry conditions
                }
            }
            
        } catch (Exception e) {
            log.warn("Error analyzing historical weather consistency: {}", e.getMessage());
        }
        
        return Math.min(riskScore, 1.0);
    }

    private List<WeatherData> getHistoricalWeatherPatterns(double latitude, double longitude, LocalDateTime dateTime) {
        // In a real implementation, this would query a historical weather database
        // For demo, we'll return simulated historical data
        List<WeatherData> historicalData = new java.util.ArrayList<>();
        
        // Simulate 7 days of historical data
        for (int i = 1; i <= 7; i++) {
            WeatherData data = getSimulatedWeatherData(dateTime.minusDays(i));
            // Add some variation to simulate real historical data
            data.setTemperature(data.getTemperature() + (Math.random() - 0.5) * 3);
            data.setHumidity(Math.max(20, Math.min(95, data.getHumidity() + (Math.random() - 0.5) * 10)));
            historicalData.add(data);
        }
        
        return historicalData;
    }

    private String buildWeatherFailureMessage(Claim claim, WeatherData actualWeather, double riskScore) {
        StringBuilder message = new StringBuilder();
        message.append(String.format(
            "Weather anomaly detected: %s vs claimed %s. ",
            actualWeather.getCondition(),
            claim.getTriggerType()
        ));
        
        if (claim.getTriggerType().equalsIgnoreCase("rainfall") && 
            !actualWeather.getCondition().contains("rain")) {
            message.append("No rainfall detected during claim period. ");
        }
        
        message.append(String.format(
            "Risk score: %.0f%%",
            riskScore * 100
        ));
        
        return message.toString();
    }

    public static class WeatherValidationResult {
        private boolean valid;
        private double riskScore;
        private String details;
        private WeatherData actualWeather;

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public WeatherData getActualWeather() { return actualWeather; }
        public void setActualWeather(WeatherData actualWeather) { this.actualWeather = actualWeather; }
    }

    public static class WeatherData {
        private double temperature;
        private double humidity;
        private String condition;
        private String description;
        private double windSpeed;
        private LocalDateTime timestamp;

        // Getters and setters
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public double getHumidity() { return humidity; }
        public void setHumidity(double humidity) { this.humidity = humidity; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    private static class WeatherDataCache {
        private final java.util.concurrent.ConcurrentHashMap<String, WeatherData> cache = 
            new java.util.concurrent.ConcurrentHashMap<>();

        public WeatherData get(String key) {
            return cache.get(key);
        }

        public void put(String key, WeatherData data) {
            cache.put(key, data);
        }
    }
}

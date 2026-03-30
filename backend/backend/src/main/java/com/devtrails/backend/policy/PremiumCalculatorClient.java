package com.devtrails.backend.policy;


import com.devtrails.backend.worker.Worker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

@Component
@Slf4j
public class PremiumCalculatorClient {


    @Value("${ml.premium.url}")
    private String mlPremiumUrl;


    private final RestTemplate restTemplate = new RestTemplate();


    public BigDecimal calculatePremium(Worker worker, String season) {
        try {
            String url = mlPremiumUrl + "/api/premium/calculate";

            // Build the request body — same fields the ML API expects
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("worker_id",          worker.getWorkerId());
            requestBody.put("zone_id",            worker.getZoneId());
            requestBody.put("persona",            worker.getPersona());
            requestBody.put("daily_earnings",     worker.getDailyEarnings());
            requestBody.put("active_hours",       worker.getActiveHours());
            requestBody.put("shift",              worker.getShift());
            requestBody.put("season",             season);
            requestBody.put("days_per_week",      worker.getDaysPerWeek());
            requestBody.put("experience_months",  worker.getExperienceMonths());

            // Set Content-Type header to application/json
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Make the POST request
            // Map.class tells RestTemplate to parse the JSON response
            // into a Java Map<String, Object>
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Extract weekly_premium from the response
                Object premium = response.getBody().get("weekly_premium");
                if (premium != null) {
                    log.info("ML premium calculated for {}: ₹{}", worker.getWorkerId(), premium);
                    return new BigDecimal(premium.toString());
                }
            }

        } catch (Exception e) {
            log.warn("ML Premium API unreachable: {}. Using fallback premium.", e.getMessage());
        }

        return getFallbackPremium(worker, season);
    }

    private BigDecimal getFallbackPremium(Worker worker, String season) {
        double weeklyEarnings = worker.getDailyEarnings() * worker.getDaysPerWeek();
        double seasonMultiplier = switch (season) {
            case "monsoon" -> 1.3;
            case "summer"  -> 0.9;
            case "winter"  -> 0.8;
            default        -> 1.0;
        };
        double premium = weeklyEarnings * 0.018 * seasonMultiplier;
        // Clamp to valid range
        premium = Math.max(49, Math.min(200, premium));
        log.info("Using fallback premium for {}: ₹{}", worker.getWorkerId(), premium);
        return BigDecimal.valueOf(Math.round(premium));
    }
}

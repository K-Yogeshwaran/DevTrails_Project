package com.devtrails.backend.policy;

import com.devtrails.backend.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class PayoutCalculatorClient {

    private static final Logger log = LoggerFactory.getLogger(PayoutCalculatorClient.class);

    @Value("${ml.premium.url}")
    private String mlUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Calls the ML payout model at claim time.
     * Returns the predicted payout amount for this specific disruption event.
     * Falls back to the deterministic formula if ML is unreachable.
     */
    public BigDecimal calculatePayout(Worker worker, String triggerType,
                                      double disruptedHours, String season) {
        try {
            String url = mlUrl + "/api/payout/calculate";

            Map<String, Object> body = new HashMap<>();
            body.put("worker_id",          worker.getWorkerId());
            body.put("zone_id",            worker.getZoneId());
            body.put("persona",            worker.getPersona());
            body.put("trigger_type",       triggerType);
            body.put("daily_earnings",     worker.getDailyEarnings());
            body.put("active_hours",       worker.getActiveHours());
            body.put("disrupted_hours",    disruptedHours);
            body.put("season",             season);
            body.put("experience_months",  worker.getExperienceMonths());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object payout = response.getBody().get("payout_amount");
                if (payout != null) {
                    log.info("ML payout for worker {} | trigger={} | amount=₹{}",
                            worker.getWorkerId(), triggerType, payout);
                    return new BigDecimal(payout.toString());
                }
            }
        } catch (Exception e) {
            log.warn("ML Payout API unreachable: {}. Using fallback formula.", e.getMessage());
        }

        return fallbackPayout(worker, triggerType, disruptedHours);
    }

    // Deterministic fallback: hourly_rate × disrupted_hours × persona_factor
    private BigDecimal fallbackPayout(Worker worker, String triggerType, double disruptedHours) {
        double hourlyRate = (double) worker.getDailyEarnings() / worker.getActiveHours();
        double personaFactor = switch (worker.getPersona()) {
            case "food"      -> 1.20;
            case "ecommerce" -> 0.85;
            default          -> 1.00;
        };
        double payout = hourlyRate * disruptedHours * personaFactor;
        payout = Math.max(50, Math.min(5000, payout));
        log.info("Fallback payout for {}: ₹{}", worker.getWorkerId(), payout);
        return BigDecimal.valueOf(Math.round(payout * 100.0) / 100.0);
    }
}

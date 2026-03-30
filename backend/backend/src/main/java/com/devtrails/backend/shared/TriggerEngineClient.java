package com.devtrails.backend.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TriggerEngineClient {

    @Value("${trigger.engine.url}")
    private String triggerEngineUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── FETCH PENDING TRIGGERS ────────────────────────────
    // Calls GET /api/triggers from the trigger engine
    // Returns a list of trigger events with status = pending_payout
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchPendingTriggers() {
        try {
            String url = triggerEngineUrl + "/api/triggers";

            // ParameterizedTypeReference tells RestTemplate
            // to parse the JSON array into List<Map<String, Object>>
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK
                    && response.getBody() != null) {

                // Filter: only return events with status = pending_payout
                return response.getBody().stream()
                        .filter(event -> "pending_payout".equals(event.get("status")))
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Could not reach trigger engine: {}", e.getMessage());
        }

        // Return empty list if trigger engine is unreachable
        return Collections.emptyList();
    }

    // ── CHECK TRIGGER ENGINE HEALTH ───────────────────────
    public boolean isHealthy() {
        try {
            String url = triggerEngineUrl + "/api/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
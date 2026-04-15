package com.devtrails.backend.payout.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class UPISimulatorGateway implements PaymentGateway {

    @Value("${upi.simulator.url}")
    private String upiSimulatorUrl;

    @Value("${upi.simulator.enabled}")
    private boolean upiSimulatorEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getGatewayName() {
        return "UPI_SIMULATOR";
    }

    @Override
    public PayoutResult initiatePayout(PayoutRequest request) {
        if (!upiSimulatorEnabled) {
            return PayoutResult.builder()
                    .success(false)
                    .transactionId(null)
                    .status("FAILED")
                    .message("UPI Simulator is disabled")
                    .build();
        }

        try {
            // Simulate UPI payout request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vpa", request.getUpiId());
            requestBody.put("amount", request.getAmount());
            requestBody.put("currency", "INR");
            requestBody.put("reference_id", request.getReferenceId());
            requestBody.put("beneficiary_name", request.getBeneficiaryName());
            requestBody.put("narration", "DevTrails Claim Payout - " + request.getClaimId());
            requestBody.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Simulator-Auth", "devtrails-simulator-key");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    upiSimulatorUrl + "/api/upi/payout",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String transactionId = (String) responseBody.get("transaction_id");
                String status = (String) responseBody.get("status");
                String message = (String) responseBody.get("message");

                log.info("UPI Simulator payout initiated: {} for amount: {}", 
                        transactionId, request.getAmount());

                return PayoutResult.builder()
                        .success("SUCCESS".equals(status))
                        .transactionId(transactionId)
                        .status(mapUPIStatus(status))
                        .message(message)
                        .gatewayResponse(responseBody.toString())
                        .build();
            } else {
                return PayoutResult.builder()
                        .success(false)
                        .transactionId(null)
                        .status("FAILED")
                        .message("UPI Simulator error: " + response.getStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("UPI Simulator payout failed: {}", e.getMessage(), e);
            
            // Fallback to simulated response if simulator is not available
            return createSimulatedPayout(request);
        }
    }

    @Override
    public PayoutStatus checkPayoutStatus(String transactionId) {
        if (!upiSimulatorEnabled) {
            return PayoutStatus.builder()
                    .transactionId(transactionId)
                    .status("UNKNOWN")
                    .message("UPI Simulator is disabled")
                    .build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Simulator-Auth", "devtrails-simulator-key");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    upiSimulatorUrl + "/api/upi/status/" + transactionId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String status = (String) responseBody.get("status");
                BigDecimal amount = new BigDecimal(responseBody.get("amount").toString());
                String message = (String) responseBody.get("message");

                return PayoutStatus.builder()
                        .transactionId(transactionId)
                        .status(mapUPIStatus(status))
                        .amount(amount)
                        .message(message)
                        .gatewayResponse(responseBody.toString())
                        .build();
            } else {
                return PayoutStatus.builder()
                        .transactionId(transactionId)
                        .status("UNKNOWN")
                        .message("Error checking status: " + response.getStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error checking UPI payout status: {}", e.getMessage(), e);
            return PayoutStatus.builder()
                    .transactionId(transactionId)
                    .status("UNKNOWN")
                    .message("Error checking status: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean validateWebhookSignature(String payload, String signature) {
        try {
            // In production, implement actual webhook signature validation
            // For demo, we'll simulate validation
            return payload != null && !payload.isEmpty() && signature != null;
        } catch (Exception e) {
            log.error("Webhook signature validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public RefundResult refundPayout(String transactionId, BigDecimal amount, String reason) {
        if (!upiSimulatorEnabled) {
            return RefundResult.builder()
                    .success(false)
                    .message("UPI Simulator is disabled")
                    .build();
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("original_transaction_id", transactionId);
            requestBody.put("refund_amount", amount);
            requestBody.put("reason", reason);
            requestBody.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Simulator-Auth", "devtrails-simulator-key");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    upiSimulatorUrl + "/api/upi/refund",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String refundId = (String) responseBody.get("refund_id");
                String status = (String) responseBody.get("status");
                String message = (String) responseBody.get("message");

                log.info("UPI Simulator refund created: {} for original transaction: {}", 
                        refundId, transactionId);

                return RefundResult.builder()
                        .success("SUCCESS".equals(status))
                        .refundId(refundId)
                        .message(message)
                        .gatewayResponse(responseBody.toString())
                        .build();
            } else {
                return RefundResult.builder()
                        .success(false)
                        .message("Refund failed: " + response.getStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error processing UPI refund: {}", e.getMessage(), e);
            return RefundResult.builder()
                    .success(false)
                    .message("Refund failed: " + e.getMessage())
                    .build();
        }
    }

    private PayoutResult createSimulatedPayout(PayoutRequest request) {
        // Create a simulated successful payout when simulator is not available
        String transactionId = "UPI_SIM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        log.info("Creating simulated UPI payout: {} for amount: {}", 
                transactionId, request.getAmount());

        return PayoutResult.builder()
                .success(true)
                .transactionId(transactionId)
                .status("PROCESSING")
                .message("Simulated payout created - will be marked as completed in 2 minutes")
                .gatewayResponse("{\"simulated\": true, \"message\": \"UPI Simulator fallback\"}")
                .build();
    }

    private String mapUPIStatus(String upiStatus) {
        return switch (upiStatus.toUpperCase()) {
            case "INITIATED" -> "PENDING";
            case "PROCESSING" -> "PROCESSING";
            case "SUCCESS" -> "COMPLETED";
            case "FAILED" -> "FAILED";
            case "REFUNDED" -> "REFUNDED";
            case "TIMEOUT" -> "FAILED";
            default -> "UNKNOWN";
        };
    }
}

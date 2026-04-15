package com.devtrails.backend.payout;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.claims.ClaimRepository;
import com.devtrails.backend.config.ApiException;
import com.devtrails.backend.worker.Worker;
import com.devtrails.backend.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PayoutController {

    private final PayoutService payoutService;
    private final PayoutRepository payoutRepository;
    private final ClaimRepository claimRepository;
    private final WorkerRepository workerRepository;

    // POST /api/payouts/initiate
    @PostMapping("/initiate")
    public ResponseEntity<PayoutResponse> initiatePayout(
            @Valid @RequestBody PayoutInitiateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // Get worker from authenticated user
            Worker worker = workerRepository.findByWorkerId(userDetails.getUsername())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Worker not found"));

            // Get claim
            Claim claim = claimRepository.findByClaimId(request.getClaimId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));

            // Verify claim belongs to worker
            if (!claim.getWorkerId().equals(worker.getWorkerId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Claim does not belong to worker");
            }

            // Verify claim is approved
            if (!"approved".equals(claim.getStatus())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Claim is not approved");
            }

            // Initiate payout
            Payout payout = payoutService.initiatePayout(
                    claim, worker, request.getPaymentMethod(),
                    request.getUpiId(), request.getAccountNumber(), request.getIfscCode()
            );

            return ResponseEntity.ok(PayoutResponse.from(payout));

        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            log.error("Error initiating payout: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initiate payout");
        }
    }

    // GET /api/payouts/{payoutId}
    @GetMapping("/{payoutId}")
    public ResponseEntity<PayoutResponse> getPayout(@PathVariable String payoutId) {
        Payout payout = payoutRepository.findByPayoutId(payoutId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payout not found"));

        return ResponseEntity.ok(PayoutResponse.from(payout));
    }

    // GET /api/payouts/worker/{workerId}
    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<PayoutResponse>> getWorkerPayouts(@PathVariable String workerId) {
        List<Payout> payouts = payoutRepository.findByWorkerIdOrderByCreatedAtDesc(workerId);
        List<PayoutResponse> response = payouts.stream()
                .map(PayoutResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    // GET /api/payouts/claim/{claimId}
    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<PayoutResponse>> getClaimPayouts(@PathVariable String claimId) {
        List<Payout> payouts = payoutRepository.findByClaimIdOrderByCreatedAtDesc(claimId);
        List<PayoutResponse> response = payouts.stream()
                .map(PayoutResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    // POST /api/payouts/{payoutId}/status
    @PostMapping("/{payoutId}/status")
    public ResponseEntity<PayoutResponse> checkPayoutStatus(@PathVariable String payoutId) {
        try {
            Payout payout = payoutService.checkPayoutStatus(payoutId);
            return ResponseEntity.ok(PayoutResponse.from(payout));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Error checking payout status: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check payout status");
        }
    }

    // POST /api/payouts/{payoutId}/refund
    @PostMapping("/{payoutId}/refund")
    public ResponseEntity<PayoutResponse> refundPayout(
            @PathVariable String payoutId,
            @Valid @RequestBody RefundRequest request) {
        
        try {
            Payout payout = payoutService.refundPayout(payoutId, request.getReason());
            return ResponseEntity.ok(PayoutResponse.from(payout));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // POST /api/payouts/webhook/razorpay
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        try {
            log.info("Received Razorpay webhook: {}", payload);
            
            // Extract payout ID from webhook payload
            // In production, parse actual webhook structure
            String payoutId = extractPayoutIdFromWebhook(payload, "razorpay");
            
            if (payoutId != null) {
                String status = extractStatusFromWebhook(payload, "razorpay");
                payoutService.updatePayoutFromWebhook(payoutId, status, payload, signature);
            }
            
            return ResponseEntity.ok("Webhook received");
            
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    // POST /api/payouts/webhook/stripe
    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        try {
            log.info("Received Stripe webhook: {}", payload);
            
            String payoutId = extractPayoutIdFromWebhook(payload, "stripe");
            
            if (payoutId != null) {
                String status = extractStatusFromWebhook(payload, "stripe");
                payoutService.updatePayoutFromWebhook(payoutId, status, payload, signature);
            }
            
            return ResponseEntity.ok("Webhook received");
            
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    // POST /api/payouts/webhook/upi
    @PostMapping("/webhook/upi")
    public ResponseEntity<String> handleUPIWebhook(
            @RequestBody String payload,
            @RequestHeader("X-UPI-Signature") String signature) {
        
        try {
            log.info("Received UPI webhook: {}", payload);
            
            String payoutId = extractPayoutIdFromWebhook(payload, "upi");
            
            if (payoutId != null) {
                String status = extractStatusFromWebhook(payload, "upi");
                payoutService.updatePayoutFromWebhook(payoutId, status, payload, signature);
            }
            
            return ResponseEntity.ok("Webhook received");
            
        } catch (Exception e) {
            log.error("Error processing UPI webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    // GET /api/payouts/analytics
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getPayoutAnalytics() {
        java.time.LocalDateTime oneMonthAgo = java.time.LocalDateTime.now().minusMonths(1);
        
        // Get analytics data
        List<Object[]> dailyStats = payoutRepository.getDailyPayoutStatsSince(oneMonthAgo);
        List<Object[]> methodBreakdown = payoutRepository.getPayoutBreakdownSince(oneMonthAgo);
        
        // Calculate totals
        long totalPayouts = payoutRepository.findByStatusInOrderByCreatedAtDesc(
                List.of("COMPLETED")
        ).size();
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalPayouts", totalPayouts);
        analytics.put("dailyStats", dailyStats);
        analytics.put("methodBreakdown", methodBreakdown);
        
        return ResponseEntity.ok(analytics);
    }

    // GET /api/payouts/health
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "payout-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    // Helper methods
    private String extractPayoutIdFromWebhook(String payload, String gateway) {
        // Simplified extraction - in production, parse actual webhook JSON
        try {
            if (payload.contains("payout_id") || payload.contains("reference_id")) {
                // Extract reference_id or similar field
                // This is a simplified implementation
                if (payload.contains("PYT-")) {
                    int start = payload.indexOf("PYT-");
                    int end = payload.indexOf("\"", start);
                    if (end > start) {
                        return payload.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting payout ID from {} webhook: {}", gateway, e.getMessage());
        }
        return null;
    }

    private String extractStatusFromWebhook(String payload, String gateway) {
        // Simplified status extraction
        try {
            if (payload.contains("\"status\"")) {
                int start = payload.indexOf("\"status\":\"") + 10;
                int end = payload.indexOf("\"", start);
                if (end > start) {
                    return payload.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting status from {} webhook: {}", gateway, e.getMessage());
        }
        return "UNKNOWN";
    }

    // DTOs
    public static class PayoutInitiateRequest {
        @NotBlank(message = "Claim ID is required")
        private String claimId;

        @NotBlank(message = "Payment method is required")
        @Pattern(regexp = "^(RAZORPAY|STRIPE|UPI_SIMULATOR)$", 
                 message = "Payment method must be RAZORPAY, STRIPE, or UPI_SIMULATOR")
        private String paymentMethod;

        private String upiId;
        private String accountNumber;
        private String ifscCode;

        // Getters and setters
        public String getClaimId() { return claimId; }
        public void setClaimId(String claimId) { this.claimId = claimId; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getIfscCode() { return ifscCode; }
        public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    }

    public static class RefundRequest {
        @NotBlank(message = "Refund reason is required")
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class PayoutResponse {
        private String payoutId;
        private String claimId;
        private String workerId;
        private String paymentMethod;
        private String paymentGatewayId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String failureReason;
        private String upiId;
        private String accountNumber;
        private String ifscCode;
        private String beneficiaryName;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime processedAt;
        private java.time.LocalDateTime completedAt;

        public static PayoutResponse from(Payout payout) {
            PayoutResponse response = new PayoutResponse();
            response.payoutId = payout.getPayoutId();
            response.claimId = payout.getClaimId();
            response.workerId = payout.getWorkerId();
            response.paymentMethod = payout.getPaymentMethod();
            response.paymentGatewayId = payout.getPaymentGatewayId();
            response.amount = payout.getAmount();
            response.currency = payout.getCurrency();
            response.status = payout.getStatus();
            response.failureReason = payout.getFailureReason();
            response.upiId = payout.getUpiId();
            response.accountNumber = maskAccountNumber(payout.getAccountNumber());
            response.ifscCode = payout.getIfscCode();
            response.beneficiaryName = payout.getBeneficiaryName();
            response.createdAt = payout.getCreatedAt();
            response.processedAt = payout.getProcessedAt();
            response.completedAt = payout.getCompletedAt();
            return response;
        }

        private static String maskAccountNumber(String accountNumber) {
            if (accountNumber == null || accountNumber.length() < 4) {
                return accountNumber;
            }
            return "XXXXXX" + accountNumber.substring(accountNumber.length() - 4);
        }

        // Getters and setters
        public String getPayoutId() { return payoutId; }
        public void setPayoutId(String payoutId) { this.payoutId = payoutId; }
        public String getClaimId() { return claimId; }
        public void setClaimId(String claimId) { this.claimId = claimId; }
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getPaymentGatewayId() { return paymentGatewayId; }
        public void setPaymentGatewayId(String paymentGatewayId) { this.paymentGatewayId = paymentGatewayId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getIfscCode() { return ifscCode; }
        public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
        public String getBeneficiaryName() { return beneficiaryName; }
        public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(java.time.LocalDateTime processedAt) { this.processedAt = processedAt; }
        public java.time.LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(java.time.LocalDateTime completedAt) { this.completedAt = completedAt; }
    }
}

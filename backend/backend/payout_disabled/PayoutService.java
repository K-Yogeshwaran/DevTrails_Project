package com.devtrails.backend.payout;

import com.devtrails.backend.claims.Claim;
import com.devtrails.backend.payout.gateway.PaymentGateway;
import com.devtrails.backend.payout.gateway.RazorpayPayoutGateway;
import com.devtrails.backend.payout.gateway.StripePayoutGateway;
import com.devtrails.backend.payout.gateway.UPISimulatorGateway;
import com.devtrails.backend.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private final PayoutRepository payoutRepository;
    private final RazorpayPayoutGateway razorpayGateway;
    private final StripePayoutGateway stripeGateway;
    private final UPISimulatorGateway upiSimulatorGateway;
    
    public PayoutService(PayoutRepository payoutRepository,
                         RazorpayPayoutGateway razorpayGateway,
                         StripePayoutGateway stripeGateway,
                         UPISimulatorGateway upiSimulatorGateway) {
        this.payoutRepository = payoutRepository;
        this.razorpayGateway = razorpayGateway;
        this.stripeGateway = stripeGateway;
        this.upiSimulatorGateway = upiSimulatorGateway;
    }
    
    @Value("${payout.min.amount}")
    private BigDecimal minPayoutAmount;
    
    @Value("${payout.max.amount}")
    private BigDecimal maxPayoutAmount;
    
    @Value("${payout.auto.approve.threshold}")
    private BigDecimal autoApproveThreshold;

    // Cache for payment gateways
    private final Map<String, PaymentGateway> gatewayCache = new ConcurrentHashMap<>();

    // Initialize gateway cache
    private Map<String, PaymentGateway> getGatewayCache() {
        if (gatewayCache.isEmpty()) {
            gatewayCache.put("RAZORPAY", razorpayGateway);
            gatewayCache.put("STRIPE", stripeGateway);
            gatewayCache.put("UPI_SIMULATOR", upiSimulatorGateway);
        }
        return gatewayCache;
    }

    @Transactional
    public Payout initiatePayout(Claim claim, Worker worker, String paymentMethod, 
                               String upiId, String accountNumber, String ifscCode) {
        
        // Validate payout amount
        if (claim.getPayoutAmount().compareTo(minPayoutAmount) < 0) {
            throw new IllegalArgumentException("Payout amount below minimum threshold: " + minPayoutAmount);
        }
        
        if (claim.getPayoutAmount().compareTo(maxPayoutAmount) > 0) {
            throw new IllegalArgumentException("Payout amount above maximum threshold: " + maxPayoutAmount);
        }

        // Check for existing payout for this claim
        List<Payout> existingPayouts = payoutRepository.findByClaimIdOrderByCreatedAtDesc(claim.getClaimId());
        if (!existingPayouts.isEmpty()) {
            Payout existing = existingPayouts.get(0);
            if (!existing.isFailed()) {
                throw new IllegalStateException("Payout already exists for claim: " + claim.getClaimId());
            }
        }

        // Create payout record
        Payout payout = new Payout();
        payout.setPayoutId(generatePayoutId(worker.getWorkerId()));
        payout.setClaimId(claim.getClaimId());
        payout.setWorkerId(worker.getWorkerId());
        payout.setPaymentMethod(paymentMethod.toUpperCase());
        payout.setAmount(claim.getPayoutAmount());
        payout.setUpiId(upiId);
        payout.setAccountNumber(accountNumber);
        payout.setIfscCode(ifscCode);
        payout.setBeneficiaryName(worker.getName());
        payout.setStatus("PENDING");

        // Save payout record
        payout = payoutRepository.save(payout);

        // Initiate payment asynchronously
        if (shouldAutoApprove(claim, worker)) {
            processPayoutAsync(payout, claim, worker);
        } else {
            log.info("Payout {} requires manual review. Amount: {}", 
                    payout.getPayoutId(), claim.getPayoutAmount());
        }

        return payout;
    }

    @Async
    public void processPayoutAsync(Payout payout, Claim claim, Worker worker) {
        try {
            log.info("Processing payout {} for claim {} with amount {}", 
                    payout.getPayoutId(), claim.getClaimId(), claim.getPayoutAmount());

            // Mark as processing
            payout.markAsProcessing();
            payoutRepository.save(payout);

            // Get appropriate gateway
            PaymentGateway gateway = getGatewayCache().get(payout.getPaymentMethod());
            if (gateway == null) {
                throw new IllegalArgumentException("Unsupported payment method: " + payout.getPaymentMethod());
            }

            // Create payout request
            PaymentGateway.PayoutRequest request = new PaymentGateway.PayoutRequest(
                    worker.getWorkerId(),
                    claim.getClaimId(),
                    payout.getPayoutId(),
                    claim.getPayoutAmount(),
                    payout.getUpiId(),
                    payout.getAccountNumber(),
                    payout.getIfscCode(),
                    worker.getName()
            );

            // Initiate payout
            PaymentGateway.PayoutResult result = gateway.initiatePayout(request);

            // Update payout record
            if (result.isSuccess()) {
                payout.setPaymentGatewayId(result.getTransactionId());
                payout.setGatewayResponse(result.getGatewayResponse());
                payoutRepository.save(payout);

                log.info("Payout {} initiated successfully with transaction ID: {}", 
                        payout.getPayoutId(), result.getTransactionId());
            } else {
                payout.markAsFailed(result.getMessage(), result.getGatewayResponse());
                payoutRepository.save(payout);

                log.error("Payout {} failed: {}", payout.getPayoutId(), result.getMessage());
            }

        } catch (Exception e) {
            log.error("Error processing payout {}: {}", payout.getPayoutId(), e.getMessage(), e);
            payout.markAsFailed("System error: " + e.getMessage(), e.getMessage());
            payoutRepository.save(payout);
        }
    }

    @Transactional
    public Payout checkPayoutStatus(String payoutId) {
        Payout payout = payoutRepository.findByPayoutId(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));

        if (payout.isCompleted() || payout.isFailed()) {
            return payout; // Already in final state
        }

        try {
            PaymentGateway gateway = getGatewayCache().get(payout.getPaymentMethod());
            if (gateway == null) {
                log.warn("Unknown payment method for payout {}: {}", 
                        payoutId, payout.getPaymentMethod());
                return payout;
            }

            PaymentGateway.PayoutStatus status = gateway.checkPayoutStatus(payout.getPaymentGatewayId());
            
            // Update payout status based on gateway response
            switch (status.getStatus().toUpperCase()) {
                case "COMPLETED":
                    payout.markAsCompleted(status.getGatewayResponse());
                    break;
                case "FAILED":
                    payout.markAsFailed("Payment gateway reported failure", status.getGatewayResponse());
                    break;
                case "PROCESSING":
                    // Still processing, no change needed
                    break;
                default:
                    log.warn("Unknown status from gateway for payout {}: {}", 
                            payoutId, status.getStatus());
                    break;
            }

            payoutRepository.save(payout);
            log.info("Updated status for payout {} to: {}", payoutId, status.getStatus());

        } catch (Exception e) {
            log.error("Error checking status for payout {}: {}", payoutId, e.getMessage(), e);
        }

        return payout;
    }

    @Transactional
    public Payout refundPayout(String payoutId, String reason) {
        Payout payout = payoutRepository.findByPayoutId(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));

        if (!payout.isCompleted()) {
            throw new IllegalStateException("Cannot refund non-completed payout: " + payoutId);
        }

        try {
            PaymentGateway gateway = getGatewayCache().get(payout.getPaymentMethod());
            if (gateway == null) {
                throw new IllegalArgumentException("Unsupported payment method: " + payout.getPaymentMethod());
            }

            PaymentGateway.RefundResult result = gateway.refundPayout(
                    payout.getPaymentGatewayId(), 
                    payout.getAmount(), 
                    reason
            );

            if (result.isSuccess()) {
                payout.markAsRefunded(reason + " - Refund ID: " + result.getRefundId());
                log.info("Payout {} refunded successfully. Refund ID: {}", 
                        payoutId, result.getRefundId());
            } else {
                log.error("Refund failed for payout {}: {}", payoutId, result.getMessage());
                throw new RuntimeException("Refund failed: " + result.getMessage());
            }

        } catch (Exception e) {
            log.error("Error refunding payout {}: {}", payoutId, e.getMessage(), e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }

        return payoutRepository.save(payout);
    }

    @Transactional
    public Payout updatePayoutFromWebhook(String payoutId, String gatewayStatus, 
                                       String gatewayResponse, String signature) {
        Payout payout = payoutRepository.findByPayoutId(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));

        try {
            // Validate webhook signature
            PaymentGateway gateway = getGatewayCache().get(payout.getPaymentMethod());
            if (gateway != null && !gateway.validateWebhookSignature(gatewayResponse, signature)) {
                throw new SecurityException("Invalid webhook signature");
            }

            // Update status based on webhook
            switch (gatewayStatus.toUpperCase()) {
                case "COMPLETED":
                case "PAID":
                    payout.markAsCompleted(gatewayResponse);
                    break;
                case "FAILED":
                    payout.markAsFailed("Payment failed via webhook", gatewayResponse);
                    break;
                case "REFUNDED":
                    payout.markAsRefunded("Refunded via webhook");
                    break;
                default:
                    log.warn("Unknown webhook status for payout {}: {}", payoutId, gatewayStatus);
                    return payout;
            }

            payout.setWebhookReceived(true);
            payout = payoutRepository.save(payout);

            log.info("Updated payout {} from webhook: {}", payoutId, gatewayStatus);

        } catch (SecurityException e) {
            log.error("Webhook security validation failed for payout {}: {}", payoutId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error processing webhook for payout {}: {}", payoutId, e.getMessage(), e);
        }

        return payout;
    }

    // Scheduled tasks for payout management
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkPendingPayouts() {
        log.debug("Checking pending payouts...");
        
        List<Payout> pendingPayouts = payoutRepository.findPendingPayoutsSince(
                LocalDateTime.now().minusMinutes(30)
        );

        for (Payout payout : pendingPayouts) {
            try {
                // Re-attempt processing
                if (payout.getPaymentGatewayId() == null) {
                    log.info("Re-attempting payout processing for: {}", payout.getPayoutId());
                    // This would require fetching claim and worker details
                    // For now, just log
                }
            } catch (Exception e) {
                log.error("Error re-attempting payout {}: {}", payout.getPayoutId(), e.getMessage(), e);
            }
        }
    }

    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void updatePayoutStatuses() {
        log.debug("Updating payout statuses from gateways...");
        
        List<Payout> processingPayouts = payoutRepository.findPayoutsNeedingStatusCheck(
                LocalDateTime.now().minusMinutes(30)
        );

        for (Payout payout : processingPayouts) {
            try {
                checkPayoutStatus(payout.getPayoutId());
            } catch (Exception e) {
                log.error("Error updating status for payout {}: {}", payout.getPayoutId(), e.getMessage(), e);
            }
        }
    }

    // Public query methods
    public List<Payout> getWorkerPayouts(String workerId) {
        return payoutRepository.findByWorkerIdOrderByCreatedAtDesc(workerId);
    }

    public List<Payout> getClaimPayouts(String claimId) {
        return payoutRepository.findByClaimIdOrderByCreatedAtDesc(claimId);
    }

    public Payout getPayout(String payoutId) {
        return payoutRepository.findByPayoutId(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
    }

    private String generatePayoutId(String workerId) {
        long timestamp = System.currentTimeMillis() / 1000;
        int sequence = (int) (System.currentTimeMillis() % 1000);
        return String.format("PYT-%s-%d-%03d", workerId, timestamp, sequence);
    }

    private boolean shouldAutoApprove(Claim claim, Worker worker) {
        // Auto-approve if amount is below threshold and worker has good history
        if (claim.getPayoutAmount().compareTo(autoApproveThreshold) <= 0) {
            // Check worker's recent payout history
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            long recentCompletedPayouts = payoutRepository.countCompletedPayoutsForWorkerSince(
                    worker.getWorkerId(), oneMonthAgo
            );
            long recentFailedPayouts = payoutRepository.countFailedPayoutsSince(oneMonthAgo);
            
            // Auto-approve if good success rate
            return recentCompletedPayouts >= 3 && 
                   (recentFailedPayouts == 0 || 
                    (double) recentFailedPayouts / (recentCompletedPayouts + recentFailedPayouts) < 0.1);
        }
        
        return false;
    }
}

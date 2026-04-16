package com.devtrails.backend.payout.gateway;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Transfer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayPayoutGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPayoutGateway.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getGatewayName() {
        return "RAZORPAY";
    }

    @Override
    public PayoutResult initiatePayout(PayoutRequest request) {
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create fund account for the beneficiary
            String fundAccountId = createFundAccount(razorpay, request);

            // Create payout
            Map<String, Object> payoutParams = new HashMap<>();
            payoutParams.put("account_number",
                    request.getUpiId() != null ? request.getUpiId() : request.getAccountNumber());
            payoutParams.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()); // Convert to
                                                                                                           // paise
            payoutParams.put("currency", "INR");
            payoutParams.put("mode", request.getUpiId() != null ? "UPI" : "IMPS");
            payoutParams.put("fund_account_id", fundAccountId);
            payoutParams.put("purpose", "payout");
            payoutParams.put("queue_if_low_balance", true);
            payoutParams.put("reference_id", request.getReferenceId());
            payoutParams.put("narration", "DevTrails Claim Payout - " + request.getClaimId());

            // Create payout using Razorpay's API
            com.razorpay.Payout payout = razorpay.payouts.create(payoutParams);

            log.info("Razorpay payout initiated successfully: {} for amount: {}",
                    payout.get("id"), request.getAmount());

            return PayoutResult.builder()
                    .success(true)
                    .transactionId(payout.get("id").toString())
                    .status(mapRazorpayStatus(payout.get("status").toString()))
                    .message("Payout initiated successfully")
                    .gatewayResponse(payout.toString())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay payout failed: {}", e.getMessage(), e);
            return PayoutResult.builder()
                    .success(false)
                    .transactionId(null)
                    .status("FAILED")
                    .message("Razorpay error: " + e.getMessage())
                    .gatewayResponse(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay payout: {}", e.getMessage(), e);
            return PayoutResult.builder()
                    .success(false)
                    .transactionId(null)
                    .status("FAILED")
                    .message("System error: " + e.getMessage())
                    .gatewayResponse(e.getMessage())
                    .build();
        }
    }

    @Override
    public PayoutStatus checkPayoutStatus(String transactionId) {
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            com.razorpay.Payout payout = razorpay.payouts.fetch(transactionId);

            return PayoutStatus.builder()
                    .transactionId(transactionId)
                    .status(mapRazorpayStatus(payout.get("status").toString()))
                    .amount(BigDecimal.valueOf(Double.parseDouble(payout.get("amount").toString()) / 100.0))
                    .gatewayResponse(payout.toString())
                    .build();

        } catch (RazorpayException e) {
            log.error("Error checking Razorpay payout status: {}", e.getMessage(), e);
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

    private String createFundAccount(RazorpayClient razorpay, PayoutRequest request) throws RazorpayException {
        Map<String, Object> fundAccountParams = new HashMap<>();

        if (request.getUpiId() != null) {
            // Create UPI fund account
            fundAccountParams.put("customer_id", request.getWorkerId());
            fundAccountParams.put("account_type", "vpa");
            fundAccountParams.put("vpa", Map.of(
                    "address", request.getUpiId(),
                    "name", request.getBeneficiaryName()));
        } else {
            // Create bank account fund account
            fundAccountParams.put("customer_id", request.getWorkerId());
            fundAccountParams.put("account_type", "bank_account");
            fundAccountParams.put("bank_account", Map.of(
                    "name", request.getBeneficiaryName(),
                    "account_number", request.getAccountNumber(),
                    "ifsc", request.getIfscCode()));
        }

        com.razorpay.FundAccount fundAccount = razorpay.fundAccounts.create(fundAccountParams);
        return fundAccount.get("id").toString();
    }

    private String mapRazorpayStatus(String razorpayStatus) {
        return switch (razorpayStatus.toLowerCase()) {
            case "queued" -> "PENDING";
            case "processing" -> "PROCESSING";
            case "processed" -> "COMPLETED";
            case "reversed" -> "REFUNDED";
            case "failed" -> "FAILED";
            default -> "UNKNOWN";
        };
    }

    @Override
    public RefundResult refundPayout(String transactionId, BigDecimal amount, String reason) {
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            Map<String, Object> refundParams = new HashMap<>();
            refundParams.put("payout_id", transactionId);
            refundParams.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue()); // Convert to paise
            refundParams.put("notes", Map.of("reason", reason));

            // Note: Razorpay doesn't have direct refund for payouts,
            // this would typically be handled through their support
            log.warn("Razorpay refund requested for payout {}: {} - {}", transactionId, amount, reason);

            return RefundResult.builder()
                    .success(false)
                    .message("Refunds for payouts must be processed through Razorpay support")
                    .build();

        } catch (Exception e) {
            log.error("Error processing Razorpay refund: {}", e.getMessage(), e);
            return RefundResult.builder()
                    .success(false)
                    .message("Refund failed: " + e.getMessage())
                    .build();
        }
    }
}

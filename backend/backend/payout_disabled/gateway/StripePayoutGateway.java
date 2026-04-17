/* package com.devtrails.backend.payout.gateway;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.TransferCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripePayoutGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePayoutGateway.class);

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Override
    public String getGatewayName() {
        return "STRIPE";
    }

    @Override
    public PayoutResult initiatePayout(PayoutRequest request) {
        try {
            Stripe.apiKey = stripeSecretKey;

            // Create or retrieve connected account for the worker
            String accountId = createConnectedAccount(request);

            // Create transfer to connected account
            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                    .setCurrency("inr")
                    .setDestination(accountId)
                    .setTransferGroup("devtrails-payout-" + request.getClaimId())
                    .putAllMetadata(Map.of(
                            "claim_id", request.getClaimId(),
                            "worker_id", request.getWorkerId(),
                            "reference_id", request.getReferenceId(),
                            "beneficiary_name", request.getBeneficiaryName()))
                    .build();

            Transfer transfer = Transfer.create(params);

            log.info("Stripe payout initiated successfully: {} for amount: {}",
                    transfer.getId(), request.getAmount());

            return PayoutResult.builder()
                    .success(true)
                    .transactionId(transfer.getId())
                    .status(mapStripeStatus(transfer.getStatus()))
                    .message("Payout initiated successfully")
                    .gatewayResponse(transfer.toJson())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payout failed: {}", e.getMessage(), e);
            return PayoutResult.builder()
                    .success(false)
                    .transactionId(null)
                    .status("FAILED")
                    .message("Stripe error: " + e.getMessage())
                    .gatewayResponse(e.getStripeError() != null ? e.getStripeError().toString() : e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Unexpected error during Stripe payout: {}", e.getMessage(), e);
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
            Stripe.apiKey = stripeSecretKey;
            Transfer transfer = Transfer.retrieve(transactionId);

            return PayoutStatus.builder()
                    .transactionId(transactionId)
                    .status(mapStripeStatus(transfer.getStatus()))
                    .amount(BigDecimal.valueOf(transfer.getAmount()).divide(BigDecimal.valueOf(100.0)))
                    .gatewayResponse(transfer.toJson())
                    .build();

        } catch (StripeException e) {
            log.error("Error checking Stripe payout status: {}", e.getMessage(), e);
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
            // In production, implement actual webhook signature validation using Stripe's
            // Webhook.constructEvent
            // For demo, we'll simulate validation
            return payload != null && !payload.isEmpty() && signature != null;
        } catch (Exception e) {
            log.error("Webhook signature validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public RefundResult refundPayout(String transactionId, BigDecimal amount, String reason) {
        try {
            Stripe.apiKey = stripeSecretKey;

            // Note: Stripe transfers can be reversed but not refunded in the traditional
            // sense
            // This would typically be handled through their support or by creating a
            // reversal
            Transfer transfer = Transfer.retrieve(transactionId);

            if ("paid".equals(transfer.getStatus())) {
                // Create a reversal for the transfer
                TransferCreateParams reversalParams = TransferCreateParams.builder()
                        .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                        .setDestination(transfer.getDestination())
                        .setTransferGroup("reversal-" + transfer.getTransferGroup())
                        .putAllMetadata(Map.of(
                                "original_transfer_id", transactionId,
                                "reversal_reason", reason))
                        .build();

                Transfer reversal = Transfer.create(reversalParams);

                log.info("Stripe reversal created: {} for original transfer: {}",
                        reversal.getId(), transactionId);

                return RefundResult.builder()
                        .success(true)
                        .refundId(reversal.getId())
                        .message("Reversal created successfully")
                        .gatewayResponse(reversal.toJson())
                        .build();
            } else {
                return RefundResult.builder()
                        .success(false)
                        .message("Cannot reverse transfer in status: " + transfer.getStatus())
                        .build();
            }

        } catch (StripeException e) {
            log.error("Error processing Stripe reversal: {}", e.getMessage(), e);
            return RefundResult.builder()
                    .success(false)
                    .message("Reversal failed: " + e.getMessage())
                    .build();
        }
    }

    private String createConnectedAccount(PayoutRequest request) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Check if account already exists for this worker
        // In production, you would store the account ID in your database
        String accountId = "acct_" + request.getWorkerId(); // Simplified for demo

        try {
            // Try to retrieve existing account
            Account.retrieve(accountId);
            return accountId;
        } catch (StripeException e) {
            // Account doesn't exist, create new one
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.CUSTOM)
                    .setCountry("IN")
                    .setEmail(request.getWorkerId() + "@devtrails.com") // Simplified email
                    .setBusinessType(AccountCreateParams.BusinessType.INDIVIDUAL)
                    .setCapabilities(Map.of(
                            "transfers", "active"))
                    .putAllIndividual(Map.of(
                            "email", request.getWorkerId() + "@devtrails.com",
                            "first_name", extractFirstName(request.getBeneficiaryName()),
                            "last_name", extractLastName(request.getBeneficiaryName())))
                    .build();

            Account account = Account.create(params);

            // Create account link for onboarding (in production, you'd send this to the
            // user)
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(account.getId())
                    .setRefreshUrl("https://devtrails.com/reauth")
                    .setReturnUrl("https://devtrails.com/return")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(linkParams);
            log.info("Stripe account created: {} with onboarding link: {}",
                    account.getId(), accountLink.getUrl());

            return account.getId();
        }
    }

    private String mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus.toLowerCase()) {
            case "pending" -> "PENDING";
            case "in_transit" -> "PROCESSING";
            case "paid" -> "COMPLETED";
            case "failed" -> "FAILED";
            case "canceled" -> "REFUNDED";
            case "reversed" -> "REFUNDED";
            default -> "UNKNOWN";
        };
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Worker";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "User";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)) : "User";
    }
}
 */
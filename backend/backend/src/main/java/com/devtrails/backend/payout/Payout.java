package com.devtrails.backend.payout;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts")
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique payout ID — format: PYT-WORKID-TIMESTAMP-SEQ
    @Column(name = "payout_id", unique = true, nullable = false, length = 60)
    private String payoutId;

    @Column(name = "claim_id", nullable = false, length = 60)
    private String claimId;

    @Column(name = "worker_id", nullable = false, length = 20)
    private String workerId;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod; // RAZORPAY, STRIPE, UPI

    @Column(name = "payment_gateway_id", length = 100)
    private String paymentGatewayId; // Transaction ID from payment gateway

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "upi_id", length = 50)
    private String upiId;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "beneficiary_name", length = 100)
    private String beneficiaryName;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "webhook_received")
    private Boolean webhookReceived = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Payout() {}

    public Payout(Long id, String payoutId, String claimId, String workerId, String paymentMethod, String paymentGatewayId, BigDecimal amount, String currency, String status, String failureReason, String upiId, String accountNumber, String ifscCode, String beneficiaryName, String gatewayResponse, Boolean webhookReceived, LocalDateTime processedAt, LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.payoutId = payoutId;
        this.claimId = claimId;
        this.workerId = workerId;
        this.paymentMethod = paymentMethod;
        this.paymentGatewayId = paymentGatewayId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.failureReason = failureReason;
        this.upiId = upiId;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.beneficiaryName = beneficiaryName;
        this.gatewayResponse = gatewayResponse;
        this.webhookReceived = webhookReceived;
        this.processedAt = processedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public void markAsProcessing() {
        this.status = "PROCESSING";
        this.processedAt = LocalDateTime.now();
    }

    public void markAsCompleted(String gatewayResponse) {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
        this.gatewayResponse = gatewayResponse;
    }

    public void markAsFailed(String reason, String gatewayResponse) {
        this.status = "FAILED";
        this.failureReason = reason;
        this.gatewayResponse = gatewayResponse;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsRefunded(String reason) {
        this.status = "REFUNDED";
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }

    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    public boolean isProcessing() {
        return "PROCESSING".equals(this.status);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public Boolean getWebhookReceived() { return webhookReceived; }
    public void setWebhookReceived(Boolean webhookReceived) { this.webhookReceived = webhookReceived; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

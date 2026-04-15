package com.devtrails.backend.payout;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique payout ID — format: PYT-WORKID-TIMESTAMP-SEQ
    // e.g. PYT-GS3F4A8B-1710758400-001
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
}

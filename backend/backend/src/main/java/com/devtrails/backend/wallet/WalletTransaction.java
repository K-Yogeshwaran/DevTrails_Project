package com.devtrails.backend.wallet;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false, length = 20)
    private String workerId;

    // "credit" or "debit"
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    // Human-readable description shown in UI
    @Column(name = "description", length = 200)
    private String description;

    // Links to policy number, claim ID etc.
    @Column(name = "reference_id", length = 60)
    private String referenceId;

    // "premium_debit" | "claim_credit" | "initial_credit" | "auto_renewal"
    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static WalletTransaction credit(String workerId, BigDecimal amount,
            BigDecimal balanceAfter, String description, String referenceId, String category) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWorkerId(workerId);
        tx.setType("credit");
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(description);
        tx.setReferenceId(referenceId);
        tx.setCategory(category);
        return tx;
    }

    public static WalletTransaction debit(String workerId, BigDecimal amount,
            BigDecimal balanceAfter, String description, String referenceId, String category) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWorkerId(workerId);
        tx.setType("debit");
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(description);
        tx.setReferenceId(referenceId);
        tx.setCategory(category);
        return tx;
    }
}

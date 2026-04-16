package com.devtrails.backend.wallet;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", unique = true, nullable = false, length = 20)
    private String workerId;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_credited", precision = 10, scale = 2)
    private BigDecimal totalCredited = BigDecimal.ZERO;

    @Column(name = "total_debited", precision = 10, scale = 2)
    private BigDecimal totalDebited = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Wallet() {}

    public Wallet(Long id, String workerId, BigDecimal balance, BigDecimal totalCredited, BigDecimal totalDebited, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.workerId = workerId;
        this.balance = balance;
        this.totalCredited = totalCredited;
        this.totalDebited = totalDebited;
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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getTotalCredited() { return totalCredited; }
    public void setTotalCredited(BigDecimal totalCredited) { this.totalCredited = totalCredited; }
    public BigDecimal getTotalDebited() { return totalDebited; }
    public void setTotalDebited(BigDecimal totalDebited) { this.totalDebited = totalDebited; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

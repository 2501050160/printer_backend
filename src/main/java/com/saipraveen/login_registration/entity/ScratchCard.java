package com.saipraveen.login_registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scratch_cards")
public class ScratchCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String orderId;
    private Double transactionAmount;
    private Double maxWinAmount;
    private Double winAmount;
    private boolean isScratched = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime scratchedAt;

    public ScratchCard() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(Double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public Double getMaxWinAmount() {
        return maxWinAmount;
    }

    public void setMaxWinAmount(Double maxWinAmount) {
        this.maxWinAmount = maxWinAmount;
    }

    public Double getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(Double winAmount) {
        this.winAmount = winAmount;
    }

    public boolean isScratched() {
        return isScratched;
    }

    public void setScratched(boolean scratched) {
        isScratched = scratched;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getScratchedAt() {
        return scratchedAt;
    }

    public void setScratchedAt(LocalDateTime scratchedAt) {
        this.scratchedAt = scratchedAt;
    }
}

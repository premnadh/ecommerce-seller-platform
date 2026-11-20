package com.premnadh.sellerplatform.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private PaymentOperation operation;

    @Column(nullable = false, length = 30, updatable = false)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        nullable = false,
        length = 3,
        columnDefinition = "CHAR(3)",
        updatable = false
    )
    private String currency;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(
        UUID sellerId,
        UUID orderId,
        PaymentOperation operation,
        BigDecimal amount,
        String currency
    ) {
        this.id = UUID.randomUUID();

        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID is required");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }

        if (operation == null) {
            throw new IllegalArgumentException("Payment operation is required");
        }

        if (amount == null || amount.signum() < 0 || amount.scale() > 2) {
            throw new IllegalArgumentException(
                "Payment amount must be non-negative and have at most two decimal places"
            );
        }

        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                "Currency must be a three-letter uppercase code"
            );
        }

        this.sellerId = sellerId;
        this.orderId = orderId;
        this.operation = operation;
        this.provider = "MOCK";
        this.status = PaymentStatus.INITIATED;
        this.amount = amount;
        this.currency = currency;
    }

    public void markSucceeded(String providerReference) {
        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException(
                "Only initiated payment attempts can succeed"
            );
        }

        if (providerReference == null || providerReference.isBlank()
            || providerReference.length() > 100) {
            throw new IllegalArgumentException(
                "Provider reference must be between 1 and 100 characters"
            );
        }

        this.status = PaymentStatus.SUCCEEDED;
        this.providerReference = providerReference.trim();
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        if (status != PaymentStatus.INITIATED) {
            throw new IllegalStateException(
                "Only initiated payment attempts can fail"
            );
        }

        if (reason == null || reason.isBlank() || reason.length() > 255) {
            throw new IllegalArgumentException(
                "Failure reason must be between 1 and 255 characters"
            );
        }

        this.status = PaymentStatus.FAILED;
        this.failureReason = reason.trim();
        this.providerReference = null;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public PaymentOperation getOperation() {
        return operation;
    }

    public String getProvider() {
        return provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

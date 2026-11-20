package com.premnadh.sellerplatform.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "orders",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_orders_seller_id_id",
            columnNames = {"seller_id", "id"}
        ),
        @UniqueConstraint(
            name = "uq_orders_seller_order_number",
            columnNames = {"seller_id", "order_number"}
        ),
        @UniqueConstraint(
            name = "uq_orders_seller_idempotency_key",
            columnNames = {"seller_id", "idempotency_key"}
        )
    }
)
public class Order {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "order_number", nullable = false, length = 40, updatable = false)
    private String orderNumber;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(
        UUID sellerId,
        String orderNumber,
        UUID idempotencyKey,
        String customerName,
        String customerEmail,
        String currency,
        BigDecimal subtotal,
        BigDecimal total
    ) {
        this.id = UUID.randomUUID();
        this.sellerId = Objects.requireNonNull(sellerId, "Seller ID is required");
        this.idempotencyKey = Objects.requireNonNull(
            idempotencyKey,
            "Idempotency key is required"
        );

        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number is required");
        }

        if (orderNumber.length() > 40) {
            throw new IllegalArgumentException(
                "Order number must not exceed 40 characters"
            );
        }

        if (customerName == null || customerName.isBlank()
            || customerName.trim().length() > 150) {
            throw new IllegalArgumentException(
                "Customer name must be between 1 and 150 characters"
            );
        }

        // Normalise the email before validating it.
        String normalisedEmail = customerEmail == null
            ? null
            : customerEmail.trim().toLowerCase(Locale.ROOT);

        if (normalisedEmail == null
            || normalisedEmail.isBlank()
            || normalisedEmail.length() > 254
            || !normalisedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("A valid customer email is required");
        }

        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                "Currency must be a three-letter uppercase code"
            );
        }

        validateAmount(subtotal, "Subtotal");
        validateAmount(total, "Total");

        if (subtotal.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                "Subtotal and total must match when no additional charges are applied"
            );
        }

        this.orderNumber = orderNumber;
        this.customerName = customerName.trim();
        this.customerEmail = normalisedEmail;
        this.currency = currency;
        this.subtotal = subtotal;
        this.total = total;
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    private void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2) {
            throw new IllegalArgumentException(
                fieldName + " must be non-negative and have at most two decimal places"
            );
        }
    }

    public void confirm() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                "Only pending-payment orders can be confirmed"
            );
        }

        this.status = OrderStatus.CONFIRMED;
    }

    public void markPaymentFailed() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                "Only pending-payment orders can be marked as payment failed"
            );
        }

        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void cancel() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Only confirmed orders can be cancelled"
            );
        }

        this.status = OrderStatus.CANCELLED;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

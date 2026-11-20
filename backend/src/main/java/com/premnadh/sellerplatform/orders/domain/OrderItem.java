package com.premnadh.sellerplatform.orders.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "order_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_order_items_order_product",
            columnNames = {"order_id", "product_id"}
        )
    }
)
public class OrderItem {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false, length = 64, updatable = false)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 200, updatable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItem() {
    }

    public OrderItem(
            UUID sellerId,
            UUID orderId,
            UUID productId,
            String sku,
            String productName,
            BigDecimal unitPrice,
            int quantity
    ) {
        if (sellerId == null || orderId == null || productId == null) {
            throw new IllegalArgumentException(
                "Seller, order and product IDs are required"
            );
        }

        if (sku == null || sku.isBlank() || sku.length() > 64) {
            throw new IllegalArgumentException(
                "A valid product SKU is required"
            );
        }

        if (productName == null || productName.isBlank()
                || productName.length() > 200) {
            throw new IllegalArgumentException(
                "A valid product name is required"
            );
        }

        if (unitPrice == null
                || unitPrice.signum() < 0
                || unitPrice.scale() > 2) {
            throw new IllegalArgumentException(
                "Unit price must be a non-negative monetary amount"
            );
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                "Order item quantity must be positive"
            );
        }

        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.orderId = orderId;
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
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

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

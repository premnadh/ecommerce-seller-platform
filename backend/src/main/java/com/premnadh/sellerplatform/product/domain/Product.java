package com.premnadh.sellerplatform.product.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(
            UUID sellerId,
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency
    ) {
        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.status = ProductStatus.DRAFT;
    }

    public void updateDetails(
            String name,
            String description,
            BigDecimal price,
            String currency
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
    }

    public void changeStatus(ProductStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException(
                "Product status cannot be null"
            );
        }

        if (this.status == ProductStatus.ARCHIVED
                && newStatus != ProductStatus.ARCHIVED) {
            throw new IllegalStateException(
                "Archived products cannot be reactivated"
            );
        }

        this.status = newStatus;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

package com.premnadh.sellerplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "inventory_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_inventory_product",
            columnNames = "product_id"
        )
    }
)
public class InventoryItem {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(
            UUID sellerId,
            UUID productId,
            int initialQuantity,
            int reorderLevel
    ) {
        if (sellerId == null || productId == null) {
            throw new IllegalArgumentException(
                "Seller ID and product ID are required"
            );
        }

        if (initialQuantity < 0 || reorderLevel < 0) {
            throw new IllegalArgumentException(
                "Stock quantities and reorder level cannot be negative"
            );
        }

        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.productId = productId;
        this.quantityOnHand = initialQuantity;
        this.quantityReserved = 0;
        this.reorderLevel = reorderLevel;
    }

    public void adjustStock(int quantityDelta) {
        if (quantityDelta == 0) {
            throw new IllegalArgumentException(
                "Stock adjustment cannot be zero"
            );
        }

        final int updatedQuantity;

        try {
            updatedQuantity = Math.addExact(
                quantityOnHand,
                quantityDelta
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                "Stock adjustment exceeds supported quantity limits",
                exception
            );
        }

        if (updatedQuantity < quantityReserved) {
            throw new IllegalStateException(
                "Stock cannot be reduced below reserved quantity"
            );
        }

        this.quantityOnHand = updatedQuantity;
    }

    public int getAvailableQuantity() {
        return quantityOnHand - quantityReserved;
    }

    public boolean isLowStock() {
        return getAvailableQuantity() <= reorderLevel;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
}
package com.premnadh.sellerplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30, updatable = false)
    private InventoryMovementType movementType;

    @Column(name = "quantity_on_hand_delta", nullable = false, updatable = false)
    private int quantityOnHandDelta;

    @Column(name = "quantity_reserved_delta", nullable = false, updatable = false)
    private int quantityReservedDelta;

    @Column(length = 255, updatable = false)
    private String reason;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryMovement() {
    }

    public InventoryMovement(
            UUID sellerId,
            UUID productId,
            InventoryMovementType movementType,
            int quantityOnHandDelta,
            int quantityReservedDelta,
            String reason,
            UUID referenceId
    ) {
        if (sellerId == null || productId == null || movementType == null) {
            throw new IllegalArgumentException(
                    "Seller, product and movement type are required"
            );
        }

        if (quantityOnHandDelta == 0 && quantityReservedDelta == 0) {
            throw new IllegalArgumentException(
                    "An inventory movement must change a quantity"
            );
        }

        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.productId = productId;
        this.movementType = movementType;
        this.quantityOnHandDelta = quantityOnHandDelta;
        this.quantityReservedDelta = quantityReservedDelta;
        this.reason = reason;
        this.referenceId = referenceId;
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

    public UUID getProductId() {
        return productId;
    }

    public InventoryMovementType getMovementType() {
        return movementType;
    }

    public int getQuantityOnHandDelta() {
        return quantityOnHandDelta;
    }

    public int getQuantityReservedDelta() {
        return quantityReservedDelta;
    }

    public String getReason() {
        return reason;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

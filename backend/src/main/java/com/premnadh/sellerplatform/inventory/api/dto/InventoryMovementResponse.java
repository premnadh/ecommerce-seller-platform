package com.premnadh.sellerplatform.inventory.api.dto;

import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovementType;

import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID sellerId,
        UUID productId,
        InventoryMovementType movementType,
        int quantityOnHandDelta,
        int quantityReservedDelta,
        String reason,
        UUID referenceId,
        Instant createdAt
) {

    public static InventoryMovementResponse from(
            InventoryMovement movement
    ) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getSellerId(),
                movement.getProductId(),
                movement.getMovementType(),
                movement.getQuantityOnHandDelta(),
                movement.getQuantityReservedDelta(),
                movement.getReason(),
                movement.getReferenceId(),
                movement.getCreatedAt()
        );
    }
}

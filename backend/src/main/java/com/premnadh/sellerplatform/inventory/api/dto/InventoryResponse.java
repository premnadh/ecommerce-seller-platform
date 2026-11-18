package com.premnadh.sellerplatform.inventory.api.dto;

import com.premnadh.sellerplatform.inventory.domain.InventoryItem;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID sellerId,
        UUID productId,
        int quantityOnHand,
        int quantityReserved,
        int availableQuantity,
        int reorderLevel,
        boolean lowStock,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    public static InventoryResponse from(InventoryItem inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getSellerId(),
                inventory.getProductId(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getAvailableQuantity(),
                inventory.getReorderLevel(),
                inventory.isLowStock(),
                inventory.getVersion(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}

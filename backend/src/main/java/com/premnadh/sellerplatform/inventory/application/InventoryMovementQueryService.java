package com.premnadh.sellerplatform.inventory.application;

import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.repository.InventoryItemRepository;
import com.premnadh.sellerplatform.inventory.repository.InventoryMovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryMovementQueryService {

    private final InventoryItemRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;

    public InventoryMovementQueryService(
            InventoryItemRepository inventoryRepository,
            InventoryMovementRepository movementRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> getProductMovements(
            UUID sellerId,
            UUID productId
    ) {
        inventoryRepository
                .findBySellerIdAndProductId(sellerId, productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found"
                ));

        return movementRepository
                .findAllBySellerIdAndProductIdOrderByCreatedAtDesc(
                        sellerId,
                        productId
                );
    }
}

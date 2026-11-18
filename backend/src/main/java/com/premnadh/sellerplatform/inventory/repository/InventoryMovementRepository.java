package com.premnadh.sellerplatform.inventory.repository;

import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovementType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, UUID> {

    List<InventoryMovement> findAllBySellerIdAndProductIdOrderByCreatedAtDesc(
            UUID sellerId,
            UUID productId
    );

    List<InventoryMovement> findAllBySellerIdOrderByCreatedAtDesc(
            UUID sellerId
    );

    Optional<InventoryMovement>
    findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
            UUID sellerId,
            UUID productId,
            InventoryMovementType movementType,
            UUID referenceId
    );
}

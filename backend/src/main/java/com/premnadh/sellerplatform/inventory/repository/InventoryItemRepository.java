package com.premnadh.sellerplatform.inventory.repository;

import com.premnadh.sellerplatform.inventory.domain.InventoryItem;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository
        extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findBySellerIdAndProductId(
            UUID sellerId,
            UUID productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT inventory
            FROM InventoryItem inventory
            WHERE inventory.sellerId = :sellerId
              AND inventory.productId = :productId
            """)
    Optional<InventoryItem> findBySellerIdAndProductIdForUpdate(
            @Param("sellerId") UUID sellerId,
            @Param("productId") UUID productId
    );

    List<InventoryItem> findAllBySellerId(UUID sellerId);

    @Query("""
            SELECT inventory
            FROM InventoryItem inventory
            WHERE inventory.sellerId = :sellerId
              AND inventory.quantityOnHand - inventory.quantityReserved
                    <= inventory.reorderLevel
            """)
    List<InventoryItem> findLowStockBySellerId(
            @Param("sellerId") UUID sellerId
    );

    boolean existsByProductId(UUID productId);

    boolean existsBySellerIdAndProductId(
            UUID sellerId,
            UUID productId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE InventoryItem inventory
            SET inventory.quantityReserved =
                    inventory.quantityReserved + :quantity,
                inventory.version = inventory.version + 1,
                inventory.updatedAt = CURRENT_TIMESTAMP
            WHERE inventory.sellerId = :sellerId
              AND inventory.productId = :productId
              AND :quantity > 0
              AND inventory.quantityOnHand - inventory.quantityReserved
                    >= :quantity
            """)
    int reserveAvailableStock(
            @Param("sellerId") UUID sellerId,
            @Param("productId") UUID productId,
            @Param("quantity") int quantity
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE InventoryItem inventory
            SET inventory.quantityReserved =
                    inventory.quantityReserved - :quantity,
                inventory.version = inventory.version + 1,
                inventory.updatedAt = CURRENT_TIMESTAMP
            WHERE inventory.sellerId = :sellerId
              AND inventory.productId = :productId
              AND :quantity > 0
              AND inventory.quantityReserved >= :quantity
            """)
    int releaseReservedStock(
            @Param("sellerId") UUID sellerId,
            @Param("productId") UUID productId,
            @Param("quantity") int quantity
    );
}

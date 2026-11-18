package com.premnadh.sellerplatform.inventory.application;

import com.premnadh.sellerplatform.inventory.domain.InventoryItem;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovementType;
import com.premnadh.sellerplatform.inventory.repository.InventoryItemRepository;
import com.premnadh.sellerplatform.inventory.repository.InventoryMovementRepository;
import com.premnadh.sellerplatform.product.application.ProductNotFoundException;
import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;

    public InventoryService(
            InventoryItemRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            ProductRepository productRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public InventoryItem initialiseInventory(
            UUID sellerId,
            UUID productId,
            int initialQuantity,
            int reorderLevel
    ) {
        Product product = productRepository
                .findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found"
                ));

        if (inventoryRepository.existsByProductId(productId)) {
            throw new IllegalStateException(
                    "Inventory already exists for this product"
            );
        }

        InventoryItem inventoryItem = new InventoryItem(
                sellerId,
                product.getId(),
                initialQuantity,
                reorderLevel
        );

        InventoryItem savedInventory =
                inventoryRepository.save(inventoryItem);

        if (initialQuantity > 0) {
            InventoryMovement movement = new InventoryMovement(
                    sellerId,
                    productId,
                    InventoryMovementType.INITIAL_STOCK,
                    initialQuantity,
                    0,
                    "Initial stock entry",
                    null
            );

            movementRepository.save(movement);
        }

        return savedInventory;
    }

    @Transactional
    public InventoryItem adjustStock(
            UUID sellerId,
            UUID productId,
            int quantityDelta,
            String reason
    ) {
        InventoryItem inventoryItem = inventoryRepository
                .findBySellerIdAndProductId(sellerId, productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found"
                ));

        inventoryItem.adjustStock(quantityDelta);

        InventoryItem savedInventory =
                inventoryRepository.save(inventoryItem);

        InventoryMovement movement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.STOCK_ADJUSTMENT,
                quantityDelta,
                0,
                reason,
                null
        );

        movementRepository.save(movement);

        return savedInventory;
    }

    @Transactional
    public InventoryItem reserveStock(
            UUID sellerId,
            UUID productId,
            int quantity,
            UUID orderReference
    ) {
        validatePositiveQuantity(quantity);
        validateOrderReference(orderReference);

        InventoryItem inventoryItem = inventoryRepository
                .findBySellerIdAndProductIdForUpdate(sellerId, productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found"
                ));

        var existingMovement = movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RESERVATION,
                        orderReference
                );

        if (existingMovement.isPresent()) {
            int originalQuantity =
                    existingMovement.get().getQuantityReservedDelta();

            if (originalQuantity != quantity) {
                throw new IllegalArgumentException(
                        "Order reference has already been used "
                                + "with a different reservation quantity"
                );
            }

            return inventoryItem;
        }

        int updatedRows = inventoryRepository.reserveAvailableStock(
                sellerId,
                productId,
                quantity
        );

        if (updatedRows == 0) {
            throw new IllegalStateException(
                    "Insufficient stock"
            );
        }

        InventoryMovement movement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.RESERVATION,
                0,
                quantity,
                "Stock reserved for order",
                orderReference
        );

        movementRepository.save(movement);

        return getInventory(sellerId, productId);
    }

    @Transactional
    public InventoryItem releaseStock(
            UUID sellerId,
            UUID productId,
            int quantity,
            UUID orderReference
    ) {
        validatePositiveQuantity(quantity);
        validateOrderReference(orderReference);

        InventoryItem inventoryItem = inventoryRepository
                .findBySellerIdAndProductIdForUpdate(sellerId, productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found"
                ));

        var existingMovement = movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RELEASE,
                        orderReference
                );

        if (existingMovement.isPresent()) {
            int originalQuantity =
                    -existingMovement.get().getQuantityReservedDelta();

            if (originalQuantity != quantity) {
                throw new IllegalArgumentException(
                        "Order reference has already been used "
                                + "with a different release quantity"
                );
            }

            return inventoryItem;
        }

        int updatedRows = inventoryRepository.releaseReservedStock(
                sellerId,
                productId,
                quantity
        );

        if (updatedRows == 0) {
            throw new IllegalStateException(
                    "Insufficient reserved stock"
            );
        }

        InventoryMovement movement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.RELEASE,
                0,
                -quantity,
                "Reserved stock released",
                orderReference
        );

        movementRepository.save(movement);

        return getInventory(sellerId, productId);
    }

    @Transactional(readOnly = true)
    public InventoryItem getInventory(
            UUID sellerId,
            UUID productId
    ) {
        return inventoryRepository
                .findBySellerIdAndProductId(sellerId, productId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found"
                ));
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getSellerInventory(UUID sellerId) {
        return inventoryRepository.findAllBySellerId(sellerId);
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockInventory(UUID sellerId) {
        return inventoryRepository.findLowStockBySellerId(sellerId);
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
    }

    private void validateOrderReference(UUID orderReference) {
        if (orderReference == null) {
            throw new IllegalArgumentException(
                    "Order reference is required"
            );
        }
    }
}

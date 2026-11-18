package com.premnadh.sellerplatform.inventory.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryItemTest {

    private UUID sellerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void shouldCreateInventoryWithInitialQuantities() {
        InventoryItem inventory = new InventoryItem(
                sellerId,
                productId,
                100,
                10
        );

        assertEquals(100, inventory.getQuantityOnHand());
        assertEquals(0, inventory.getQuantityReserved());
        assertEquals(100, inventory.getAvailableQuantity());
        assertEquals(10, inventory.getReorderLevel());
    }

    @Test
    void shouldRejectNegativeInitialQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryItem(sellerId, productId, -1, 10)
        );
    }

    @Test
    void shouldRejectNegativeReorderLevel() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryItem(sellerId, productId, 100, -1)
        );
    }

    @Test
    void shouldRejectMissingSellerOrProductId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryItem(null, productId, 100, 10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryItem(sellerId, null, 100, 10)
        );
    }

    @Test
    void shouldIncreaseStock() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 100, 10
        );

        inventory.adjustStock(25);

        assertEquals(125, inventory.getQuantityOnHand());
        assertEquals(125, inventory.getAvailableQuantity());
    }

    @Test
    void shouldDecreaseStock() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 100, 10
        );

        inventory.adjustStock(-30);

        assertEquals(70, inventory.getQuantityOnHand());
        assertEquals(70, inventory.getAvailableQuantity());
    }

    @Test
    void shouldRejectZeroStockAdjustment() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 100, 10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> inventory.adjustStock(0)
        );
    }

    @Test
    void shouldRejectAdjustmentThatMakesStockNegative() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 10, 2
        );

        assertThrows(
                IllegalStateException.class,
                () -> inventory.adjustStock(-11)
        );

        assertEquals(10, inventory.getQuantityOnHand());
    }

    @Test
    void shouldRejectIntegerOverflow() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, Integer.MAX_VALUE, 10
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> inventory.adjustStock(1)
        );

        assertEquals(Integer.MAX_VALUE, inventory.getQuantityOnHand());
    }

    @Test
    void shouldIdentifyLowStockUsingReorderLevel() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 10, 10
        );

        assertTrue(inventory.isLowStock());

        inventory.adjustStock(5);

        assertFalse(inventory.isLowStock());
    }
}

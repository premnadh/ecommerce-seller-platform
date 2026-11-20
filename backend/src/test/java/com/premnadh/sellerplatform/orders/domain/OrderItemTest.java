package com.premnadh.sellerplatform.orders.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    private OrderItem createOrderItem() {
        return new OrderItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "KB-001",
            "Gaming Keyboard",
            new BigDecimal("49.99"),
            2
        );
    }

    @Test
    void shouldCreateOrderItemSuccessfully() {
        OrderItem item = createOrderItem();

        assertNotNull(item.getId());
        assertEquals("KB-001", item.getSku());
        assertEquals("Gaming Keyboard", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(
            new BigDecimal("49.99"),
            item.getUnitPrice()
        );
    }

    @Test
    void shouldCalculateLineTotalCorrectly() {
        OrderItem item = createOrderItem();

        assertEquals(
            new BigDecimal("99.98"),
            item.getLineTotal()
        );
    }

    @Test
    void shouldSupportSingleQuantity() {
        OrderItem item = new OrderItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "MOUSE-001",
            "Wireless Mouse",
            new BigDecimal("25.00"),
            1
        );

        assertEquals(
            new BigDecimal("25.00"),
            item.getLineTotal()
        );
    }

    @Test
    void shouldRejectZeroQuantity() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "KB-001",
                "Gaming Keyboard",
                new BigDecimal("49.99"),
                0
            )
        );
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "KB-001",
                "Gaming Keyboard",
                new BigDecimal("49.99"),
                -2
            )
        );
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "KB-001",
                "Gaming Keyboard",
                new BigDecimal("-10.00"),
                1
            )
        );
    }

    @Test
    void shouldRejectMissingProductId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "KB-001",
                "Gaming Keyboard",
                new BigDecimal("49.99"),
                1
            )
        );
    }

    @Test
    void shouldRejectMissingSku() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                " ",
                "Gaming Keyboard",
                new BigDecimal("49.99"),
                1
            )
        );
    }

    @Test
    void shouldRejectMissingProductName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "KB-001",
                " ",
                new BigDecimal("49.99"),
                1
            )
        );
    }

    @Test
    void shouldPreserveOriginalProductPriceSnapshot() {
        BigDecimal originalPrice = new BigDecimal("49.99");

        OrderItem item = new OrderItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "KB-001",
            "Gaming Keyboard",
            originalPrice,
            2
        );

        originalPrice = new BigDecimal("59.99");

        assertEquals(
            new BigDecimal("49.99"),
            item.getUnitPrice()
        );

        assertEquals(
            new BigDecimal("99.98"),
            item.getLineTotal()
        );
    }
}
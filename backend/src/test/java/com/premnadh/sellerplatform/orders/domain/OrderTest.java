package com.premnadh.sellerplatform.orders.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order createOrder() {
        return new Order(
            UUID.randomUUID(),
            "ORD-2026-0001",
            UUID.randomUUID(),
            "Alex Smith",
            "alex@example.com",
            "GBP",
            new BigDecimal("99.99"),
            new BigDecimal("99.99")
        );
    }

    @Test
    void shouldCreateOrderWithPendingPaymentStatus() {
        Order order = createOrder();

        assertNotNull(order.getId());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals("Alex Smith", order.getCustomerName());
        assertEquals("alex@example.com", order.getCustomerEmail());
        assertEquals("GBP", order.getCurrency());
        assertEquals(new BigDecimal("99.99"), order.getTotal());
    }

    @Test
    void shouldNormaliseCustomerEmail() {
        Order order = new Order(
            UUID.randomUUID(),
            "ORD-2026-0002",
            UUID.randomUUID(),
            "Alex Smith",
            " ALEX@EXAMPLE.COM ",
            "GBP",
            new BigDecimal("50.00"),
            new BigDecimal("50.00")
        );

        assertEquals("alex@example.com", order.getCustomerEmail());
    }

    @Test
    void shouldConfirmPendingOrder() {
        Order order = createOrder();

        order.confirm();

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void shouldMarkPendingOrderAsPaymentFailed() {
        Order order = createOrder();

        order.markPaymentFailed();

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
    }

    @Test
    void shouldCancelConfirmedOrder() {
        Order order = createOrder();
        order.confirm();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldRejectCancellationOfPendingOrder() {
        Order order = createOrder();

        assertThrows(
            IllegalStateException.class,
            order::cancel
        );

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }

    @Test
    void shouldRejectConfirmationOfPaymentFailedOrder() {
        Order order = createOrder();
        order.markPaymentFailed();

        assertThrows(
            IllegalStateException.class,
            order::confirm
        );

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
    }

    @Test
    void shouldRejectRepeatedConfirmation() {
        Order order = createOrder();
        order.confirm();

        assertThrows(
            IllegalStateException.class,
            order::confirm
        );
    }

    @Test
    void shouldRejectCancellationOfAlreadyCancelledOrder() {
        Order order = createOrder();
        order.confirm();
        order.cancel();

        assertThrows(
            IllegalStateException.class,
            order::cancel
        );
    }

    @Test
    void shouldRejectNegativeOrderTotal() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                UUID.randomUUID(),
                "ORD-2026-0003",
                UUID.randomUUID(),
                "Alex Smith",
                "alex@example.com",
                "GBP",
                new BigDecimal("-10.00"),
                new BigDecimal("-10.00")
            )
        );
    }

    @Test
    void shouldRejectMismatchedSubtotalAndTotal() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                UUID.randomUUID(),
                "ORD-2026-0004",
                UUID.randomUUID(),
                "Alex Smith",
                "alex@example.com",
                "GBP",
                new BigDecimal("50.00"),
                new BigDecimal("60.00")
            )
        );
    }

    @Test
    void shouldRejectInvalidCurrency() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Order(
                UUID.randomUUID(),
                "ORD-2026-0005",
                UUID.randomUUID(),
                "Alex Smith",
                "alex@example.com",
                "GB",
                new BigDecimal("50.00"),
                new BigDecimal("50.00")
            )
        );
    }
}

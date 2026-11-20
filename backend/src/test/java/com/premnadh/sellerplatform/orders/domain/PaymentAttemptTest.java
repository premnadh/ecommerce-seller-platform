package com.premnadh.sellerplatform.orders.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAttemptTest {

    private PaymentAttempt createPaymentAttempt() {
        return new PaymentAttempt(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentOperation.PAYMENT,
                new BigDecimal("99.99"),
                "GBP"
        );
    }

    @Test
    void shouldCreatePaymentAttemptSuccessfully() {
        PaymentAttempt attempt = createPaymentAttempt();

        assertNotNull(attempt.getId());
        assertEquals(PaymentOperation.PAYMENT, attempt.getOperation());
        assertEquals(PaymentStatus.INITIATED, attempt.getStatus());
        assertEquals("MOCK", attempt.getProvider());
        assertEquals(new BigDecimal("99.99"), attempt.getAmount());
        assertEquals("GBP", attempt.getCurrency());
        assertNull(attempt.getProviderReference());
        assertNull(attempt.getFailureReason());
    }

    @Test
    void shouldMarkPaymentAsSucceeded() {
        PaymentAttempt attempt = createPaymentAttempt();

        attempt.markSucceeded("MOCK-PAY-123");

        assertEquals(PaymentStatus.SUCCEEDED, attempt.getStatus());
        assertEquals("MOCK-PAY-123", attempt.getProviderReference());
        assertNull(attempt.getFailureReason());
    }

    @Test
    void shouldMarkPaymentAsFailed() {
        PaymentAttempt attempt = createPaymentAttempt();

        attempt.markFailed("Insufficient funds");

        assertEquals(PaymentStatus.FAILED, attempt.getStatus());
        assertEquals("Insufficient funds", attempt.getFailureReason());
        assertNull(attempt.getProviderReference());
    }

    @Test
    void shouldRejectSuccessWithoutProviderReference() {
        PaymentAttempt attempt = createPaymentAttempt();

        assertThrows(
                IllegalArgumentException.class,
                () -> attempt.markSucceeded(" ")
        );
    }

    @Test
    void shouldRejectSuccessWithReferenceLongerThan100Characters() {
        PaymentAttempt attempt = createPaymentAttempt();
        String reference = "R".repeat(101);

        assertThrows(
                IllegalArgumentException.class,
                () -> attempt.markSucceeded(reference)
        );
    }

    @Test
    void shouldRejectFailureWithoutReason() {
        PaymentAttempt attempt = createPaymentAttempt();

        assertThrows(
                IllegalArgumentException.class,
                () -> attempt.markFailed(" ")
        );
    }

    @Test
    void shouldRejectFailureReasonLongerThan255Characters() {
        PaymentAttempt attempt = createPaymentAttempt();
        String reason = "F".repeat(256);

        assertThrows(
                IllegalArgumentException.class,
                () -> attempt.markFailed(reason)
        );
    }

    @Test
    void shouldNotAllowCompletingAnAttemptMoreThanOnce() {
        PaymentAttempt attempt = createPaymentAttempt();
        attempt.markSucceeded("MOCK-PAY-123");

        assertThrows(
                IllegalStateException.class,
                () -> attempt.markFailed("Payment declined")
        );
    }

    @Test
    void shouldNotAllowSuccessAfterFailure() {
        PaymentAttempt attempt = createPaymentAttempt();
        attempt.markFailed("Payment declined");

        assertThrows(
                IllegalStateException.class,
                () -> attempt.markSucceeded("MOCK-PAY-456")
        );
    }

    @Test
    void shouldRejectNegativePaymentAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAttempt(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        PaymentOperation.PAYMENT,
                        new BigDecimal("-1.00"),
                        "GBP"
                )
        );
    }

    @Test
    void shouldRejectInvalidCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAttempt(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        PaymentOperation.PAYMENT,
                        new BigDecimal("10.00"),
                        "GB"
                )
        );
    }

    @Test
    void shouldRejectMissingOrderId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAttempt(
                        UUID.randomUUID(),
                        null,
                        PaymentOperation.PAYMENT,
                        new BigDecimal("10.00"),
                        "GBP"
                )
        );
    }
}

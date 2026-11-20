package com.premnadh.sellerplatform.orders.application.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    private final boolean chargeSuccessful;
    private final boolean refundSuccessful;

    public MockPaymentGateway(
        @Value("${app.payments.mock.charge-success:true}")
        boolean chargeSuccessful,

        @Value("${app.payments.mock.refund-success:true}")
        boolean refundSuccessful
    ) {
        this.chargeSuccessful = chargeSuccessful;
        this.refundSuccessful = refundSuccessful;
    }

    @Override
    public PaymentResult charge(
        UUID orderId,
        BigDecimal amount,
        String currency
    ) {
        if (!chargeSuccessful) {
            return PaymentResult.failed(
                "Mock payment was declined"
            );
        }

        return PaymentResult.succeeded(
            "MOCK-PAY-" + UUID.randomUUID()
        );
    }

    @Override
    public PaymentResult refund(
        UUID orderId,
        BigDecimal amount,
        String currency
    ) {
        if (!refundSuccessful) {
            return PaymentResult.failed(
                "Mock refund was declined"
            );
        }

        return PaymentResult.succeeded(
            "MOCK-REFUND-" + UUID.randomUUID()
        );
    }
}

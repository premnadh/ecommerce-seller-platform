package com.premnadh.sellerplatform.orders.application.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    PaymentResult charge(
        UUID orderId,
        BigDecimal amount,
        String currency
    );

    PaymentResult refund(
        UUID orderId,
        BigDecimal amount,
        String currency
    );
}

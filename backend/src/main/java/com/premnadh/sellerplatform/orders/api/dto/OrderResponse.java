package com.premnadh.sellerplatform.orders.api.dto;

import com.premnadh.sellerplatform.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    String customerName,
    String customerEmail,
    OrderStatus status,
    String currency,
    BigDecimal subtotal,
    BigDecimal total,
    List<OrderItemResponse> items,
    List<PaymentAttemptResponse> paymentAttempts,
    Instant createdAt,
    Instant updatedAt
) {
}

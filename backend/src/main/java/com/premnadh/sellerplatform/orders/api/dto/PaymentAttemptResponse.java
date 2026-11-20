package com.premnadh.sellerplatform.orders.api.dto;

import com.premnadh.sellerplatform.orders.domain.PaymentOperation;
import com.premnadh.sellerplatform.orders.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(
    UUID id,
    PaymentOperation operation,
    String provider,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String providerReference,
    String failureReason,
    Instant createdAt
) {
}

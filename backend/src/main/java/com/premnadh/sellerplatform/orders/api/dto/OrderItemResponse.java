package com.premnadh.sellerplatform.orders.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    UUID productId,
    String sku,
    String productName,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal
) {
}

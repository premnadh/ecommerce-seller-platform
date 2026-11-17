package com.premnadh.sellerplatform.product.api.dto;

import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID sellerId,
    String sku,
    String name,
    String description,
    BigDecimal price,
    String currency,
    ProductStatus status,
    Instant createdAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getSellerId(),
            product.getSku(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCurrency(),
            product.getStatus(),
            product.getCreatedAt()
        );
    }
}

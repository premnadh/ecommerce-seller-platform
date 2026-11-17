package com.premnadh.sellerplatform.product.api.dto;

import com.premnadh.sellerplatform.product.domain.ProductStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(

    @NotNull
    ProductStatus status

) {
}

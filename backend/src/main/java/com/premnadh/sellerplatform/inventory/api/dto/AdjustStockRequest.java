package com.premnadh.sellerplatform.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustStockRequest(

        @NotNull
        Integer quantityDelta,

        @NotBlank
        @Size(max = 255)
        String reason

) {}

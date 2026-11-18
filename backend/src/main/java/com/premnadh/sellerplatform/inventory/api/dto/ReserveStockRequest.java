package com.premnadh.sellerplatform.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveStockRequest(

        @NotNull
        @Min(1)
        Integer quantity,

        @NotNull
        UUID orderReference

) {}

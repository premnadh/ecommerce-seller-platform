package com.premnadh.sellerplatform.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(

        @NotNull
        @Min(0)
        Integer initialQuantity,

        @NotNull
        @Min(0)
        Integer reorderLevel

) {}

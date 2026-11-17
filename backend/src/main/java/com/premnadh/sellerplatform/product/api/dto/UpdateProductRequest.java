package com.premnadh.sellerplatform.product.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(

    @NotBlank
    @Size(max = 200)
    String name,

    @Size(max = 5000)
    String description,

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    BigDecimal price,

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    String currency

) {
}

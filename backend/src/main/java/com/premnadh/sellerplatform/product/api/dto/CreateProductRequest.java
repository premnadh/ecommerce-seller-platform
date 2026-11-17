package com.premnadh.sellerplatform.product.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProductRequest(

    @NotBlank
    @Size(max = 64)
    String sku,

    @NotBlank
    @Size(max = 200)
    String name,

    @Size(max = 5000)
    String description,

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    BigDecimal price,

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$",
             message = "Currency must be a three-letter uppercase code")
    String currency

) {}

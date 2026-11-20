package com.premnadh.sellerplatform.orders.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

        @NotBlank(message = "Customer name is required")
        @Size(max = 120, message = "Customer name cannot exceed 120 characters")
        String customerName,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be valid")
        @Size(max = 254, message = "Customer email cannot exceed 254 characters")
        String customerEmail,

        @NotNull(message = "Order items are required")
        @NotEmpty(message = "An order must contain at least one item")
        @Size(max = 50, message = "An order cannot contain more than 50 items")
        List<@NotNull @Valid CreateOrderItemRequest> items

) {
}

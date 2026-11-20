package com.premnadh.sellerplatform.orders.api;

import com.premnadh.sellerplatform.orders.api.dto.CreateOrderRequest;
import com.premnadh.sellerplatform.orders.api.dto.OrderResponse;
import com.premnadh.sellerplatform.orders.application.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        OrderResponse order = orderService.createOrder(
                sellerId,
                idempotencyKey,
                request
        );

        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + order.id()))
                .body(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getSellerOrders(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        List<OrderResponse> orders = orderService
                .getSellerOrders(sellerId);

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        OrderResponse order = orderService.getOrder(
                sellerId,
                orderId
        );

        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        OrderResponse order = orderService.cancelOrder(
                sellerId,
                orderId
        );

        return ResponseEntity.ok(order);
    }

    private UUID getAuthenticatedSellerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

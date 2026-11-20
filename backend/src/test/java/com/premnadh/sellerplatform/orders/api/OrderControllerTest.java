package com.premnadh.sellerplatform.orders.api;

import com.premnadh.sellerplatform.orders.api.dto.OrderResponse;
import com.premnadh.sellerplatform.orders.application.OrderNotFoundException;
import com.premnadh.sellerplatform.orders.application.OrderService;
import com.premnadh.sellerplatform.orders.domain.OrderStatus;
import com.premnadh.sellerplatform.product.application.ProductNotFoundException;
import com.premnadh.sellerplatform.security.config.SecurityConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({
        OrderExceptionHandler.class,
        SecurityConfiguration.class
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private UUID sellerId;
    private UUID orderId;
    private UUID idempotencyKey;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();

        Instant now = Instant.now();

        orderResponse = new OrderResponse(
                orderId,
                "ORD-TEST-001",
                "John Smith",
                "john@example.com",
                OrderStatus.CONFIRMED,
                "GBP",
                new BigDecimal("799.99"),
                new BigDecimal("799.99"),
                List.of(),
                List.of(),
                now,
                now
        );
    }

    private RequestPostProcessor sellerJwt() {
        return jwt().jwt(token ->
                token.subject(sellerId.toString())
        );
    }

    @Test
    void shouldRejectUnauthenticatedOrderRequests() throws Exception {

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldCreateOrderAndReturn201() throws Exception {

        when(orderService.createOrder(
                eq(sellerId),
                eq(idempotencyKey),
                any()
        )).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .with(sellerJwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "customerName": "John Smith",
                                    "customerEmail": "john@example.com",
                                    "items": [
                                        {
                                            "productId": "%s",
                                            "quantity": 2
                                        }
                                    ]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/orders/" + orderId
                ))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-TEST-001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.total").value(799.99));

        verify(orderService).createOrder(
                eq(sellerId),
                eq(idempotencyKey),
                any()
        );
    }

    @Test
    void shouldRejectMissingIdempotencyKey() throws Exception {

        mockMvc.perform(post("/api/v1/orders")
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "customerName": "John Smith",
                                    "customerEmail": "john@example.com",
                                    "items": [
                                        {
                                            "productId": "%s",
                                            "quantity": 1
                                        }
                                    ]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectInvalidOrderRequest() throws Exception {

        mockMvc.perform(post("/api/v1/orders")
                        .with(sellerJwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "customerName": "",
                                    "customerEmail": "invalid-email",
                                    "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectNonPositiveOrderQuantity() throws Exception {

        mockMvc.perform(post("/api/v1/orders")
                        .with(sellerJwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "customerName": "John Smith",
                                    "customerEmail": "john@example.com",
                                    "items": [
                                        {
                                            "productId": "%s",
                                            "quantity": 0
                                        }
                                    ]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldReturnOrdersForAuthenticatedSeller() throws Exception {

        when(orderService.getSellerOrders(sellerId))
                .thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders")
                        .with(sellerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-TEST-001"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        verify(orderService).getSellerOrders(sellerId);
    }

    @Test
    void shouldUseAuthenticatedSellerWhenGettingOrder() throws Exception {

        when(orderService.getOrder(sellerId, orderId))
                .thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .with(sellerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.total").value(799.99));

        verify(orderService).getOrder(sellerId, orderId);
    }

    @Test
    void shouldReturn404WhenOrderDoesNotBelongToSeller() throws Exception {

        when(orderService.getOrder(sellerId, orderId))
                .thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .with(sellerJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(orderService).getOrder(sellerId, orderId);
    }

    @Test
    void shouldReturn404WhenProductDoesNotExist() throws Exception {

        when(orderService.createOrder(
                eq(sellerId),
                eq(idempotencyKey),
                any()
        )).thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(post("/api/v1/orders")
                        .with(sellerJwt())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "customerName": "John Smith",
                                    "customerEmail": "john@example.com",
                                    "items": [
                                        {
                                            "productId": "%s",
                                            "quantity": 1
                                        }
                                    ]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldCancelOrderAndReturn200() throws Exception {

        OrderResponse cancelledOrder = new OrderResponse(
                orderId,
                "ORD-TEST-001",
                "John Smith",
                "john@example.com",
                OrderStatus.CANCELLED,
                "GBP",
                new BigDecimal("799.99"),
                new BigDecimal("799.99"),
                List.of(),
                List.of(),
                orderResponse.createdAt(),
                Instant.now()
        );

        when(orderService.cancelOrder(sellerId, orderId))
                .thenReturn(cancelledOrder);

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .with(sellerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder(sellerId, orderId);
    }

    @Test
    void shouldReturn409WhenOrderCannotBeCancelled() throws Exception {

        when(orderService.cancelOrder(sellerId, orderId))
                .thenThrow(new IllegalStateException(
                        "Only confirmed orders can be cancelled"
                ));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .with(sellerJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(orderService).cancelOrder(sellerId, orderId);
    }
}

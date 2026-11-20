package com.premnadh.sellerplatform.orders.application;

import com.premnadh.sellerplatform.inventory.application.InventoryService;
import com.premnadh.sellerplatform.inventory.domain.InventoryItem;
import com.premnadh.sellerplatform.orders.api.dto.CreateOrderItemRequest;
import com.premnadh.sellerplatform.orders.api.dto.CreateOrderRequest;
import com.premnadh.sellerplatform.orders.api.dto.OrderResponse;
import com.premnadh.sellerplatform.orders.application.payment.PaymentGateway;
import com.premnadh.sellerplatform.orders.application.payment.PaymentResult;
import com.premnadh.sellerplatform.orders.domain.OrderStatus;
import com.premnadh.sellerplatform.orders.domain.PaymentOperation;
import com.premnadh.sellerplatform.orders.domain.PaymentStatus;
import com.premnadh.sellerplatform.orders.repository.OrderRepository;
import com.premnadh.sellerplatform.orders.repository.PaymentAttemptRepository;
import com.premnadh.sellerplatform.product.application.ProductService;
import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;
import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SellerAccountRepository sellerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @MockitoBean
    private PaymentGateway paymentGateway;

    private UUID sellerId;
    private UUID productId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        String testSecret = Base64.getEncoder().encodeToString(
                "integration-test-jwt-secret-at-least-32-bytes"
                        .getBytes(StandardCharsets.UTF_8)
        );

        registry.add("app.security.jwt.secret", () -> testSecret);
    }

    @BeforeEach
    void setUp() {

        SellerAccount seller = sellerRepository.save(
                new SellerAccount(
                        "Order Integration Test Seller",
                        "order-test-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        sellerId = seller.getId();

        Product product = productService.createProduct(
                sellerId,
                "ORDER-TEST-" + UUID.randomUUID(),
                "Integration Test Product",
                "Product used for order integration testing",
                new BigDecimal("29.99"),
                "GBP"
        );

        productId = product.getId();

        productService.updateProductStatus(
                sellerId,
                productId,
                ProductStatus.ACTIVE
        );

        inventoryService.initialiseInventory(
                sellerId,
                productId,
                10,
                2
        );
    }

    @Test
    void shouldCreateOrderAndReserveInventoryAfterSuccessfulPayment() {

        when(paymentGateway.charge(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.succeeded("payment-success-001"));

        OrderResponse response = orderService.createOrder(
                sellerId,
                UUID.randomUUID(),
                createRequest(2)
        );

        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals("GBP", response.currency());
        assertEquals(new BigDecimal("59.98"), response.subtotal());
        assertEquals(new BigDecimal("59.98"), response.total());

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("29.99"),
                response.items().get(0).unitPrice());

        assertEquals(1, response.paymentAttempts().size());
        assertEquals(PaymentOperation.PAYMENT,
                response.paymentAttempts().get(0).operation());
        assertEquals(PaymentStatus.SUCCEEDED,
                response.paymentAttempts().get(0).status());

        InventoryItem inventory =
                inventoryService.getInventory(sellerId, productId);

        assertEquals(10, inventory.getQuantityOnHand());
        assertEquals(2, inventory.getQuantityReserved());
        assertEquals(8, inventory.getAvailableQuantity());

        verify(paymentGateway, times(1)).charge(
                any(UUID.class),
                eq(new BigDecimal("59.98")),
                eq("GBP")
        );
    }

    @Test
    void shouldReleaseInventoryWhenPaymentFails() {

        when(paymentGateway.charge(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.failed("Card declined"));

        OrderResponse response = orderService.createOrder(
                sellerId,
                UUID.randomUUID(),
                createRequest(2)
        );

        assertEquals(OrderStatus.PAYMENT_FAILED, response.status());

        assertEquals(1, response.paymentAttempts().size());
        assertEquals(PaymentStatus.FAILED,
                response.paymentAttempts().get(0).status());
        assertEquals("Card declined",
                response.paymentAttempts().get(0).failureReason());

        InventoryItem inventory =
                inventoryService.getInventory(sellerId, productId);

        assertEquals(10, inventory.getQuantityOnHand());
        assertEquals(0, inventory.getQuantityReserved());
        assertEquals(10, inventory.getAvailableQuantity());
    }

    @Test
    void shouldCancelOrderAndReleaseInventoryAfterSuccessfulRefund() {

        when(paymentGateway.charge(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.succeeded("payment-success-002"));

        when(paymentGateway.refund(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.succeeded("refund-success-001"));

        OrderResponse createdOrder = orderService.createOrder(
                sellerId,
                UUID.randomUUID(),
                createRequest(2)
        );

        OrderResponse cancelledOrder = orderService.cancelOrder(
                sellerId,
                createdOrder.id()
        );

        assertEquals(OrderStatus.CANCELLED, cancelledOrder.status());

        assertEquals(2, cancelledOrder.paymentAttempts().size());

        assertTrue(cancelledOrder.paymentAttempts().stream().anyMatch(
                attempt ->
                        attempt.operation() == PaymentOperation.PAYMENT
                                && attempt.status() == PaymentStatus.SUCCEEDED
        ));

        assertTrue(cancelledOrder.paymentAttempts().stream().anyMatch(
                attempt ->
                        attempt.operation() == PaymentOperation.REFUND
                                && attempt.status() == PaymentStatus.SUCCEEDED
        ));

        InventoryItem inventory =
                inventoryService.getInventory(sellerId, productId);

        assertEquals(10, inventory.getQuantityOnHand());
        assertEquals(0, inventory.getQuantityReserved());
        assertEquals(10, inventory.getAvailableQuantity());

        verify(paymentGateway, times(1)).refund(
                any(UUID.class),
                eq(new BigDecimal("59.98")),
                eq("GBP")
        );
    }

    @Test
    void shouldKeepOrderAndInventoryUnchangedWhenRefundFails() {

        when(paymentGateway.charge(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.succeeded("payment-success-003"));

        when(paymentGateway.refund(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.failed("Refund declined"));

        OrderResponse createdOrder = orderService.createOrder(
                sellerId,
                UUID.randomUUID(),
                createRequest(2)
        );

        OrderResponse cancellationAttempt = orderService.cancelOrder(
                sellerId,
                createdOrder.id()
        );

        assertEquals(
                OrderStatus.CONFIRMED,
                cancellationAttempt.status()
        );

        assertTrue(cancellationAttempt.paymentAttempts().stream().anyMatch(
                attempt ->
                        attempt.operation() == PaymentOperation.REFUND
                                && attempt.status() == PaymentStatus.FAILED
                                && "Refund declined".equals(
                                        attempt.failureReason()
                                )
        ));

        InventoryItem inventory =
                inventoryService.getInventory(sellerId, productId);

        assertEquals(10, inventory.getQuantityOnHand());
        assertEquals(2, inventory.getQuantityReserved());
        assertEquals(8, inventory.getAvailableQuantity());
    }

    @Test
    void shouldReturnExistingOrderWithoutChargingAgainForSameIdempotencyKey() {

        when(paymentGateway.charge(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.succeeded("payment-success-004"));

        UUID idempotencyKey = UUID.randomUUID();

        OrderResponse firstResponse = orderService.createOrder(
                sellerId,
                idempotencyKey,
                createRequest(2)
        );

        OrderResponse retryResponse = orderService.createOrder(
                sellerId,
                idempotencyKey,
                createRequest(2)
        );

        assertEquals(firstResponse.id(), retryResponse.id());
        assertEquals(firstResponse.orderNumber(), retryResponse.orderNumber());
        assertEquals(firstResponse.status(), retryResponse.status());

        assertEquals(
                1,
                paymentAttemptRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                sellerId,
                                firstResponse.id()
                        ).size()
        );

        assertEquals(
                1,
                orderRepository.findAllBySellerIdOrderByCreatedAtDesc(
                        sellerId
                ).stream()
                        .filter(order ->
                                order.getIdempotencyKey()
                                        .equals(idempotencyKey)
                        )
                        .count()
        );

        InventoryItem inventory =
                inventoryService.getInventory(sellerId, productId);

        assertEquals(2, inventory.getQuantityReserved());

        verify(paymentGateway, times(1)).charge(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        );
    }

    @Test
    void shouldPreventAnotherSellerFromAccessingOrder() {

        when(paymentGateway.charge(
                any(UUID.class),
                any(BigDecimal.class),
                eq("GBP")
        )).thenReturn(PaymentResult.succeeded("payment-success-005"));

        OrderResponse response = orderService.createOrder(
                sellerId,
                UUID.randomUUID(),
                createRequest(1)
        );

        SellerAccount anotherSeller = sellerRepository.save(
                new SellerAccount(
                        "Another Integration Test Seller",
                        "another-order-test-" + UUID.randomUUID()
                                + "@example.com",
                        "test-password-hash"
                )
        );

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrder(
                        anotherSeller.getId(),
                        response.id()
                )
        );
    }

    private CreateOrderRequest createRequest(int quantity) {

        return new CreateOrderRequest(
                "Integration Test Customer",
                "customer@example.com",
                List.of(
                        new CreateOrderItemRequest(productId, quantity)
                )
        );
    }
}

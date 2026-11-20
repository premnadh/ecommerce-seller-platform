package com.premnadh.sellerplatform.orders.repository;

import com.premnadh.sellerplatform.orders.domain.Order;
import com.premnadh.sellerplatform.orders.domain.OrderItem;
import com.premnadh.sellerplatform.orders.domain.PaymentAttempt;
import com.premnadh.sellerplatform.orders.domain.PaymentOperation;
import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.repository.ProductRepository;
import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class OrderRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private SellerAccountRepository sellerRepository;

    @Autowired
    private ProductRepository productRepository;

    private UUID sellerAId;
    private UUID sellerBId;
    private UUID productAId;
    private String productSku;

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
        SellerAccount sellerA = sellerRepository.save(
                new SellerAccount(
                        "Seller A",
                        "seller-a-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        SellerAccount sellerB = sellerRepository.save(
                new SellerAccount(
                        "Seller B",
                        "seller-b-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        sellerAId = sellerA.getId();
        sellerBId = sellerB.getId();

        productSku = "ORDER-SKU-" + UUID.randomUUID();

        Product product = productRepository.save(
                new Product(
                        sellerAId,
                        productSku,
                        "Gaming Keyboard",
                        "Mechanical gaming keyboard",
                        new BigDecimal("49.99"),
                        "GBP"
                )
        );

        productAId = product.getId();
    }

    private Order createOrder(UUID sellerId) {
        return new Order(
                sellerId,
                "ORD-" + UUID.randomUUID(),
                UUID.randomUUID(),
                "Customer A",
                "customer@example.com",
                "GBP",
                new BigDecimal("99.98"),
                new BigDecimal("99.98")
        );
    }

    @Test
    void shouldPersistAndRetrieveOrder() {
        Order order = orderRepository.saveAndFlush(
                createOrder(sellerAId)
        );

        Order retrieved = orderRepository
                .findBySellerIdAndId(sellerAId, order.getId())
                .orElseThrow();

        assertEquals(order.getId(), retrieved.getId());
        assertEquals(sellerAId, retrieved.getSellerId());
    }

    @Test
    void shouldNotRetrieveAnotherSellersOrder() {
        Order sellerAOrder = orderRepository.saveAndFlush(
                createOrder(sellerAId)
        );

        assertTrue(
                orderRepository.findBySellerIdAndId(
                        sellerBId,
                        sellerAOrder.getId()
                ).isEmpty()
        );
    }

    @Test
    void shouldFindOrderUsingIdempotencyKey() {
        Order order = createOrder(sellerAId);

        orderRepository.saveAndFlush(order);

        Order retrieved = orderRepository
                .findBySellerIdAndIdempotencyKey(
                        sellerAId,
                        order.getIdempotencyKey()
                )
                .orElseThrow();

        assertEquals(order.getId(), retrieved.getId());
    }

    @Test
    void shouldRetrieveOrdersOnlyForRequestedSeller() {
        Order sellerAOrder = orderRepository.saveAndFlush(
                createOrder(sellerAId)
        );

        Order sellerBOrder = orderRepository.saveAndFlush(
                createOrder(sellerBId)
        );

        var sellerAOrders =
                orderRepository.findAllBySellerIdOrderByCreatedAtDesc(
                        sellerAId
                );

        assertEquals(1, sellerAOrders.size());
        assertEquals(sellerAOrder.getId(), sellerAOrders.get(0).getId());
        assertNotEquals(sellerBOrder.getId(), sellerAOrders.get(0).getId());
    }

    @Test
    void shouldPersistAndRetrieveOrderItems() {
        Order order = orderRepository.saveAndFlush(
                createOrder(sellerAId)
        );

        OrderItem item = new OrderItem(
                sellerAId,
                order.getId(),
                productAId,
                productSku,
                "Gaming Keyboard",
                new BigDecimal("49.99"),
                2
        );

        orderItemRepository.saveAndFlush(item);

        var retrievedItems =
                orderItemRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                sellerAId,
                                order.getId()
                        );

        assertEquals(1, retrievedItems.size());
        assertEquals(item.getId(), retrievedItems.get(0).getId());
        assertEquals(
                new BigDecimal("99.98"),
                retrievedItems.get(0).getLineTotal()
        );
    }

    @Test
    void shouldNotRetrieveAnotherSellersOrderItems() {
        Order order = orderRepository.saveAndFlush(
                createOrder(sellerAId)
        );

        orderItemRepository.saveAndFlush(
                new OrderItem(
                        sellerAId,
                        order.getId(),
                        productAId,
                        productSku,
                        "Gaming Keyboard",
                        new BigDecimal("49.99"),
                        1
                )
        );

        var retrievedItems =
                orderItemRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                sellerBId,
                                order.getId()
                        );

        assertTrue(retrievedItems.isEmpty());
    }

    @Test
    void shouldPersistAndRetrievePaymentAttempt() {
        Order order = orderRepository.saveAndFlush(
                createOrder(sellerAId)
        );

        PaymentAttempt attempt = new PaymentAttempt(
                sellerAId,
                order.getId(),
                PaymentOperation.PAYMENT,
                new BigDecimal("99.98"),
                "GBP"
        );

        attempt.markSucceeded("MOCK-PAY-" + UUID.randomUUID());

        paymentAttemptRepository.saveAndFlush(attempt);

        var retrievedAttempts =
                paymentAttemptRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                sellerAId,
                                order.getId()
                        );

        assertEquals(1, retrievedAttempts.size());
        assertEquals(
                attempt.getId(),
                retrievedAttempts.get(0).getId()
        );
    }

    @Test
    void shouldNotRetrieveAnotherSellersPaymentAttempts() {
        Order order = orderRepository.saveAndFlush(
                createOrder(sellerAId)
        );

        paymentAttemptRepository.saveAndFlush(
                new PaymentAttempt(
                        sellerAId,
                        order.getId(),
                        PaymentOperation.PAYMENT,
                        new BigDecimal("99.98"),
                        "GBP"
                )
        );

        var retrievedAttempts =
                paymentAttemptRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                sellerBId,
                                order.getId()
                        );

        assertTrue(retrievedAttempts.isEmpty());
    }
}

package com.premnadh.sellerplatform.inventory.application;

import com.premnadh.sellerplatform.inventory.domain.InventoryItem;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovementType;
import com.premnadh.sellerplatform.inventory.repository.InventoryItemRepository;
import com.premnadh.sellerplatform.inventory.repository.InventoryMovementRepository;
import com.premnadh.sellerplatform.product.application.ProductService;
import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class InventoryConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SellerAccountRepository sellerRepository;

    @Autowired
    private InventoryItemRepository inventoryRepository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    private UUID sellerId;
    private UUID productId;
    private UUID orderId;

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
                        "Concurrency Test Seller",
                        "concurrency-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        sellerId = seller.getId();

        Product product = productService.createProduct(
                sellerId,
                "STOCK-001",
                "Concurrency Test Product",
                "Product used for inventory concurrency testing",
                new BigDecimal("49.99"),
                "GBP"
        );

        productId = product.getId();
        orderId = UUID.randomUUID();

        inventoryService.initialiseInventory(
                sellerId,
                productId,
                10,
                2
        );
    }

    @Test
    void shouldPreventOversellingDuringConcurrentReservations()
            throws Exception {

        int reservationQuantity = 7;

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            Future<Boolean> firstReservation = executor.submit(() -> {

                ready.countDown();
                start.await();

                try {
                    inventoryService.reserveStock(
                            sellerId,
                            productId,
                            reservationQuantity,
                            orderId
                    );

                    return true;

                } catch (IllegalStateException exception) {
                    return false;
                }
            });

            Future<Boolean> secondReservation = executor.submit(() -> {

                ready.countDown();
                start.await();

                try {
                    inventoryService.reserveStock(
                            sellerId,
                            productId,
                            reservationQuantity,
                            UUID.randomUUID()
                    );

                    return true;

                } catch (IllegalStateException exception) {
                    return false;
                }
            });

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both reservation threads should be ready"
            );

            start.countDown();

            boolean firstSucceeded =
                    firstReservation.get(15, TimeUnit.SECONDS);

            boolean secondSucceeded =
                    secondReservation.get(15, TimeUnit.SECONDS);

            int successfulReservations =
                    (firstSucceeded ? 1 : 0)
                    + (secondSucceeded ? 1 : 0);

            assertEquals(
                    1,
                    successfulReservations,
                    "Exactly one reservation should succeed"
            );

        } finally {

            start.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Reservation threads should terminate"
            );
        }

        InventoryItem inventory = inventoryRepository
                .findBySellerIdAndProductId(sellerId, productId)
                .orElseThrow();

        assertEquals(10, inventory.getQuantityOnHand());
        assertEquals(7, inventory.getQuantityReserved());
        assertEquals(3, inventory.getAvailableQuantity());

        List<InventoryMovement> movements =
                movementRepository
                        .findAllBySellerIdAndProductIdOrderByCreatedAtDesc(
                                sellerId,
                                productId
                        );

        long reservationCount = movements.stream()
                .filter(movement ->
                        movement.getMovementType()
                                == InventoryMovementType.RESERVATION
                )
                .count();

        assertEquals(
                1,
                reservationCount,
                "Only one reservation movement should be recorded"
        );
    }

    @Test
    void shouldHandleConcurrentRetriesOfSameReservationIdempotently()
            throws Exception {

        int reservationQuantity = 4;

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            Future<Boolean> firstRequest = executor.submit(() -> {

                ready.countDown();
                start.await();

                inventoryService.reserveStock(
                        sellerId,
                        productId,
                        reservationQuantity,
                        orderId
                );

                return true;
            });

            Future<Boolean> secondRequest = executor.submit(() -> {

                ready.countDown();
                start.await();

                inventoryService.reserveStock(
                        sellerId,
                        productId,
                        reservationQuantity,
                        orderId
                );

                return true;
            });

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both retry threads should be ready"
            );

            start.countDown();

            assertTrue(
                    firstRequest.get(15, TimeUnit.SECONDS),
                    "First reservation request should succeed"
            );

            assertTrue(
                    secondRequest.get(15, TimeUnit.SECONDS),
                    "Repeated reservation request should succeed idempotently"
            );

        } finally {

            start.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Reservation threads should terminate"
            );
        }

        InventoryItem inventory = inventoryRepository
                .findBySellerIdAndProductId(sellerId, productId)
                .orElseThrow();

        assertEquals(10, inventory.getQuantityOnHand());
        assertEquals(4, inventory.getQuantityReserved());
        assertEquals(6, inventory.getAvailableQuantity());

        List<InventoryMovement> movements =
                movementRepository
                        .findAllBySellerIdAndProductIdOrderByCreatedAtDesc(
                                sellerId,
                                productId
                        );

        long reservationCount = movements.stream()
                .filter(movement ->
                        movement.getMovementType()
                                == InventoryMovementType.RESERVATION
                )
                .count();

        assertEquals(
                1,
                reservationCount,
                "Concurrent retries must create only one reservation movement"
        );
    }
}

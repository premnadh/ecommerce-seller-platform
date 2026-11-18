package com.premnadh.sellerplatform.product.application;

import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;
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
class ProductTenantIsolationIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProductService productService;

    @Autowired
    private SellerAccountRepository sellerRepository;

    @Autowired
    private ProductRepository productRepository;

    private UUID sellerAId;
    private UUID sellerBId;

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
    }

    @Test
    void shouldIsolateProductAccessBetweenSellers() {

        Product productA = productService.createProduct(
                sellerAId,
                "PHONE-001",
                "Seller A Phone",
                "Product belonging to seller A",
                new BigDecimal("799.99"),
                "GBP"
        );

        Product productB = productService.createProduct(
                sellerBId,
                "PHONE-001",
                "Seller B Phone",
                "Product belonging to seller B",
                new BigDecimal("699.99"),
                "GBP"
        );

        // Each seller sees only their own products.
        assertEquals(
                1,
                productService.getProductsBySeller(sellerAId).size()
        );

        assertEquals(
                1,
                productService.getProductsBySeller(sellerBId).size()
        );

        assertEquals(
                productA.getId(),
                productService.getProductsBySeller(sellerAId)
                        .get(0).getId()
        );

        assertEquals(
                productB.getId(),
                productService.getProductsBySeller(sellerBId)
                        .get(0).getId()
        );

        // Seller A cannot retrieve seller B's product.
        assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(
                        sellerAId,
                        productB.getId()
                )
        );

        // Seller A cannot update seller B's product.
        assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(
                        sellerAId,
                        productB.getId(),
                        "Unauthorised update",
                        "Attempted cross-seller update",
                        new BigDecimal("1.00"),
                        "GBP"
                )
        );

        // Seller A cannot change seller B's product status.
        assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProductStatus(
                        sellerAId,
                        productB.getId(),
                        ProductStatus.ARCHIVED
                )
        );

        // Confirm seller B's product remains unchanged.
        Product unchangedProduct = productRepository
                .findByIdAndSellerId(productB.getId(), sellerBId)
                .orElseThrow();

        assertEquals("Seller B Phone", unchangedProduct.getName());
        assertEquals(ProductStatus.DRAFT, unchangedProduct.getStatus());
    }
}

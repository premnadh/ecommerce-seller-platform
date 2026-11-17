package com.premnadh.sellerplatform.product.application;

import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;
import com.premnadh.sellerplatform.product.repository.ProductRepository;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SellerAccountRepository sellerRepository;

    @InjectMocks
    private ProductService productService;

    private UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
    }

    @Test
    void shouldCreateProductSuccessfully() {
        when(sellerRepository.existsById(sellerId)).thenReturn(true);

        when(productRepository.existsBySellerIdAndSku(
                sellerId, "PHONE-001")).thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product product = productService.createProduct(
                sellerId,
                "phone-001",
                "Samsung Galaxy S25",
                "Flagship smartphone",
                new BigDecimal("799.99"),
                "GBP"
        );

        assertNotNull(product.getId());
        assertEquals(sellerId, product.getSellerId());
        assertEquals("PHONE-001", product.getSku());
        assertEquals("Samsung Galaxy S25", product.getName());
        assertEquals(new BigDecimal("799.99"), product.getPrice());
        assertEquals("GBP", product.getCurrency());
        assertEquals(ProductStatus.DRAFT, product.getStatus());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldRejectDuplicateSku() {
        when(sellerRepository.existsById(sellerId)).thenReturn(true);

        when(productRepository.existsBySellerIdAndSku(
                sellerId, "PHONE-001")).thenReturn(true);

        assertThrows(
                DuplicateProductSkuException.class,
                () -> productService.createProduct(
                        sellerId,
                        "PHONE-001",
                        "Samsung Galaxy S25",
                        "Description",
                        new BigDecimal("799.99"),
                        "GBP"
                )
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectProductCreationForNonexistentSeller() {
        when(sellerRepository.existsById(sellerId)).thenReturn(false);

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.createProduct(
                        sellerId,
                        "PHONE-001",
                        "Samsung Galaxy S25",
                        "Description",
                        new BigDecimal("799.99"),
                        "GBP"
                )
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldReturnProductsForSeller() {
        Product product = createProduct();

        when(sellerRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.findAllBySellerId(sellerId))
                .thenReturn(List.of(product));

        List<Product> products =
                productService.getProductsBySeller(sellerId);

        assertEquals(1, products.size());
        assertEquals(product.getId(), products.get(0).getId());

        verify(productRepository).findAllBySellerId(sellerId);
    }

    @Test
    void shouldRejectRetrievalOfProductOwnedByAnotherSeller() {
        Product product = createProduct();

        when(sellerRepository.existsById(sellerId)).thenReturn(true);

        when(productRepository.findByIdAndSellerId(
                product.getId(), sellerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(
                        sellerId, product.getId())
        );
    }

    @Test
    void shouldUpdateProductDetails() {
        Product product = createProduct();

        when(sellerRepository.existsById(sellerId)).thenReturn(true);

        when(productRepository.findByIdAndSellerId(
                product.getId(), sellerId))
                .thenReturn(Optional.of(product));

        Product updated = productService.updateProduct(
                sellerId,
                product.getId(),
                "Samsung Galaxy S25 Ultra",
                "Updated description",
                new BigDecimal("749.99"),
                "GBP"
        );

        assertEquals("Samsung Galaxy S25 Ultra", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(new BigDecimal("749.99"), updated.getPrice());
        assertEquals("GBP", updated.getCurrency());
    }

    @Test
    void shouldArchiveProduct() {
        Product product = createProduct();

        when(sellerRepository.existsById(sellerId)).thenReturn(true);

        when(productRepository.findByIdAndSellerId(
                product.getId(), sellerId))
                .thenReturn(Optional.of(product));

        Product archived = productService.updateProductStatus(
                sellerId,
                product.getId(),
                ProductStatus.ARCHIVED
        );

        assertEquals(ProductStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void shouldRejectReactivationOfArchivedProduct() {
        Product product = createProduct();
        product.changeStatus(ProductStatus.ARCHIVED);

        when(sellerRepository.existsById(sellerId)).thenReturn(true);

        when(productRepository.findByIdAndSellerId(
                product.getId(), sellerId))
                .thenReturn(Optional.of(product));

        assertThrows(
                IllegalStateException.class,
                () -> productService.updateProductStatus(
                        sellerId,
                        product.getId(),
                        ProductStatus.ACTIVE
                )
        );
    }

    private Product createProduct() {
        return new Product(
                sellerId,
                "PHONE-001",
                "Samsung Galaxy S25",
                "Flagship smartphone",
                new BigDecimal("799.99"),
                "GBP"
        );
    }
}

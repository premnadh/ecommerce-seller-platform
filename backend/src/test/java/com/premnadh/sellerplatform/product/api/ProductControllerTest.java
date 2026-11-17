package com.premnadh.sellerplatform.product.api;

import com.premnadh.sellerplatform.product.application.DuplicateProductSkuException;
import com.premnadh.sellerplatform.product.application.ProductNotFoundException;
import com.premnadh.sellerplatform.product.application.ProductService;
import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(ProductExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private UUID sellerId;
    private Product product;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();

        product = new Product(
                sellerId,
                "PHONE-001",
                "Samsung Galaxy S25",
                "Flagship smartphone",
                new BigDecimal("799.99"),
                "GBP"
        );
    }

    @Test
    void shouldCreateProductAndReturn201() throws Exception {
        when(productService.createProduct(
                eq(sellerId),
                eq("PHONE-001"),
                eq("Samsung Galaxy S25"),
                eq("Flagship smartphone"),
                eq(new BigDecimal("799.99")),
                eq("GBP")
        )).thenReturn(product);

        mockMvc.perform(post("/api/v1/sellers/{sellerId}/products", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sku": "PHONE-001",
                                    "name": "Samsung Galaxy S25",
                                    "description": "Flagship smartphone",
                                    "price": 799.99,
                                    "currency": "GBP"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.sku").value("PHONE-001"))
                .andExpect(jsonPath("$.name").value("Samsung Galaxy S25"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(productService).createProduct(
                eq(sellerId),
                eq("PHONE-001"),
                eq("Samsung Galaxy S25"),
                eq("Flagship smartphone"),
                eq(new BigDecimal("799.99")),
                eq("GBP")
        );
    }

    @Test
    void shouldRejectInvalidProductRequest() throws Exception {
        mockMvc.perform(post("/api/v1/sellers/{sellerId}/products", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sku": "",
                                    "name": "",
                                    "description": "Description",
                                    "price": -10.00,
                                    "currency": "gbp"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void shouldReturnProductsForSeller() throws Exception {
        when(productService.getProductsBySeller(sellerId))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/api/v1/sellers/{sellerId}/products", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()))
                .andExpect(jsonPath("$[0].sku").value("PHONE-001"))
                .andExpect(jsonPath("$[0].name").value("Samsung Galaxy S25"));

        verify(productService).getProductsBySeller(sellerId);
    }

    @Test
    void shouldReturnSingleProduct() throws Exception {
        when(productService.getProductById(sellerId, product.getId()))
                .thenReturn(product);

        mockMvc.perform(get(
                        "/api/v1/sellers/{sellerId}/products/{productId}",
                        sellerId,
                        product.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.price").value(799.99));

        verify(productService).getProductById(sellerId, product.getId());
    }

    @Test
    void shouldReturn404WhenProductDoesNotExist() throws Exception {
        when(productService.getProductById(sellerId, product.getId()))
                .thenThrow(new ProductNotFoundException(
                        "Product not found: " + product.getId()
                ));

        mockMvc.perform(get(
                        "/api/v1/sellers/{sellerId}/products/{productId}",
                        sellerId,
                        product.getId()
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateProductAndReturn200() throws Exception {
        product.updateDetails(
                "Samsung Galaxy S25 Ultra",
                "Updated description",
                new BigDecimal("749.99"),
                "GBP"
        );

        when(productService.updateProduct(
                eq(sellerId),
                eq(product.getId()),
                eq("Samsung Galaxy S25 Ultra"),
                eq("Updated description"),
                eq(new BigDecimal("749.99")),
                eq("GBP")
        )).thenReturn(product);

        mockMvc.perform(put(
                        "/api/v1/sellers/{sellerId}/products/{productId}",
                        sellerId,
                        product.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Samsung Galaxy S25 Ultra",
                                    "description": "Updated description",
                                    "price": 749.99,
                                    "currency": "GBP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung Galaxy S25 Ultra"))
                .andExpect(jsonPath("$.price").value(749.99));
    }

    @Test
    void shouldRejectDuplicateSkuWith409() throws Exception {
        when(productService.createProduct(
                any(UUID.class),
                anyString(),
                anyString(),
                any(),
                any(BigDecimal.class),
                anyString()
        )).thenThrow(new DuplicateProductSkuException("PHONE-001"));

        mockMvc.perform(post("/api/v1/sellers/{sellerId}/products", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sku": "PHONE-001",
                                    "name": "Samsung Galaxy S25",
                                    "description": "Description",
                                    "price": 799.99,
                                    "currency": "GBP"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldUpdateProductStatus() throws Exception {
        product.changeStatus(ProductStatus.ACTIVE);

        when(productService.updateProductStatus(
                sellerId,
                product.getId(),
                ProductStatus.ACTIVE
        )).thenReturn(product);

        mockMvc.perform(patch(
                        "/api/v1/sellers/{sellerId}/products/{productId}/status",
                        sellerId,
                        product.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn409ForInvalidProductStateTransition() throws Exception {
        when(productService.updateProductStatus(
                sellerId,
                product.getId(),
                ProductStatus.ACTIVE
        )).thenThrow(new IllegalStateException(
                "Archived products cannot be reactivated"
        ));

        mockMvc.perform(patch(
                        "/api/v1/sellers/{sellerId}/products/{productId}/status",
                        sellerId,
                        product.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isConflict());
    }
}

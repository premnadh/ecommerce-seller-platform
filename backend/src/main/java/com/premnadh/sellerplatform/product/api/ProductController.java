package com.premnadh.sellerplatform.product.api;

import com.premnadh.sellerplatform.product.api.dto.CreateProductRequest;
import com.premnadh.sellerplatform.product.api.dto.ProductResponse;
import com.premnadh.sellerplatform.product.api.dto.UpdateProductRequest;
import com.premnadh.sellerplatform.product.api.dto.UpdateProductStatusRequest;
import com.premnadh.sellerplatform.product.application.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers/{sellerId}/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @PathVariable UUID sellerId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        var product = productService.createProduct(
            sellerId,
            request.sku(),
            request.name(),
            request.description(),
            request.price(),
            request.currency()
        );

        return ResponseEntity
            .created(URI.create(
                "/api/v1/sellers/" + sellerId
                + "/products/" + product.getId()
            ))
            .body(ProductResponse.from(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @PathVariable UUID sellerId
    ) {
        List<ProductResponse> products = productService
            .getProductsBySeller(sellerId)
            .stream()
            .map(ProductResponse::from)
            .toList();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable UUID sellerId,
            @PathVariable UUID productId
    ) {
        var product = productService.getProductById(sellerId, productId);

        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID sellerId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        var product = productService.updateProduct(
            sellerId,
            productId,
            request.name(),
            request.description(),
            request.price(),
            request.currency()
        );

        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable UUID sellerId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        var product = productService.updateProductStatus(
            sellerId,
            productId,
            request.status()
        );

        return ResponseEntity.ok(ProductResponse.from(product));
    }
}

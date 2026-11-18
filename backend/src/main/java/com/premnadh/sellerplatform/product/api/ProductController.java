package com.premnadh.sellerplatform.product.api;

import com.premnadh.sellerplatform.product.api.dto.CreateProductRequest;
import com.premnadh.sellerplatform.product.api.dto.ProductResponse;
import com.premnadh.sellerplatform.product.api.dto.UpdateProductRequest;
import com.premnadh.sellerplatform.product.api.dto.UpdateProductStatusRequest;
import com.premnadh.sellerplatform.product.application.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProductRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var product = productService.createProduct(
                sellerId,
                request.sku(),
                request.name(),
                request.description(),
                request.price(),
                request.currency()
        );

        return ResponseEntity
                .created(URI.create("/api/v1/products/" + product.getId()))
                .body(ProductResponse.from(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        List<ProductResponse> products = productService
                .getProductsBySeller(sellerId)
                .stream()
                .map(ProductResponse::from)
                .toList();

        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var product = productService.getProductById(sellerId, productId);

        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

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
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var product = productService.updateProductStatus(
                sellerId,
                productId,
                request.status()
        );

        return ResponseEntity.ok(ProductResponse.from(product));
    }

    private UUID getAuthenticatedSellerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

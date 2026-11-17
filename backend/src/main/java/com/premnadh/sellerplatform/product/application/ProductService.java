package com.premnadh.sellerplatform.product.application;

import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;
import com.premnadh.sellerplatform.product.repository.ProductRepository;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerAccountRepository sellerRepository;

    public ProductService(
            ProductRepository productRepository,
            SellerAccountRepository sellerRepository
    ) {
        this.productRepository = productRepository;
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public Product createProduct(
            UUID sellerId,
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency
    ) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new ProductNotFoundException(
                "Seller account not found: " + sellerId
            );
        }

        String normalizedSku = sku.trim().toUpperCase(Locale.ROOT);

        if (productRepository.existsBySellerIdAndSku(
                sellerId, normalizedSku)) {
            throw new DuplicateProductSkuException(normalizedSku);
        }

        Product product = new Product(
            sellerId,
            normalizedSku,
            name.trim(),
            description == null ? null : description.trim(),
            price,
            currency
        );

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsBySeller(UUID sellerId) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new ProductNotFoundException(
                "Seller account not found: " + sellerId
            );
        }

        return productRepository.findAllBySellerId(sellerId);
    }

    @Transactional(readOnly = true)
    public Product getProductById(UUID sellerId, UUID productId) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new ProductNotFoundException(
                "Seller account not found: " + sellerId
            );
        }

        return productRepository.findByIdAndSellerId(productId, sellerId)
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found: " + productId
            ));
    }

    @Transactional
    public Product updateProduct(
            UUID sellerId,
            UUID productId,
            String name,
            String description,
            BigDecimal price,
            String currency
    ) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new ProductNotFoundException(
                "Seller account not found: " + sellerId
            );
        }

        Product product = productRepository
            .findByIdAndSellerId(productId, sellerId)
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found: " + productId
            ));

        product.updateDetails(
            name.trim(),
            description == null ? null : description.trim(),
            price,
            currency
        );

        return product;
    }

    @Transactional
    public Product updateProductStatus(
            UUID sellerId,
            UUID productId,
            ProductStatus status
    ) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new ProductNotFoundException(
                "Seller account not found: " + sellerId
            );
        }

        Product product = productRepository
            .findByIdAndSellerId(productId, sellerId)
            .orElseThrow(() -> new ProductNotFoundException(
                "Product not found: " + productId
            ));

        product.changeStatus(status);

        return product;
    }
}

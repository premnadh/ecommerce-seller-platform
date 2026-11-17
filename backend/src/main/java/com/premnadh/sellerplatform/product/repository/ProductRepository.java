package com.premnadh.sellerplatform.product.repository;

import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySellerIdAndSku(UUID sellerId, String sku);

    Optional<Product> findByIdAndSellerId(UUID id, UUID sellerId);

    List<Product> findAllBySellerId(UUID sellerId);

    List<Product> findAllBySellerIdAndStatus(
            UUID sellerId,
            ProductStatus status
    );
}
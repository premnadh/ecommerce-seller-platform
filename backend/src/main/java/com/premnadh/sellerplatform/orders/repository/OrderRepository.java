package com.premnadh.sellerplatform.orders.repository;

import com.premnadh.sellerplatform.orders.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findBySellerIdAndId(UUID sellerId, UUID orderId);

    Optional<Order> findBySellerIdAndIdempotencyKey(
            UUID sellerId,
            UUID idempotencyKey
    );

    List<Order> findAllBySellerIdOrderByCreatedAtDesc(UUID sellerId);
}

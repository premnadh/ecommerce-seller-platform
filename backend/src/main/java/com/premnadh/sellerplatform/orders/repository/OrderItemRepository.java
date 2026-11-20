package com.premnadh.sellerplatform.orders.repository;

import com.premnadh.sellerplatform.orders.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
            UUID sellerId,
            UUID orderId
    );
}

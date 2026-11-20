package com.premnadh.sellerplatform.orders.repository;

import com.premnadh.sellerplatform.orders.domain.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAttemptRepository
        extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
            UUID sellerId,
            UUID orderId
    );
}

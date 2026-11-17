package com.premnadh.sellerplatform.seller.repository;

import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SellerAccountRepository
        extends JpaRepository<SellerAccount, UUID> {

    boolean existsByEmailIgnoreCase(String email);
}

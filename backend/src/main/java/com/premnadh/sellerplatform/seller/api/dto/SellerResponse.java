package com.premnadh.sellerplatform.seller.api.dto;

import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.domain.SellerStatus;

import java.time.Instant;
import java.util.UUID;

public record SellerResponse(
        UUID id,
        String businessName,
        String email,
        SellerStatus status,
        Instant createdAt
) {

    public static SellerResponse from(SellerAccount seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getBusinessName(),
                seller.getEmail(),
                seller.getStatus(),
                seller.getCreatedAt()
        );
    }
}
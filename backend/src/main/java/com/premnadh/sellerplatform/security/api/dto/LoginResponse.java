package com.premnadh.sellerplatform.security.api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
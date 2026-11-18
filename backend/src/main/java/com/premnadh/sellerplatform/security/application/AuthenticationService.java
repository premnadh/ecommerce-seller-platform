package com.premnadh.sellerplatform.security.application;

import java.util.Locale;

import com.premnadh.sellerplatform.security.api.dto.LoginRequest;
import com.premnadh.sellerplatform.security.api.dto.LoginResponse;
import com.premnadh.sellerplatform.security.jwt.JwtTokenService;
import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.domain.SellerStatus;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final SellerAccountRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final long accessTokenExpirationSeconds;

    public AuthenticationService(
            SellerAccountRepository sellerRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            @Value("${app.security.jwt.access-token-expiration-seconds}")
            long accessTokenExpirationSeconds
    ) {
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        SellerAccount seller = sellerRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(this::invalidCredentials);

        if (seller.getPasswordHash() == null
                || !passwordEncoder.matches(
                        request.password(),
                        seller.getPasswordHash()
                )) {
            throw invalidCredentials();
        }

        if (seller.getStatus() != SellerStatus.ACTIVE) {
            throw invalidCredentials();
        }

        String accessToken = jwtTokenService.generateAccessToken(
                seller.getId(),
                seller.getEmail()
        );

        return new LoginResponse(
                accessToken,
                "Bearer",
                accessTokenExpirationSeconds
        );
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Invalid email or password");
    }
}

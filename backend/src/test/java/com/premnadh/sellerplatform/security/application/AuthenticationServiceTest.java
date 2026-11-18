package com.premnadh.sellerplatform.security.application;

import com.premnadh.sellerplatform.security.api.dto.LoginRequest;
import com.premnadh.sellerplatform.security.api.dto.LoginResponse;
import com.premnadh.sellerplatform.security.jwt.JwtTokenService;
import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.domain.SellerStatus;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private SellerAccountRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthenticationService authenticationService;

    private static final String EMAIL = "seller@example.com";
    private static final String PASSWORD = "SecurePassword123!";
    private static final String HASH = "$2a$12$exampleHash";
    private static final String TOKEN = "mock.jwt.access.token";

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                sellerRepository,
                passwordEncoder,
                jwtTokenService,
                900
        );
    }

    @Test
    void shouldLoginWithValidCredentials() {
        SellerAccount seller = new SellerAccount(
                "Example Business",
                EMAIL,
                HASH
        );

        when(sellerRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(seller));

        when(passwordEncoder.matches(PASSWORD, HASH))
                .thenReturn(true);

        when(jwtTokenService.generateAccessToken(seller.getId(), EMAIL))
                .thenReturn(TOKEN);

        LoginResponse response = authenticationService.login(
                new LoginRequest(EMAIL, PASSWORD)
        );

        assertEquals(TOKEN, response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());

        verify(passwordEncoder).matches(PASSWORD, HASH);
        verify(jwtTokenService).generateAccessToken(seller.getId(), EMAIL);
    }

    @Test
    void shouldRejectIncorrectPassword() {
        SellerAccount seller = new SellerAccount(
                "Example Business",
                EMAIL,
                HASH
        );

        when(sellerRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(seller));

        when(passwordEncoder.matches(PASSWORD, HASH))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(EMAIL, PASSWORD)
                )
        );

        verify(jwtTokenService, never())
                .generateAccessToken(any(UUID.class), anyString());
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(sellerRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(EMAIL, PASSWORD)
                )
        );

        verifyNoInteractions(passwordEncoder, jwtTokenService);
    }

    @Test
    void shouldRejectSuspendedSeller() throws Exception {
        SellerAccount seller = new SellerAccount(
                "Example Business",
                EMAIL,
                HASH
        );

        setSellerStatus(seller, SellerStatus.SUSPENDED);

        when(sellerRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(seller));

        when(passwordEncoder.matches(PASSWORD, HASH))
                .thenReturn(true);

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(EMAIL, PASSWORD)
                )
        );

        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void shouldRejectSellerWithoutPasswordHash() {
        SellerAccount seller = new SellerAccount(
                "Example Business",
                EMAIL,
                null
        );

        when(sellerRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(seller));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(
                        new LoginRequest(EMAIL, PASSWORD)
                )
        );

        verifyNoInteractions(passwordEncoder, jwtTokenService);
    }

    private void setSellerStatus(
            SellerAccount seller,
            SellerStatus status
    ) throws Exception {
        Field statusField = SellerAccount.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(seller, status);
    }
}

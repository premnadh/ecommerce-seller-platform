package com.premnadh.sellerplatform.seller.application;

import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.exception.DuplicateSellerEmailException;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerAccountServiceTest {

    @Mock
    private SellerAccountRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SellerAccountService sellerAccountService;

    private final String rawPassword = "SecurePassword123!";
    private final String passwordHash = "$2a$12$exampleHashedPassword";

    @Test
    void shouldRegisterSellerAndHashPassword() {
        when(sellerRepository.existsByEmailIgnoreCase("seller@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(passwordHash);

        when(sellerRepository.save(any(SellerAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerAccount result = sellerAccountService.registerSeller(
                "  Example Business  ",
                " SELLER@EXAMPLE.COM ",
                rawPassword
        );

        assertNotNull(result.getId());
        assertEquals("Example Business", result.getBusinessName());
        assertEquals("seller@example.com", result.getEmail());
        assertEquals(passwordHash, result.getPasswordHash());

        verify(passwordEncoder).encode(rawPassword);
        verify(sellerRepository).save(any(SellerAccount.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(sellerRepository.existsByEmailIgnoreCase("seller@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateSellerEmailException.class,
                () -> sellerAccountService.registerSeller(
                        "Example Business",
                        "SELLER@EXAMPLE.COM",
                        rawPassword
                )
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(sellerRepository, never()).save(any(SellerAccount.class));
    }

    @Test
    void shouldNotStoreRawPassword() {
        when(sellerRepository.existsByEmailIgnoreCase("seller@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(rawPassword))
                .thenReturn(passwordHash);

        when(sellerRepository.save(any(SellerAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerAccount result = sellerAccountService.registerSeller(
                "Example Business",
                "seller@example.com",
                rawPassword
        );

        assertNotEquals(rawPassword, result.getPasswordHash());
        assertEquals(passwordHash, result.getPasswordHash());
    }
}

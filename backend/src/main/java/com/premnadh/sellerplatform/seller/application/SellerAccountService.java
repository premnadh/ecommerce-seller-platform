package com.premnadh.sellerplatform.seller.application;

import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.exception.DuplicateSellerEmailException;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SellerAccountService {

    private final SellerAccountRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    public SellerAccountService(
            SellerAccountRepository sellerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SellerAccount registerSeller(
            String businessName,
            String email,
            String password
    ) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (sellerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateSellerEmailException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(password);

        SellerAccount seller = new SellerAccount(
                businessName.trim(),
                normalizedEmail,
                passwordHash
        );

        return sellerRepository.save(seller);
    }
}

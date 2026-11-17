package com.premnadh.sellerplatform.seller.application;

import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import com.premnadh.sellerplatform.seller.exception.DuplicateSellerEmailException;
import com.premnadh.sellerplatform.seller.repository.SellerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SellerAccountService {

    private final SellerAccountRepository sellerRepository;

    public SellerAccountService(SellerAccountRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public SellerAccount registerSeller(String businessName, String email) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (sellerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateSellerEmailException(normalizedEmail);
        }

        SellerAccount seller = new SellerAccount(
                businessName.trim(),
                normalizedEmail
        );

        return sellerRepository.save(seller);
    }
}
package com.premnadh.sellerplatform.seller.api;

import com.premnadh.sellerplatform.seller.api.dto.CreateSellerRequest;
import com.premnadh.sellerplatform.seller.api.dto.SellerResponse;
import com.premnadh.sellerplatform.seller.application.SellerAccountService;
import com.premnadh.sellerplatform.seller.domain.SellerAccount;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/sellers")
public class SellerAccountController {

    private final SellerAccountService sellerAccountService;

    public SellerAccountController(
            SellerAccountService sellerAccountService
    ) {
        this.sellerAccountService = sellerAccountService;
    }

    @PostMapping
    public ResponseEntity<SellerResponse> registerSeller(
            @Valid @RequestBody CreateSellerRequest request
    ) {
        SellerAccount seller = sellerAccountService.registerSeller(
                request.businessName(),
                request.email()
        );

        SellerResponse response = SellerResponse.from(seller);

        return ResponseEntity
                .created(URI.create("/api/v1/sellers/" + seller.getId()))
                .body(response);
    }
}
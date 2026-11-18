package com.premnadh.sellerplatform.security.api;

import com.premnadh.sellerplatform.security.api.dto.LoginRequest;
import com.premnadh.sellerplatform.security.api.dto.LoginResponse;
import com.premnadh.sellerplatform.security.application.AuthenticationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authenticationService.login(request);

        return ResponseEntity.ok(response);
    }
}

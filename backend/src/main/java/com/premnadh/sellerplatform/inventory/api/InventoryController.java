package com.premnadh.sellerplatform.inventory.api;

import com.premnadh.sellerplatform.inventory.api.dto.AdjustStockRequest;
import com.premnadh.sellerplatform.inventory.api.dto.CreateInventoryRequest;
import com.premnadh.sellerplatform.inventory.api.dto.InventoryMovementResponse;
import com.premnadh.sellerplatform.inventory.api.dto.InventoryResponse;
import com.premnadh.sellerplatform.inventory.api.dto.ReleaseStockRequest;
import com.premnadh.sellerplatform.inventory.api.dto.ReserveStockRequest;
import com.premnadh.sellerplatform.inventory.application.InventoryMovementQueryService;
import com.premnadh.sellerplatform.inventory.application.InventoryService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryMovementQueryService movementQueryService;

    public InventoryController(
            InventoryService inventoryService,
            InventoryMovementQueryService movementQueryService
    ) {
        this.inventoryService = inventoryService;
        this.movementQueryService = movementQueryService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<InventoryResponse> initialiseInventory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var inventory = inventoryService.initialiseInventory(
                sellerId,
                productId,
                request.initialQuantity(),
                request.reorderLevel()
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/inventory/" + productId
                ))
                .body(InventoryResponse.from(inventory));
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getSellerInventory(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        List<InventoryResponse> inventory = inventoryService
                .getSellerInventory(sellerId)
                .stream()
                .map(InventoryResponse::from)
                .toList();

        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockInventory(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        List<InventoryResponse> inventory = inventoryService
                .getLowStockInventory(sellerId)
                .stream()
                .map(InventoryResponse::from)
                .toList();

        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var inventory = inventoryService.getInventory(
                sellerId,
                productId
        );

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PatchMapping("/{productId}/adjustments")
    public ResponseEntity<InventoryResponse> adjustStock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody AdjustStockRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var inventory = inventoryService.adjustStock(
                sellerId,
                productId,
                request.quantityDelta(),
                request.reason()
        );

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PostMapping("/{productId}/reservations")
    public ResponseEntity<InventoryResponse> reserveStock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody ReserveStockRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var inventory = inventoryService.reserveStock(
                sellerId,
                productId,
                request.quantity(),
                request.orderReference()
        );

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PostMapping("/{productId}/releases")
    public ResponseEntity<InventoryResponse> releaseStock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody ReleaseStockRequest request
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        var inventory = inventoryService.releaseStock(
                sellerId,
                productId,
                request.quantity(),
                request.orderReference()
        );

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @GetMapping("/{productId}/movements")
    public ResponseEntity<List<InventoryMovementResponse>> getProductMovements(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        UUID sellerId = getAuthenticatedSellerId(jwt);

        List<InventoryMovementResponse> movements = movementQueryService
                .getProductMovements(sellerId, productId)
                .stream()
                .map(InventoryMovementResponse::from)
                .toList();

        return ResponseEntity.ok(movements);
    }

    private UUID getAuthenticatedSellerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

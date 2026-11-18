package com.premnadh.sellerplatform.inventory.api;

import com.premnadh.sellerplatform.inventory.application.InventoryMovementQueryService;
import com.premnadh.sellerplatform.inventory.application.InventoryNotFoundException;
import com.premnadh.sellerplatform.inventory.application.InventoryService;
import com.premnadh.sellerplatform.inventory.domain.InventoryItem;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovementType;
import com.premnadh.sellerplatform.security.config.SecurityConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import({
        InventoryExceptionHandler.class,
        SecurityConfiguration.class
})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private InventoryMovementQueryService movementQueryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private UUID sellerId;
    private UUID productId;
    private UUID orderReference;
    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderReference = UUID.randomUUID();

        inventoryItem = new InventoryItem(
                sellerId,
                productId,
                20,
                5
        );
    }

    private RequestPostProcessor sellerJwt() {
        return jwt().jwt(token ->
                token.subject(sellerId.toString())
        );
    }

    @Test
    void shouldRejectUnauthenticatedInventoryRequests() throws Exception {
        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(inventoryService);
        verifyNoInteractions(movementQueryService);
    }

    @Test
    void shouldInitialiseInventoryAndReturn201() throws Exception {
        when(inventoryService.initialiseInventory(
                sellerId,
                productId,
                20,
                5
        )).thenReturn(inventoryItem);

        mockMvc.perform(post("/api/v1/inventory/{productId}", productId)
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "initialQuantity": 20,
                                    "reorderLevel": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/inventory/" + productId
                ))
                .andExpect(jsonPath("$.productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.quantityOnHand").value(20))
                .andExpect(jsonPath("$.quantityReserved").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(20))
                .andExpect(jsonPath("$.reorderLevel").value(5))
                .andExpect(jsonPath("$.lowStock").value(false));

        verify(inventoryService).initialiseInventory(
                sellerId,
                productId,
                20,
                5
        );
    }

    @Test
    void shouldRejectInvalidInventoryInitialisationRequest()
            throws Exception {

        mockMvc.perform(post("/api/v1/inventory/{productId}", productId)
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "initialQuantity": -5,
                                    "reorderLevel": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldReturnInventoryForAuthenticatedSeller()
            throws Exception {

        when(inventoryService.getSellerInventory(sellerId))
                .thenReturn(List.of(inventoryItem));

        mockMvc.perform(get("/api/v1/inventory")
                        .with(sellerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerId")
                        .value(sellerId.toString()))
                .andExpect(jsonPath("$[0].productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$[0].quantityOnHand").value(20));

        verify(inventoryService).getSellerInventory(sellerId);
    }

    @Test
    void shouldReturnLowStockInventoryForAuthenticatedSeller()
            throws Exception {

        when(inventoryService.getLowStockInventory(sellerId))
                .thenReturn(List.of(inventoryItem));

        mockMvc.perform(get("/api/v1/inventory/low-stock")
                        .with(sellerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId")
                        .value(productId.toString()));

        verify(inventoryService).getLowStockInventory(sellerId);
    }

    @Test
    void shouldUseAuthenticatedSellerIdentityWhenGettingInventory()
            throws Exception {

        when(inventoryService.getInventory(sellerId, productId))
                .thenReturn(inventoryItem);

        mockMvc.perform(get(
                        "/api/v1/inventory/{productId}",
                        productId
                ).with(sellerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.availableQuantity").value(20));

        verify(inventoryService).getInventory(sellerId, productId);
    }

    @Test
    void shouldReturn404WhenInventoryDoesNotExist()
            throws Exception {

        when(inventoryService.getInventory(sellerId, productId))
                .thenThrow(new InventoryNotFoundException(
                        "Inventory not found"
                ));

        mockMvc.perform(get(
                        "/api/v1/inventory/{productId}",
                        productId
                ).with(sellerJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldAdjustStockAndReturn200() throws Exception {
        when(inventoryService.adjustStock(
                sellerId,
                productId,
                10,
                "Restock"
        )).thenReturn(inventoryItem);

        mockMvc.perform(patch(
                        "/api/v1/inventory/{productId}/adjustments",
                        productId
                )
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantityDelta": 10,
                                    "reason": "Restock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId")
                        .value(productId.toString()));

        verify(inventoryService).adjustStock(
                sellerId,
                productId,
                10,
                "Restock"
        );
    }

    @Test
    void shouldRejectInvalidStockAdjustmentRequest()
            throws Exception {

        mockMvc.perform(patch(
                        "/api/v1/inventory/{productId}/adjustments",
                        productId
                )
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantityDelta": 10,
                                    "reason": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldReserveStockAndReturn200() throws Exception {
        when(inventoryService.reserveStock(
                sellerId,
                productId,
                4,
                orderReference
        )).thenReturn(inventoryItem);

        mockMvc.perform(post(
                        "/api/v1/inventory/{productId}/reservations",
                        productId
                )
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 4,
                                    "orderReference": "%s"
                                }
                                """.formatted(orderReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId")
                        .value(productId.toString()));

        verify(inventoryService).reserveStock(
                sellerId,
                productId,
                4,
                orderReference
        );
    }

    @Test
    void shouldReleaseStockAndReturn200() throws Exception {
        when(inventoryService.releaseStock(
                sellerId,
                productId,
                4,
                orderReference
        )).thenReturn(inventoryItem);

        mockMvc.perform(post(
                        "/api/v1/inventory/{productId}/releases",
                        productId
                )
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 4,
                                    "orderReference": "%s"
                                }
                                """.formatted(orderReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId")
                        .value(productId.toString()));

        verify(inventoryService).releaseStock(
                sellerId,
                productId,
                4,
                orderReference
        );
    }

    @Test
    void shouldReturn409WhenStockIsInsufficient()
            throws Exception {

        when(inventoryService.reserveStock(
                sellerId,
                productId,
                100,
                orderReference
        )).thenThrow(new IllegalStateException(
                "Insufficient stock"
        ));

        mockMvc.perform(post(
                        "/api/v1/inventory/{productId}/reservations",
                        productId
                )
                        .with(sellerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 100,
                                    "orderReference": "%s"
                                }
                                """.formatted(orderReference)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnInventoryMovementHistory()
            throws Exception {

        InventoryMovement movement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.STOCK_ADJUSTMENT,
                10,
                0,
                "Restock",
                null
        );

        when(movementQueryService.getProductMovements(
                sellerId,
                productId
        )).thenReturn(List.of(movement));

        mockMvc.perform(get(
                        "/api/v1/inventory/{productId}/movements",
                        productId
                ).with(sellerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$[0].movementType")
                        .value("STOCK_ADJUSTMENT"))
                .andExpect(jsonPath("$[0].quantityOnHandDelta").value(10))
                .andExpect(jsonPath("$[0].quantityReservedDelta").value(0))
                .andExpect(jsonPath("$[0].reason").value("Restock"));

        verify(movementQueryService).getProductMovements(
                sellerId,
                productId
        );
    }
}

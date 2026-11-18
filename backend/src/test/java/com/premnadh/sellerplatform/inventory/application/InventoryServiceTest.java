package com.premnadh.sellerplatform.inventory.application;

import com.premnadh.sellerplatform.inventory.domain.InventoryItem;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovementType;
import com.premnadh.sellerplatform.inventory.repository.InventoryItemRepository;
import com.premnadh.sellerplatform.inventory.repository.InventoryMovementRepository;
import com.premnadh.sellerplatform.product.application.ProductNotFoundException;
import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryRepository;

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID sellerId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = mock(Product.class);
    }

    @Test
    void shouldInitialiseInventoryAndRecordInitialMovement() {
        when(product.getId()).thenReturn(productId);
        when(productRepository.findByIdAndSellerId(productId, sellerId))
                .thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(productId))
                .thenReturn(false);
        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.initialiseInventory(
                sellerId,
                productId,
                100,
                10
        );

        assertEquals(100, result.getQuantityOnHand());
        assertEquals(0, result.getQuantityReserved());
        assertEquals(10, result.getReorderLevel());

        ArgumentCaptor<InventoryMovement> movementCaptor =
                ArgumentCaptor.forClass(InventoryMovement.class);

        verify(movementRepository).save(movementCaptor.capture());

        InventoryMovement movement = movementCaptor.getValue();

        assertEquals(
                InventoryMovementType.INITIAL_STOCK,
                movement.getMovementType()
        );
        assertEquals(100, movement.getQuantityOnHandDelta());
        assertEquals(0, movement.getQuantityReservedDelta());
    }

    @Test
    void shouldRejectInventoryForNonexistentProduct() {
        when(productRepository.findByIdAndSellerId(productId, sellerId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> inventoryService.initialiseInventory(
                        sellerId, productId, 100, 10
                )
        );

        verify(inventoryRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateInventoryInitialisation() {
        when(productRepository.findByIdAndSellerId(productId, sellerId))
                .thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(productId))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> inventoryService.initialiseInventory(
                        sellerId, productId, 100, 10
                )
        );

        verify(inventoryRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldNotRecordMovementForZeroInitialStock() {
        when(product.getId()).thenReturn(productId);
        when(productRepository.findByIdAndSellerId(productId, sellerId))
                .thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(productId))
                .thenReturn(false);
        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.initialiseInventory(
                sellerId,
                productId,
                0,
                5
        );

        assertEquals(0, result.getQuantityOnHand());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldAdjustStockAndRecordMovement() {
        InventoryItem inventory = new InventoryItem(
                sellerId,
                productId,
                100,
                10
        );

        when(inventoryRepository.findBySellerIdAndProductId(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(inventoryRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = inventoryService.adjustStock(
                sellerId,
                productId,
                -10,
                "Damaged stock"
        );

        assertEquals(90, result.getQuantityOnHand());

        ArgumentCaptor<InventoryMovement> movementCaptor =
                ArgumentCaptor.forClass(InventoryMovement.class);

        verify(movementRepository).save(movementCaptor.capture());

        InventoryMovement movement = movementCaptor.getValue();

        assertEquals(
                InventoryMovementType.STOCK_ADJUSTMENT,
                movement.getMovementType()
        );
        assertEquals(-10, movement.getQuantityOnHandDelta());
        assertEquals("Damaged stock", movement.getReason());
    }

    @Test
    void shouldRejectAdjustmentWhenInventoryDoesNotExist() {
        when(inventoryRepository.findBySellerIdAndProductId(
                sellerId, productId
        )).thenReturn(Optional.empty());

        assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.adjustStock(
                        sellerId, productId, 10, "Restock"
                )
        );

        verify(inventoryRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldNotRecordMovementWhenAdjustmentIsInvalid() {
        InventoryItem inventory = new InventoryItem(
                sellerId,
                productId,
                10,
                2
        );

        when(inventoryRepository.findBySellerIdAndProductId(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        assertThrows(
                IllegalStateException.class,
                () -> inventoryService.adjustStock(
                        sellerId, productId, -11, "Invalid adjustment"
                )
        );

        verify(inventoryRepository, never()).save(any());
        verify(movementRepository, never()).save(any());

        assertEquals(10, inventory.getQuantityOnHand());
    }

    @Test
    void shouldRetrieveLowStockInventoryForSeller() {
        inventoryService.getLowStockInventory(sellerId);

        verify(inventoryRepository).findLowStockBySellerId(sellerId);
    }
}

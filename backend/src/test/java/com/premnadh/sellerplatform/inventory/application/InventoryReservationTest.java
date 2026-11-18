package com.premnadh.sellerplatform.inventory.application;

import com.premnadh.sellerplatform.inventory.domain.InventoryItem;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovement;
import com.premnadh.sellerplatform.inventory.domain.InventoryMovementType;
import com.premnadh.sellerplatform.inventory.repository.InventoryItemRepository;
import com.premnadh.sellerplatform.inventory.repository.InventoryMovementRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryReservationTest {

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
    private UUID orderId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    void shouldReserveAvailableStock() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 20, 5
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RESERVATION,
                        orderId
                )).thenReturn(Optional.empty());

        when(inventoryRepository.reserveAvailableStock(
                sellerId, productId, 3
        )).thenReturn(1);

        when(inventoryRepository.findBySellerIdAndProductId(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        InventoryItem result = inventoryService.reserveStock(
                sellerId, productId, 3, orderId
        );

        assertEquals(20, result.getQuantityOnHand());

        verify(inventoryRepository).findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        );

        verify(inventoryRepository).reserveAvailableStock(
                sellerId, productId, 3
        );

        ArgumentCaptor<InventoryMovement> captor =
                ArgumentCaptor.forClass(InventoryMovement.class);

        verify(movementRepository).save(captor.capture());

        InventoryMovement movement = captor.getValue();

        assertEquals(
                InventoryMovementType.RESERVATION,
                movement.getMovementType()
        );
        assertEquals(0, movement.getQuantityOnHandDelta());
        assertEquals(3, movement.getQuantityReservedDelta());
        assertEquals(orderId, movement.getReferenceId());
    }

    @Test
    void shouldReturnExistingReservationWithoutReservingAgain() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 20, 5
        );

        InventoryMovement existingMovement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.RESERVATION,
                0,
                3,
                "Stock reserved for order",
                orderId
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RESERVATION,
                        orderId
                )).thenReturn(Optional.of(existingMovement));

        InventoryItem result = inventoryService.reserveStock(
                sellerId, productId, 3, orderId
        );

        assertSame(inventory, result);

        verify(inventoryRepository, never())
                .reserveAvailableStock(any(), any(), anyInt());

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectReservationRetryWithDifferentQuantity() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 20, 5
        );

        InventoryMovement existingMovement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.RESERVATION,
                0,
                3,
                "Stock reserved for order",
                orderId
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RESERVATION,
                        orderId
                )).thenReturn(Optional.of(existingMovement));

        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.reserveStock(
                        sellerId, productId, 5, orderId
                )
        );

        verify(inventoryRepository, never())
                .reserveAvailableStock(any(), any(), anyInt());

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectReservationWhenStockIsInsufficient() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 10, 2
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RESERVATION,
                        orderId
                )).thenReturn(Optional.empty());

        when(inventoryRepository.reserveAvailableStock(
                sellerId, productId, 10
        )).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> inventoryService.reserveStock(
                        sellerId, productId, 10, orderId
                )
        );

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectReservationWithNonPositiveQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.reserveStock(
                        sellerId, productId, 0, orderId
                )
        );

        verify(inventoryRepository, never())
                .findBySellerIdAndProductIdForUpdate(any(), any());

        verify(inventoryRepository, never())
                .reserveAvailableStock(any(), any(), anyInt());

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldReleaseReservedStock() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 20, 5
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RELEASE,
                        orderId
                )).thenReturn(Optional.empty());

        when(inventoryRepository.releaseReservedStock(
                sellerId, productId, 3
        )).thenReturn(1);

        when(inventoryRepository.findBySellerIdAndProductId(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        InventoryItem result = inventoryService.releaseStock(
                sellerId, productId, 3, orderId
        );

        assertEquals(20, result.getQuantityOnHand());

        verify(inventoryRepository).releaseReservedStock(
                sellerId, productId, 3
        );

        ArgumentCaptor<InventoryMovement> captor =
                ArgumentCaptor.forClass(InventoryMovement.class);

        verify(movementRepository).save(captor.capture());

        InventoryMovement movement = captor.getValue();

        assertEquals(
                InventoryMovementType.RELEASE,
                movement.getMovementType()
        );
        assertEquals(0, movement.getQuantityOnHandDelta());
        assertEquals(-3, movement.getQuantityReservedDelta());
        assertEquals(orderId, movement.getReferenceId());
    }

    @Test
    void shouldReturnExistingReleaseWithoutReleasingAgain() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 20, 5
        );

        InventoryMovement existingMovement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.RELEASE,
                0,
                -3,
                "Reserved stock released",
                orderId
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RELEASE,
                        orderId
                )).thenReturn(Optional.of(existingMovement));

        InventoryItem result = inventoryService.releaseStock(
                sellerId, productId, 3, orderId
        );

        assertSame(inventory, result);

        verify(inventoryRepository, never())
                .releaseReservedStock(any(), any(), anyInt());

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectReleaseRetryWithDifferentQuantity() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 20, 5
        );

        InventoryMovement existingMovement = new InventoryMovement(
                sellerId,
                productId,
                InventoryMovementType.RELEASE,
                0,
                -3,
                "Reserved stock released",
                orderId
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RELEASE,
                        orderId
                )).thenReturn(Optional.of(existingMovement));

        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.releaseStock(
                        sellerId, productId, 5, orderId
                )
        );

        verify(inventoryRepository, never())
                .releaseReservedStock(any(), any(), anyInt());

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectReleaseWhenReservedStockIsInsufficient() {
        InventoryItem inventory = new InventoryItem(
                sellerId, productId, 10, 2
        );

        when(inventoryRepository.findBySellerIdAndProductIdForUpdate(
                sellerId, productId
        )).thenReturn(Optional.of(inventory));

        when(movementRepository
                .findBySellerIdAndProductIdAndMovementTypeAndReferenceId(
                        sellerId,
                        productId,
                        InventoryMovementType.RELEASE,
                        orderId
                )).thenReturn(Optional.empty());

        when(inventoryRepository.releaseReservedStock(
                sellerId, productId, 5
        )).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> inventoryService.releaseStock(
                        sellerId, productId, 5, orderId
                )
        );

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectReleaseWithNonPositiveQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.releaseStock(
                        sellerId, productId, -1, orderId
                )
        );

        verify(inventoryRepository, never())
                .findBySellerIdAndProductIdForUpdate(any(), any());

        verify(inventoryRepository, never())
                .releaseReservedStock(any(), any(), anyInt());

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectReservationWithoutOrderReference() {
        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.reserveStock(
                        sellerId, productId, 3, null
                )
        );

        verify(inventoryRepository, never())
                .findBySellerIdAndProductIdForUpdate(any(), any());
    }

    @Test
    void shouldRejectReleaseWithoutOrderReference() {
        assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.releaseStock(
                        sellerId, productId, 3, null
                )
        );

        verify(inventoryRepository, never())
                .findBySellerIdAndProductIdForUpdate(any(), any());
    }
}

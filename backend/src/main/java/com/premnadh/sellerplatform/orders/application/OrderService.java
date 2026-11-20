package com.premnadh.sellerplatform.orders.application;

import com.premnadh.sellerplatform.inventory.application.InventoryService;
import com.premnadh.sellerplatform.orders.api.dto.CreateOrderItemRequest;
import com.premnadh.sellerplatform.orders.api.dto.CreateOrderRequest;
import com.premnadh.sellerplatform.orders.api.dto.OrderItemResponse;
import com.premnadh.sellerplatform.orders.api.dto.OrderResponse;
import com.premnadh.sellerplatform.orders.api.dto.PaymentAttemptResponse;
import com.premnadh.sellerplatform.orders.application.payment.PaymentGateway;
import com.premnadh.sellerplatform.orders.application.payment.PaymentResult;
import com.premnadh.sellerplatform.orders.domain.Order;
import com.premnadh.sellerplatform.orders.domain.OrderItem;
import com.premnadh.sellerplatform.orders.domain.OrderStatus;
import com.premnadh.sellerplatform.orders.domain.PaymentAttempt;
import com.premnadh.sellerplatform.orders.domain.PaymentOperation;
import com.premnadh.sellerplatform.orders.repository.OrderItemRepository;
import com.premnadh.sellerplatform.orders.repository.OrderRepository;
import com.premnadh.sellerplatform.orders.repository.PaymentAttemptRepository;
import com.premnadh.sellerplatform.product.application.ProductService;
import com.premnadh.sellerplatform.product.domain.Product;
import com.premnadh.sellerplatform.product.domain.ProductStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class OrderService {

    private static final int MAX_REQUEST_ITEMS = 50;
    private static final int MAX_ORDER_PRECISION = 12;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final PaymentGateway paymentGateway;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            ProductService productService,
            InventoryService inventoryService,
            PaymentGateway paymentGateway
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public OrderResponse createOrder(
            UUID sellerId,
            UUID idempotencyKey,
            CreateOrderRequest request
    ) {
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID is required");
        }

        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        if (request == null) {
            throw new IllegalArgumentException("Order request is required");
        }

        // A repeated key must not create or charge another order.
        var existingOrder = orderRepository
                .findBySellerIdAndIdempotencyKey(sellerId, idempotencyKey);

        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get());
        }

        Map<UUID, Integer> requestedQuantities =
                aggregateRequestedItems(request.items());

        List<Product> products = new ArrayList<>();
        String currency = null;

        // Product IDs are processed in a stable order.
        for (UUID productId : requestedQuantities.keySet()) {
            Product product = productService.getProductById(
                    sellerId,
                    productId
            );

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Product is not available for ordering: " + productId
                );
            }

            if (currency == null) {
                currency = product.getCurrency();
            } else if (!currency.equals(product.getCurrency())) {
                throw new IllegalArgumentException(
                        "An order cannot contain products with different currencies"
                );
            }

            products.add(product);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Product product : products) {
            int quantity = requestedQuantities.get(product.getId());

            OrderItem item = new OrderItem(
                    sellerId,
                    UUID.randomUUID(),
                    product.getId(),
                    product.getSku(),
                    product.getName(),
                    product.getPrice(),
                    quantity
            );

            validateMoneyPrecision(item.getLineTotal());

            subtotal = subtotal.add(item.getLineTotal());
            orderItems.add(item);
        }

        validateMoneyPrecision(subtotal);

        String orderNumber = generateOrderNumber();

        Order order = new Order(
                sellerId,
                orderNumber,
                idempotencyKey,
                request.customerName(),
                request.customerEmail(),
                currency,
                subtotal,
                subtotal
        );

        // Rebuild order items using the actual generated order ID.
        List<OrderItem> persistedItems = new ArrayList<>();

        for (OrderItem item : orderItems) {
            persistedItems.add(new OrderItem(
                    sellerId,
                    order.getId(),
                    item.getProductId(),
                    item.getSku(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity()
            ));
        }

        /*
         * Entities use pre-generated UUIDs.
         * Retain the entity returned by save(), because Spring Data JPA
         * may use merge() rather than persist().
         */
        order = orderRepository.save(order);

        orderItemRepository.saveAll(persistedItems);

        /*
         * Reserve inventory in deterministic product-ID order.
         * If a reservation fails, the transaction rolls back the
         * order, order items and previous reservations.
         */
        for (OrderItem item : persistedItems) {
            inventoryService.reserveStock(
                    sellerId,
                    item.getProductId(),
                    item.getQuantity(),
                    order.getId()
            );
        }

        PaymentAttempt paymentAttempt = new PaymentAttempt(
                sellerId,
                order.getId(),
                PaymentOperation.PAYMENT,
                order.getTotal(),
                order.getCurrency()
        );

        paymentAttempt = paymentAttemptRepository.save(paymentAttempt);

        PaymentResult paymentResult = paymentGateway.charge(
                order.getId(),
                order.getTotal(),
                order.getCurrency()
        );

        if (paymentResult == null) {
            throw new IllegalStateException(
                    "Payment gateway returned no result"
            );
        }

        if (paymentResult.successful()) {
            paymentAttempt.markSucceeded(
                    paymentResult.providerReference()
            );

            order.confirm();

        } else {
            paymentAttempt.markFailed(
                    paymentResult.failureReason()
            );

            /*
             * A declined payment is a business outcome, not an exception.
             * Record the failure and release all reserved stock.
             */
            for (OrderItem item : persistedItems) {
                inventoryService.releaseStock(
                        sellerId,
                        item.getProductId(),
                        item.getQuantity(),
                        order.getId()
                );
            }

            order.markPaymentFailed();
        }

        // Explicitly persist the final payment and order states.
        paymentAttempt = paymentAttemptRepository.save(paymentAttempt);
        order = orderRepository.save(order);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID sellerId, UUID orderId) {
        Order order = findSellerOrder(sellerId, orderId);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getSellerOrders(UUID sellerId) {
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID is required");
        }

        return orderRepository
                .findAllBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(UUID sellerId, UUID orderId) {
        Order order = findSellerOrder(sellerId, orderId);

        // Repeated cancellation must not issue another refund.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only confirmed orders can be cancelled"
            );
        }

        PaymentAttempt refundAttempt = new PaymentAttempt(
                sellerId,
                order.getId(),
                PaymentOperation.REFUND,
                order.getTotal(),
                order.getCurrency()
        );

        refundAttempt = paymentAttemptRepository.save(refundAttempt);

        PaymentResult refundResult = paymentGateway.refund(
                order.getId(),
                order.getTotal(),
                order.getCurrency()
        );

        if (refundResult == null) {
            throw new IllegalStateException(
                    "Payment gateway returned no refund result"
            );
        }

        if (!refundResult.successful()) {
            refundAttempt.markFailed(refundResult.failureReason());

            // Persist the failed refund attempt.
            refundAttempt = paymentAttemptRepository.save(refundAttempt);

            // Keep the order confirmed and stock reserved.
            order = orderRepository.save(order);

            return toResponse(order);
        }

        refundAttempt.markSucceeded(
                refundResult.providerReference()
        );

        // Persist the successful refund attempt.
        refundAttempt = paymentAttemptRepository.save(refundAttempt);

        List<OrderItem> items =
                orderItemRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                sellerId,
                                order.getId()
                        );

        // Release stock only after the refund succeeds.
        for (OrderItem item : items) {
            inventoryService.releaseStock(
                    sellerId,
                    item.getProductId(),
                    item.getQuantity(),
                    order.getId()
            );
        }

        order.cancel();

        // Explicitly persist the cancelled order.
        order = orderRepository.save(order);

        return toResponse(order);
    }

    private Map<UUID, Integer> aggregateRequestedItems(
            List<CreateOrderItemRequest> requestedItems
    ) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "An order must contain at least one item"
            );
        }

        if (requestedItems.size() > MAX_REQUEST_ITEMS) {
            throw new IllegalArgumentException(
                    "An order cannot contain more than "
                            + MAX_REQUEST_ITEMS + " request items"
            );
        }

        // TreeMap gives a deterministic product-ID ordering.
        Map<UUID, Integer> quantities = new TreeMap<>();

        for (CreateOrderItemRequest item : requestedItems) {
            if (item == null || item.productId() == null) {
                throw new IllegalArgumentException(
                        "Every order item must have a product ID"
                );
            }

            if (item.quantity() <= 0) {
                throw new IllegalArgumentException(
                        "Order item quantity must be greater than zero"
                );
            }

            try {
                quantities.merge(
                        item.productId(),
                        item.quantity(),
                        Math::addExact
                );
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Combined product quantity is too large",
                        exception
                );
            }
        }

        return quantities;
    }

    private void validateMoneyPrecision(BigDecimal amount) {
        if (amount == null
                || amount.signum() < 0
                || amount.scale() > 2
                || amount.precision() > MAX_ORDER_PRECISION) {

            throw new IllegalArgumentException(
                    "Order amount exceeds the supported monetary precision"
            );
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 32)
                .toUpperCase();
    }

    private Order findSellerOrder(UUID sellerId, UUID orderId) {
        if (sellerId == null || orderId == null) {
            throw new IllegalArgumentException(
                    "Seller ID and order ID are required"
            );
        }

        return orderRepository.findBySellerIdAndId(sellerId, orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found"
                ));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses =
                orderItemRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                order.getSellerId(),
                                order.getId()
                        )
                        .stream()
                        .map(item -> new OrderItemResponse(
                                item.getId(),
                                item.getProductId(),
                                item.getSku(),
                                item.getProductName(),
                                item.getUnitPrice(),
                                item.getQuantity(),
                                item.getLineTotal()
                        ))
                        .toList();

        List<PaymentAttemptResponse> paymentResponses =
                paymentAttemptRepository
                        .findAllBySellerIdAndOrderIdOrderByCreatedAtAsc(
                                order.getSellerId(),
                                order.getId()
                        )
                        .stream()
                        .map(payment -> new PaymentAttemptResponse(
                                payment.getId(),
                                payment.getOperation(),
                                payment.getProvider(),
                                payment.getStatus(),
                                payment.getAmount(),
                                payment.getCurrency(),
                                payment.getProviderReference(),
                                payment.getFailureReason(),
                                payment.getCreatedAt()
                        ))
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotal(),
                order.getTotal(),
                itemResponses,
                paymentResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

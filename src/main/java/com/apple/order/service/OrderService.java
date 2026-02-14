package com.apple.order.service;

import com.apple.order.dto.OrderRequest;
import com.apple.order.entity.Order;
import com.apple.order.entity.User;
import com.apple.order.exception.ApiException;
import com.apple.order.repository.OrderRepository;
import com.apple.order.repository.UserRepository;
import com.apple.order.validation.OrderValidationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * ===============================================================
 * ORDER SERVICE
 * ===============================================================
 *
 * Handles complete order lifecycle:
 *  - Create order
 *  - Fetch order
 *  - Update order (PUT)
 *  - Partial update (PATCH)
 *  - Delete order
 *
 * Business Rules:
 *  - Blocked users cannot place orders
 *  - Shipped orders cannot be updated or deleted
 *  - Cannot cancel an order after shipment
 *  - Order validation enforced before persistence
 *
 * Transaction Strategy:
 *  - All operations run within transactional boundary
 *  - Changes automatically persisted due to JPA dirty checking
 *
 * HTTP Status Mapping:
 *  - 200 OK → Success
 *  - 404 Not Found → Order/User not found
 *  - 403 Forbidden → Blocked user
 *  - 409 Conflict → Illegal state transition
 *  - 422 Unprocessable Entity → Invalid status transition
 */
@Service
@RequiredArgsConstructor
@Transactional
@Tag(name = "Order Service",
    description = "Handles order creation, retrieval, update, partial updates, and deletion " +
        "while enforcing business validation and order state transitions."
)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderValidationEngine validationEngine;

    /**
     * ===============================================================
     * Create Order
     * ===============================================================
     *
     * Flow:
     *  1. Validate order request
     *  2. Validate user existence
     *  3. Ensure user is not blocked
     *  4. Build order entity
     *  5. Persist order
     *
     * @param request order request payload
     * @return created Order entity
     */
    @Operation(
            summary = "Create new order",
            description = "Creates a new order after validating request data and ensuring " +
                    "the associated user is active (not blocked)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order created successfully"),
            @ApiResponse(responseCode = "403",
                    description = "Blocked users cannot place orders"),
            @ApiResponse(responseCode = "404",
                    description = "User not found"),
            @ApiResponse(responseCode = "422",
                    description = "Order validation failed"),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error")
    })
    public Order createOrder(OrderRequest request) {
        validationEngine.validateOrder(request);
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (user.isBlocked()) {
            throw new ApiException(
                    "Blocked users cannot place orders",
                    HttpStatus.FORBIDDEN
            );
        }
        Order order = Order.builder()
                .productName(request.getProductName())
                .amount(request.getAmount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Order.OrderStatus.CREATED)
                .paymentType(Order.PaymentType.valueOf(
                        request.getPayment().getPaymentType().name()))
                .cardNumber(request.getPayment().getCardNumber())
                .upiId(request.getPayment().getUpiId())
                .user(user)
                .build();
        return orderRepository.save(order);
    }

    /**
     * ===============================================================
     * Get Order By ID
     * ===============================================================
     *
     * Fetches order along with associated user (optimized fetch join).
     *
     * @param id order id
     * @return Order entity
     */
    @Transactional(readOnly = true)
    @Operation(
            summary = "Fetch order by ID",
            description = "Retrieves an order with associated user information. " +
                    "Throws exception if order does not exist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Order not found")
    })
    public Order getOrder(Long id) {
        return orderRepository.findOrderWithUser(id)
                .orElseThrow(() ->
                        new ApiException("Order not found", HttpStatus.NOT_FOUND));
    }

    /**
     * ===============================================================
     * Update Order (Full Update - PUT)
     * ===============================================================
     *
     * Business Rule:
     *  - Cannot update shipped orders
     *
     * @param id order id
     * @param request updated order payload
     * @return updated Order entity
     */
    @Operation(
            summary = "Update order",
            description = "Performs full update on an order. " +
                    "Shipped orders cannot be modified."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order updated successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Order not found"),
            @ApiResponse(responseCode = "409",
                    description = "Cannot update shipped order"),
            @ApiResponse(responseCode = "422",
                    description = "Validation failed")
    })
    public Order update(Long id, OrderRequest request) {
        Order order = getOrder(id);
        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            throw new ApiException(
                    "Cannot update shipped order",
                    HttpStatus.CONFLICT
            );
        }
        validationEngine.validateOrder(request);
        order.setProductName(request.getProductName());
        order.setAmount(request.getAmount());
        order.setStartDate(request.getStartDate());
        order.setEndDate(request.getEndDate());
        return order;
    }

    /**
     * ===============================================================
     * Patch Order (Partial Update)
     * ===============================================================
     *
     * Supports partial updates:
     *  - status
     *  - amount
     *
     * Business Rules:
     *  - Cannot cancel after shipment
     *
     * @param id order id
     * @param updates map of fields to update
     * @return updated Order entity
     */
    @Operation(
            summary = "Partially update order",
            description = "Applies partial updates to order fields such as status or amount. " +
                    "Prevents illegal state transitions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order patched successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Order not found"),
            @ApiResponse(responseCode = "422",
                    description = "Invalid status transition")
    })
    public Order patch(Long id, Map<String, Object> updates) {
        Order order = getOrder(id);
        if (updates.containsKey("status")) {
            Order.OrderStatus newStatus =
                    Order.OrderStatus.valueOf(updates.get("status").toString());
            if (order.getStatus() == Order.OrderStatus.SHIPPED
                    && newStatus == Order.OrderStatus.CANCELLED) {
                throw new ApiException(
                        "Cannot cancel after shipment",
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
            order.setStatus(newStatus);
        }
        if (updates.containsKey("amount")) {
            Double amount = Double.valueOf(updates.get("amount").toString());
            order.setAmount(amount);
        }
        return order;
    }

    /**
     * ===============================================================
     * Delete Order
     * ===============================================================
     *
     * Business Rule:
     *  - Cannot delete shipped orders
     *
     * @param id order id
     */
    @Operation(
            summary = "Delete order",
            description = "Deletes an order if it has not been shipped. " +
                    "Shipped orders are protected from deletion."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order deleted successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Order not found"),
            @ApiResponse(responseCode = "409",
                    description = "Cannot delete shipped order")
    })
    public void delete(Long id) {
        Order order = getOrder(id);
        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            throw new ApiException(
                    "Cannot delete shipped order",
                    HttpStatus.CONFLICT
            );
        }
        orderRepository.delete(order);
    }
}
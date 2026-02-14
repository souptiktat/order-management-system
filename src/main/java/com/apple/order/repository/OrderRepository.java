package com.apple.order.repository;

import com.apple.order.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ===============================================================
 * ORDER REPOSITORY
 * ===============================================================
 *
 * Data Access Layer for Order entity.
 *
 * Responsibilities:
 *  - CRUD operations for Order
 *  - Custom business queries
 *  - Optimized fetch strategies
 *  - Business rule validation support
 *
 * Enterprise Design:
 *  - Extends JpaRepository for standard persistence
 *  - Custom JPQL queries for optimized fetching
 *  - Business rule enforcement via query methods
 *  - Supports validation engine logic
 *
 * Database Table:
 *  - orders
 */
@Repository
@Tag(
        name = "Order Repository",
        description = "Data access layer responsible for managing Order entity persistence, " +
                "custom queries, and business rule validations."
)
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * ===============================================================
     * Fetch Orders By User ID
     * ===============================================================
     *
     * Retrieves all orders associated with a specific user.
     *
     * @param userId user identifier
     * @return list of orders for the user
     */
    @Operation(
            summary = "Find orders by user ID",
            description = "Fetches all orders associated with the specified user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    List<Order> findByUserId(Long userId);

    /**
     * ===============================================================
     * Check Order Status By ID
     * ===============================================================
     *
     * Used in business rule:
     *  "Cannot cancel order after shipment"
     *
     * @param id order ID
     * @param status order status
     * @return true if order exists with given status
     */
    @Operation(
            summary = "Check if order exists with specific status",
            description = "Validates whether an order exists with a given ID and status. " +
                    "Used for business rule enforcement such as preventing cancellation after shipment."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Validation check completed"),
            @ApiResponse(responseCode = "404",
                    description = "Order not found"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    boolean existsByIdAndStatus(Long id, Order.OrderStatus status);

    /**
     * ===============================================================
     * Fetch Orders By Status
     * ===============================================================
     *
     * Retrieves all orders with a specific status.
     *
     * Example:
     *  - CREATED
     *  - SHIPPED
     *  - CANCELLED
     *
     * @param status order status
     * @return list of matching orders
     */
    @Operation(
            summary = "Find orders by status",
            description = "Fetches all orders having the specified status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * ===============================================================
     * Fetch Order With User (Optimized)
     * ===============================================================
     *
     * Uses JOIN FETCH to:
     *  - Avoid LazyInitializationException
     *  - Reduce N+1 query problem
     *
     * @param id order ID
     * @return optional order with associated user
     */
    @Operation(
            summary = "Find order with user details",
            description = "Fetches order along with associated user using JOIN FETCH to avoid lazy loading issues."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Order with user retrieved successfully"),
            @ApiResponse(responseCode = "404",
                    description = "Order not found"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    @Query("""
           SELECT o FROM Order o
           JOIN FETCH o.user
           WHERE o.id = :id
           """)
    Optional<Order> findOrderWithUser(@Param("id") Long id);

    /**
     * ===============================================================
     * Prevent Duplicate Active Product
     * ===============================================================
     *
     * Business Rule:
     *  A user cannot create multiple active (CREATED) orders
     *  for the same product.
     *
     * This query checks whether an active product order already exists.
     *
     * @param userId user ID
     * @param product product name
     * @return true if active product already exists
     */
    @Operation(
            summary = "Check duplicate active product for user",
            description = "Validates whether a user already has an active (CREATED) order " +
                    "for the same product to prevent duplicate active orders."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Validation check completed"),
            @ApiResponse(responseCode = "422",
                    description = "Duplicate active product exists"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    @Query("""
           SELECT COUNT(o) > 0 FROM Order o
           WHERE o.user.id = :userId
           AND o.productName = :product
           AND o.status = 'CREATED'
           """)
    boolean existsActiveProductForUser(
            @Param("userId") Long userId,
            @Param("product") String product
    );
}
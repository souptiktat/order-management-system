package com.apple.order.repository;

import com.apple.order.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ===============================================================
 * USER REPOSITORY
 * ===============================================================
 *
 * Data Access Layer for User entity.
 *
 * Responsibilities:
 *  - CRUD operations for User
 *  - Email uniqueness validation
 *  - Authentication support
 *  - Business rule validation
 *  - Optimized fetch operations
 *
 * Enterprise Design:
 *  - Extends JpaRepository for standard persistence
 *  - Uses JPQL queries for optimized fetching
 *  - Supports validation engine
 *  - Supports authentication and authorization logic
 *
 * Database Table:
 *  - users
 */
@Repository
@Tag(
        name = "User Repository",
        description = "Data access layer responsible for managing User entity persistence, " +
                "authentication queries, validation checks, and business rule enforcement."
)
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ===============================================================
     * Check Email Uniqueness
     * ===============================================================
     *
     * Used in:
     *  - @UniqueEmail custom validation
     *  - Registration validation
     *
     * Ensures no duplicate email exists in database.
     *
     * @param email user email
     * @return true if email already exists
     */
    @Operation(
            summary = "Check if email already exists",
            description = "Validates whether a given email is already present in the database. " +
                    "Used for unique email validation during user registration."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Validation check completed"),
            @ApiResponse(responseCode = "409",
                    description = "Email already exists"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    boolean existsByEmail(String email);

    /**
     * ===============================================================
     * Find User By Email
     * ===============================================================
     *
     * Used in:
     *  - JWT authentication
     *  - Login process
     *
     * @param email user email
     * @return optional User entity
     */
    @Operation(
            summary = "Find user by email",
            description = "Retrieves a user by email address. " +
                    "Primarily used during authentication (JWT login process)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User found successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    Optional<User> findByEmail(String email);

    /**
     * ===============================================================
     * Check Active (Not Blocked) User
     * ===============================================================
     *
     * Business Rule:
     *  Orders can only be placed if user is not blocked.
     *
     * @param id user ID
     * @return true if user exists and is not blocked
     */
    @Operation(
            summary = "Check if user exists and is not blocked",
            description = "Validates that a user exists and is not marked as blocked. " +
                    "Used in business rule validation before order placement."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Validation check completed"),
            @ApiResponse(responseCode = "403",
                    description = "User is blocked"),
            @ApiResponse(responseCode = "404",
                    description = "User not found"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    boolean existsByIdAndBlockedFalse(Long id);

    /**
     * ===============================================================
     * Fetch User With Orders (Optimized)
     * ===============================================================
     *
     * Uses LEFT JOIN FETCH to:
     *  - Avoid LazyInitializationException
     *  - Prevent N+1 query problem
     *
     * Fetches user along with associated orders.
     *
     * @param id user ID
     * @return optional User with orders loaded
     */
    @Operation(
            summary = "Find user with associated orders",
            description = "Fetches a user along with all associated orders using JOIN FETCH " +
                    "to optimize query performance and avoid lazy loading issues."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User with orders retrieved successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    @Query("""
           SELECT u FROM User u
           LEFT JOIN FETCH u.orders
           WHERE u.id = :id
           """)
    Optional<User> findUserWithOrders(@Param("id") Long id);

    /**
     * ===============================================================
     * Fetch Credit Limit If User Is Active
     * ===============================================================
     *
     * Business Rule:
     *  - Only active (non-blocked) users can place orders.
     *  - Credit limit is required for order validation.
     *
     * Optimized Query:
     *  - Fetches only creditLimit instead of full entity
     *  - Improves performance
     *
     * @param id user ID
     * @return optional credit limit value
     */
    @Operation(
            summary = "Fetch credit limit for active user",
            description = "Retrieves the credit limit of a user only if the user is active (not blocked). " +
                    "Used for validating order amount against available credit limit."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Credit limit retrieved successfully"),
            @ApiResponse(responseCode = "403",
                    description = "User is blocked"),
            @ApiResponse(responseCode = "404",
                    description = "User not found"),
            @ApiResponse(responseCode = "500",
                    description = "Database error")
    })
    @Query("""
           SELECT u.creditLimit FROM User u
           WHERE u.id = :id AND u.blocked = false
           """)
    Optional<Double> findCreditLimitIfActive(@Param("id") Long id);
}
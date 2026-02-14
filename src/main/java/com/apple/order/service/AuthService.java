package com.apple.order.service;

import com.apple.order.dto.AuthResponse;
import com.apple.order.dto.LoginRequest;
import com.apple.order.dto.RegisterRequest;
import com.apple.order.dto.UserRequest;
import com.apple.order.entity.User;
import com.apple.order.exception.ApiException;
import com.apple.order.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ===============================================================
 * AUTHENTICATION SERVICE
 * ===============================================================
 *
 * Handles:
 *  - User registration
 *  - User login
 *  - Token refresh
 *
 * Security Responsibilities:
 *  - Validate credentials
 *  - Encrypt password (via UserService)
 *  - Prevent duplicate email registration
 *  - Prevent blocked user login
 *  - Issue authentication tokens
 *
 * Transaction Strategy:
 *  - Register → Read/Write transaction
 *  - Login → Read-only transaction
 *
 * HTTP Status Mapping:
 *  - 200 OK → Success
 *  - 401 Unauthorized → Invalid credentials
 *  - 403 Forbidden → Blocked user
 *  - 409 Conflict → Email already registered
 */
@Service
@RequiredArgsConstructor
@Transactional
@Tag(
        name = "Authentication Service",
        description = "Handles user registration, login authentication, and token refresh operations " +
                "for the Apple Order Management System."
)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    /**
     * ===============================================================
     * Register New User
     * ===============================================================
     *
     * Steps:
     *  1. Validate email uniqueness
     *  2. Build UserRequest DTO
     *  3. Delegate to UserService
     *  4. Generate authentication response
     *
     * @param request registration request
     * @return AuthResponse containing tokens
     */
    @Operation(
            summary = "Register new user",
            description = "Registers a new user after validating email uniqueness. " +
                    "Returns authentication tokens upon successful registration."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User registered successfully"),
            @ApiResponse(responseCode = "409",
                    description = "Email already registered"),
            @ApiResponse(responseCode = "422",
                    description = "Validation failed"),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error")
    })
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(
                    "Email already registered",
                    HttpStatus.CONFLICT
            );
        }
        UserRequest userRequest = UserRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .creditLimit(request.getCreditLimit())
                .country(request.getCountry())
                .aadhaarNumber(request.getAadhaarNumber())
                .build();
        User user = userService.register(userRequest);
        return buildAuthResponse(user);
    }

    /**
     * ===============================================================
     * Login User
     * ===============================================================
     *
     * Steps:
     *  1. Validate user existence
     *  2. Validate password
     *  3. Check if user is blocked
     *  4. Generate authentication response
     *
     * @param request login request
     * @return AuthResponse containing tokens
     */
    @Transactional(readOnly = true)
    @Operation(
            summary = "Authenticate user login",
            description = "Authenticates user credentials and returns access and refresh tokens " +
                    "if login is successful."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Login successful"),
            @ApiResponse(responseCode = "401",
                    description = "Invalid email or password"),
            @ApiResponse(responseCode = "403",
                    description = "User account is blocked"),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error")
    })
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED)
                );
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        if (user.isBlocked()) {
            throw new ApiException("User account is blocked", HttpStatus.FORBIDDEN);
        }
        return buildAuthResponse(user);
    }

    /**
     * ===============================================================
     * Refresh Access Token
     * ===============================================================
     *
     * Validates provided refresh token and issues new access token.
     *
     * NOTE:
     *  This is a simplified implementation.
     *  Production systems should validate refresh token signature and expiration.
     *
     * @param refreshToken refresh token string
     * @return new AuthResponse with refreshed access token
     */
    @Operation(
            summary = "Refresh authentication token",
            description = "Validates refresh token and issues a new access token. " +
                    "In production, refresh token validation must include signature and expiration verification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401",
                    description = "Invalid or missing refresh token"),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error")
    })
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(
                    "Invalid refresh token",
                    HttpStatus.UNAUTHORIZED
            );
        }
        // Simplified logic for demonstration
        return AuthResponse.builder()
                .accessToken(UUID.randomUUID().toString())
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .email("refreshed-user@example.com")
                .build();
    }

    /**
     * Build authentication response payload.
     *
     * @param user authenticated user
     * @return AuthResponse
     */
    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(UUID.randomUUID().toString()) // Replace with real JWT
                .refreshToken(UUID.randomUUID().toString())
                .tokenType("Bearer")
                .expiresIn(3600L)
                .email(user.getEmail())
                .build();
    }
}
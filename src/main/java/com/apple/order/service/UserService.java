package com.apple.order.service;

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

import java.util.List;

/**
 * ===============================================================
 * USER SERVICE
 * ===============================================================
 *
 * Handles:
 *  - User registration
 *  - Admin user creation
 *  - User retrieval
 *  - User updates
 *  - Blocking users
 *  - Deleting users
 *
 * Business Rules:
 *  - Email must be unique
 *  - Indian users must provide Aadhaar number
 *  - Passwords must be stored encrypted
 *  - Blocked users remain in system (soft restriction)
 *
 * Transaction Strategy:
 *  - Write operations run in default transaction
 *  - Read-only operations optimized with readOnly flag
 *
 * HTTP Status Mapping:
 *  - 200 OK → Success
 *  - 400 Bad Request → Validation failure
 *  - 404 Not Found → User not found
 *  - 409 Conflict → Email already exists
 */
@Service
@RequiredArgsConstructor
@Transactional
@Tag(
        name = "User Service",
        description = "Manages user lifecycle including registration, retrieval, updates, " +
                "blocking, and deletion while enforcing business validation rules."
)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ===============================================================
     * Public Registration
     * ===============================================================
     *
     * Used by authentication layer.
     *
     * Business Rules:
     *  - Aadhaar required for Indian users
     *  - Email must be unique
     *
     * @param request user registration request
     * @return saved User entity
     */
    @Operation(
            summary = "Register new user",
            description = "Registers a new user after validating country-specific rules " +
                    "and ensuring email uniqueness."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User registered successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Aadhaar required for Indian users"),
            @ApiResponse(responseCode = "409",
                    description = "Email already exists")
    })
    public User register(UserRequest request) {
        validateIndianUser(request);
        validateEmailUniqueness(request.getEmail(), null);
        User user = buildUserEntity(request);
        return userRepository.save(user);
    }

    /**
     * ===============================================================
     * Admin Create User
     * ===============================================================
     *
     * Same validation rules as public registration.
     *
     * @param request user creation request
     * @return saved User entity
     */
    @Operation(
            summary = "Admin create user",
            description = "Allows administrators to create users while enforcing the same " +
                    "validation rules as public registration."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User created successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Aadhaar required for Indian users"),
            @ApiResponse(responseCode = "409",
                    description = "Email already exists")
    })
    public User createUser(UserRequest request) {
        validateIndianUser(request);
        validateEmailUniqueness(request.getEmail(), null);
        User user = buildUserEntity(request);
        return userRepository.save(user);
    }

    /**
     * ===============================================================
     * Get Single User
     * ===============================================================
     *
     * @param id user id
     * @return User entity
     */
    @Transactional(readOnly = true)
    @Operation(
            summary = "Fetch user by ID",
            description = "Retrieves a single user by ID. Throws exception if user does not exist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found")
    })
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    /**
     * ===============================================================
     * Get All Users
     * ===============================================================
     *
     * @return list of users
     */
    @Transactional(readOnly = true)
    @Operation(
            summary = "Fetch all users",
            description = "Returns list of all registered users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Users retrieved successfully")
    })
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * ===============================================================
     * Update User
     * ===============================================================
     *
     * Business Rules:
     *  - Email uniqueness must be maintained
     *  - Aadhaar required for Indian users
     *  - Password updated only if provided
     *
     * @param id user id
     * @param request updated user data
     * @return updated User entity
     */
    @Operation(
            summary = "Update user",
            description = "Updates user information while enforcing email uniqueness " +
                    "and country-specific validation rules."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User updated successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Aadhaar required for Indian users"),
            @ApiResponse(responseCode = "404",
                    description = "User not found"),
            @ApiResponse(responseCode = "409",
                    description = "Email already exists")
    })
    public User updateUser(Long id, UserRequest request) {
        User existingUser = getUser(id);
        validateIndianUser(request);
        validateEmailUniqueness(request.getEmail(), id);
        existingUser.setName(request.getName());
        existingUser.setEmail(request.getEmail());
        existingUser.setCreditLimit(request.getCreditLimit());
        existingUser.setCountry(request.getCountry());
        existingUser.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existingUser.setPassword(
                    passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(existingUser);
    }

    /**
     * ===============================================================
     * Block User
     * ===============================================================
     *
     * Soft restriction mechanism.
     * Blocked users cannot authenticate or place orders.
     *
     * @param id user id
     */
    @Operation(
            summary = "Block user",
            description = "Marks user as blocked. Blocked users cannot log in or place orders."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User blocked successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found")
    })
    public void blockUser(Long id) {
        User user = getUser(id);
        user.setBlocked(true);
    }

    /**
     * ===============================================================
     * Delete User
     * ===============================================================
     *
     * @param id user id
     */
    @Operation(
            summary = "Delete user",
            description = "Deletes user permanently if exists."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User deleted successfully"),
            @ApiResponse(responseCode = "404",
                    description = "User not found")
    })
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    // ==============================
    // Private Helper Methods
    // ==============================
    private void validateIndianUser(UserRequest request) {
        if ("INDIA".equalsIgnoreCase(request.getCountry())
                && request.getAadhaarNumber() == null) {
            throw new ApiException(
                    "Aadhaar number required for Indian users",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateEmailUniqueness(String email, Long userId) {
        userRepository.findByEmail(email)
            .ifPresent(existing -> {
                if (userId == null || !existing.getId().equals(userId)) {
                    throw new ApiException(
                            "Email already exists",
                            HttpStatus.CONFLICT
                    );
                }
            });
    }

    private User buildUserEntity(UserRequest request) {
        return User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .creditLimit(request.getCreditLimit())
            .country(request.getCountry())
            .aadhaarNumber(request.getAadhaarNumber())
            .blocked(false)
            .build();
    }
}
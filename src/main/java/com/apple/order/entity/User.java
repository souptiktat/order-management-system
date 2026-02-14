package com.apple.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "User",
        description = "Represents a system user who can place orders. " +
                "Contains authentication details, credit limit configuration, " +
                "regional compliance information, and account status."
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
            description = "Unique identifier of the user",
            example = "101",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Column(nullable = false, length = 100)
    @Schema(
            description = "Full name of the user",
            example = "John Doe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Column(nullable = false, length = 150)
    @Schema(
            description = "Unique email address used for login and communication",
            example = "john.doe@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Column(nullable = false)
    @Schema(
            description = "Encrypted password of the user. Never returned in API responses.",
            example = "$2a$10$E9fXKJ8pTz1YlP9o1bQHBe",
            accessMode = Schema.AccessMode.WRITE_ONLY,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    @Column(nullable = false)
    @Schema(
            description = "Maximum credit limit allocated to the user for placing orders",
            example = "50000.0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Double creditLimit;

    @Column(nullable = false)
    @Schema(
            description = "Country of residence of the user. " +
                    "Used for regulatory validations (e.g., Aadhaar for India).",
            example = "INDIA",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String country;

    @Column(length = 12)
    @Schema(
            description = "Aadhaar number required for Indian users as per compliance rules. " +
                    "Optional for other countries.",
            example = "123412341234"
    )
    private String aadhaarNumber;

    @Column(nullable = false)
    @Schema(
            description = "Indicates whether the user account is blocked. " +
                    "Blocked users cannot log in or place orders.",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private boolean blocked = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Schema(
            description = "List of orders placed by the user. " +
                    "Loaded lazily and typically excluded from user listing APIs.",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private List<Order> orders;
}
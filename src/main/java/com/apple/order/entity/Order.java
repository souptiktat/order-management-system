package com.apple.order.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "Order",
        description = "Represents a purchase order placed by a user. " +
                "Contains product details, payment information, duration period, " +
                "and lifecycle status of the order."
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
            description = "Unique identifier of the order",
            example = "1001",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Column(nullable = false)
    @Schema(
            description = "Name of the product being purchased",
            example = "iPhone 15 Pro",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String productName;

    @Column(nullable = false)
    @Schema(
            description = "Total monetary amount of the order. " +
                    "Must not exceed the user's credit limit.",
            example = "99999.99",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Double amount;

    @Column(nullable = false)
    @Schema(
            description = "Start date of the order validity or subscription period",
            example = "2026-01-01",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate startDate;

    @Column(nullable = false)
    @Schema(
            description = "End date of the order validity or subscription period. " +
                    "Must be greater than start date.",
            example = "2026-12-31",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(
            description = "Current lifecycle status of the order",
            example = "CREATED",
            allowableValues = {"CREATED", "SHIPPED", "CANCELLED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(
            description = "Payment method used for this order",
            example = "CREDIT_CARD",
            allowableValues = {"CREDIT_CARD", "UPI", "CASH"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentType paymentType;

    @Schema(
            description = "Credit card number used when payment type is CREDIT_CARD. " +
                    "Required only for credit card payments.",
            example = "4111111111111111",
            accessMode = Schema.AccessMode.WRITE_ONLY
    )
    private String cardNumber;

    @Schema(
            description = "UPI ID used when payment type is UPI. " +
                    "Required only for UPI payments.",
            example = "john@upi",
            accessMode = Schema.AccessMode.WRITE_ONLY
    )
    private String upiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(
            description = "User who placed the order",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private User user;

    // ================= ENUMS =================

    @Schema(description = "Represents lifecycle state of an order")
    public enum OrderStatus {
        @Schema(description = "Order has been created but not yet shipped")
        CREATED,
        @Schema(description = "Order has been shipped to the user")
        SHIPPED,
        @Schema(description = "Order has been cancelled before shipment")
        CANCELLED
    }

    @Schema(description = "Supported payment methods for an order")
    public enum PaymentType {
        @Schema(description = "Payment made using a credit card")
        CREDIT_CARD,
        @Schema(description = "Payment made using UPI (Unified Payments Interface)")
        UPI,
        @Schema(description = "Payment made using cash")
        CASH
    }
}
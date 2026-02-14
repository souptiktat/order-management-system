package com.apple.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "Order creation request payload")
@Data
public class OrderRequest {

    @NotNull(message = "User ID is required")
    @Schema(example = "1")
    private Long userId;

    @NotBlank(message = "Product name is required")
    @Schema(example = "MacBook Pro M3")
    private String productName;

    @Positive(message = "Amount must be positive")
    @Schema(example = "2500")
    private Double amount;

    @NotNull(message = "Start date required")
    @Schema(example = "2026-02-10")
    private LocalDate startDate;

    @NotNull(message = "End date required")
    @Schema(example = "2026-02-15")
    private LocalDate endDate;

    @Valid
    @NotNull(message = "Payment details required")
    private PaymentRequest payment;
}

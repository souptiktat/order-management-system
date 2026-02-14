package com.apple.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(description = "Payment details for order")
@Data
public class PaymentRequest {

    @NotNull(message = "Payment type is required")
    @Schema(example = "CREDIT_CARD", allowableValues = {"CREDIT_CARD", "UPI", "CASH"})
    private PaymentType paymentType;

    @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
    @Schema(example = "1234567812345678")
    private String cardNumber;

    @Schema(example = "UPI_ID")
    private String upiId;

    public enum PaymentType {
        CREDIT_CARD,
        UPI,
        CASH
    }
}

package com.apple.order.validation;

import com.apple.order.dto.PaymentRequest;
import com.apple.order.exception.ApiException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Conditional validation component responsible for validating
 * payment-related business rules before order processing.
 *
 * <p><b>Validation Rules:</b></p>
 * <ul>
 *     <li>If paymentType is CREDIT_CARD → cardNumber must be provided</li>
 *     <li>Other payment types may have their own conditional requirements</li>
 * </ul>
 *
 * <p><b>HTTP Behavior:</b></p>
 * <ul>
 *     <li>400 BAD_REQUEST – Missing required payment information</li>
 * </ul>
 *
 * <p>
 * This validator operates at domain level and throws {@link ApiException}
 * when a business rule violation occurs.
 * </p>
 *
 * <p>
 * Used by {@link OrderValidationEngine} as part of centralized
 * order validation workflow.
 * </p>
 */
@Slf4j
@Component
@Schema(
        name = "PaymentConditionalValidator",
        description = "Performs conditional payment validation rules " +
                "based on selected payment type before order processing."
)
public class PaymentConditionalValidator {

    /**
     * Validates conditional payment rules.
     *
     * @param request PaymentRequest DTO
     * @throws ApiException when validation fails
     */
    public void validate(PaymentRequest request) {
        if (request == null) {
            return; // Let higher-level validation handle null checks
        }
        if (request.getPaymentType() == PaymentRequest.PaymentType.CREDIT_CARD
                && request.getCardNumber() == null) {
            log.warn("Payment validation failed: card number required for CREDIT_CARD");
            throw new ApiException("Card number required", HttpStatus.BAD_REQUEST);
        }
        log.debug("Payment validation passed");
    }
}

package com.apple.order.validation;

import com.apple.order.dto.OrderRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Central validation orchestrator responsible for executing
 * all business and conditional validation rules for Order processing.
 *
 * <p>
 * This class coordinates:
 * </p>
 * <ul>
 *     <li>Business-level validations (credit limit, domain rules)</li>
 *     <li>Conditional validations (payment method requirements)</li>
 * </ul>
 *
 * <p><b>Validation Flow:</b></p>
 * <ol>
 *     <li>Validate user's credit limit against order amount</li>
 *     <li>Validate payment method based on business conditions</li>
 * </ol>
 *
 * <p><b>HTTP Behavior:</b></p>
 * <ul>
 *     <li>If validation fails → ApiException is thrown</li>
 *     <li>GlobalExceptionHandler converts exception to proper HTTP response</li>
 * </ul>
 *
 * <p><b>Possible HTTP Responses Triggered:</b></p>
 * <ul>
 *     <li>404 NOT_FOUND – User not found</li>
 *     <li>422 UNPROCESSABLE_ENTITY – Credit limit exceeded</li>
 *     <li>400 BAD_REQUEST – Invalid payment configuration</li>
 * </ul>
 *
 * <p>
 * This class ensures separation of concerns by isolating validation logic
 * from controller and service layers.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Schema(
        name = "OrderValidationEngine",
        description = "Centralized validation engine that orchestrates " +
                "business and conditional validation rules before order processing."
)
public class OrderValidationEngine {

    /**
     * Handles domain-level business validations.
     */
    private final BusinessRuleValidator businessRuleValidator;

    /**
     * Handles conditional payment-related validations.
     */
    private final PaymentConditionalValidator paymentConditionalValidator;

    /**
     * Executes all validation rules before order creation.
     *
     * @param request Order request DTO
     * @throws com.apple.order.exception.ApiException if any validation rule fails
     */
    public void validateOrder(OrderRequest request) {
        log.debug("Starting order validation for userId={}", request.getUserId());
        // Validate credit limit and user existence
        businessRuleValidator.validateCreditLimit(request.getUserId(), request.getAmount());
        // Validate payment conditions
        paymentConditionalValidator.validate(request.getPayment());
        log.debug("Order validation completed successfully for userId={}", request.getUserId());
    }
}

package com.apple.order.validation;

import com.apple.order.entity.User;
import com.apple.order.exception.ApiException;
import com.apple.order.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Validates domain-specific business rules related to Order creation.
 *
 * <p>
 * This validator ensures:
 * <ul>
 *     <li>User exists</li>
 *     <li>Order amount does not exceed user's credit limit</li>
 * </ul>
 *
 * Throws:
 * <ul>
 *     <li>404 NOT_FOUND → If user does not exist</li>
 *     <li>422 UNPROCESSABLE_ENTITY → If credit limit exceeded</li>
 * </ul>
 *
 * This class is invoked internally by the OrderService layer.
 */
@Component
@RequiredArgsConstructor
@Tag(
        name = "Business Rule Validation",
        description = "Performs domain-level validations such as credit limit checks before order creation."
)
@Schema(
        name = "BusinessRuleValidator",
        description = "Validates domain constraints before persisting order data. " +
                "Ensures financial and compliance rules are satisfied."
)
public class BusinessRuleValidator {

    private final UserRepository userRepository;

    /**
     * Validates whether the user has sufficient credit limit
     * to place an order with the given amount.
     *
     * @param userId ID of the user placing the order
     * @param amount Order amount requested
     *
     * @throws ApiException
     *      404 NOT_FOUND → if user does not exist
     *      422 UNPROCESSABLE_ENTITY → if credit limit exceeded
     */
    public void validateCreditLimit(Long userId, Double amount) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (amount > user.getCreditLimit()) {
            throw new ApiException("Credit limit exceeded", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}

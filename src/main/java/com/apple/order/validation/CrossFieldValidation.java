package com.apple.order.validation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation used for validating logical consistency
 * between two fields of a request object.
 *
 * <p><b>Example Use Case:</b></p>
 * <ul>
 *     <li>Ensuring endDate is after startDate</li>
 *     <li>Ensuring password and confirmPassword match</li>
 *     <li>Ensuring cardNumber is provided when paymentType is CREDIT_CARD</li>
 * </ul>
 *
 * <p>
 * When validation fails, a 400 BAD_REQUEST response is returned
 * via GlobalExceptionHandler with detailed field-level error messages.
 * </p>
 *
 * <p>
 * This annotation works together with {@link CrossFieldValidator}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = CrossFieldValidator.class)
@Schema(
        name = "CrossFieldValidation",
        description = "Custom validation annotation used to enforce logical " +
                "relationships between two fields in a request DTO. " +
                "Commonly used for date comparisons, password matching, or " +
                "conditional field requirements."
)
public @interface CrossFieldValidation {

    /**
     * Default validation error message returned when cross-field validation fails.
     */
    String message() default "Cross field validation failed";

    /**
     * Primary field name involved in validation.
     * Example: startDate
     */
    String field();

    /**
     * Secondary field name that must match or satisfy condition.
     * Example: endDate
     */
    String fieldMatch();

    /**
     * Validation groups for conditional validation execution.
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients of the Bean Validation API.
     */
    Class<? extends Payload>[] payload() default {};
}

package com.apple.order.validation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom validation annotation used to ensure that
 * password and confirmPassword fields match in a request DTO.
 *
 * <p><b>Typical Usage:</b></p>
 * <ul>
 *     <li>User Registration Request</li>
 *     <li>Password Reset Request</li>
 *     <li>Password Change Request</li>
 * </ul>
 *
 * <p>
 * This annotation must be applied at the class level and works
 * together with {@link PasswordMatchValidator}.
 * </p>
 *
 * <p><b>Validation Behavior:</b></p>
 * <ul>
 *     <li>If passwords match → validation passes</li>
 *     <li>If passwords do not match → validation fails</li>
 * </ul>
 *
 * <p><b>HTTP Impact:</b></p>
 * <ul>
 *     <li>On validation failure → 400 BAD_REQUEST</li>
 *     <li>Error details returned via GlobalExceptionHandler</li>
 *     <li>Error attached to confirmPassword field</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Schema(
        name = "PasswordMatch",
        description = "Custom validation annotation ensuring that password and " +
                "confirmPassword fields in a request object contain identical values."
)
public @interface PasswordMatch {

    /**
     * Default error message returned when passwords do not match.
     */
    String message() default "Passwords do not match";

    /**
     * Validation groups for conditional validation scenarios.
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients of the Bean Validation API.
     */
    Class<? extends Payload>[] payload() default {};
}

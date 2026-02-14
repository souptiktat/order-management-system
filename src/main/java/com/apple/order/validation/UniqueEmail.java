package com.apple.order.validation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom validation annotation used to ensure that
 * an email address is unique in the system.
 *
 * <p><b>Typical Usage:</b></p>
 * <ul>
 *     <li>User registration</li>
 *     <li>User profile update</li>
 * </ul>
 *
 * <p>
 * This annotation is applied at field level and works
 * together with {@link UniqueEmailValidator}.
 * </p>
 *
 * <p><b>Validation Behavior:</b></p>
 * <ul>
 *     <li>If email does not exist in database → validation passes</li>
 *     <li>If email already exists → validation fails</li>
 * </ul>
 *
 * <p><b>HTTP Impact:</b></p>
 * <ul>
 *     <li>400 BAD_REQUEST – Duplicate email detected</li>
 * </ul>
 *
 * <p>
 * Note: This validation does not replace database-level unique constraints.
 * It provides early feedback before persistence.
 * </p>
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Schema(
        name = "UniqueEmail",
        description = "Custom validation annotation that ensures the provided " +
                "email address does not already exist in the system."
)
public @interface UniqueEmail {

    /**
     * Default validation error message returned
     * when email already exists in database.
     */
    String message() default "Email already exists";

    /**
     * Validation groups for conditional validation execution.
     */
    Class<?>[] groups() default {};

    /**
     * Payload for clients of the Bean Validation API.
     */
    Class<? extends Payload>[] payload() default {};
}

package com.apple.order.validation;

import com.apple.order.dto.UserRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator implementation for {@link PasswordMatch}.
 *
 * <p>
 * Ensures that password and confirmPassword fields in {@link UserRequest}
 * contain identical values.
 * </p>
 *
 * <p><b>Validation Rules:</b></p>
 * <ul>
 *     <li>If either field is null → handled separately by @NotBlank/@NotNull</li>
 *     <li>If values match → validation passes</li>
 *     <li>If values differ → validation fails</li>
 * </ul>
 *
 * <p><b>HTTP Behavior:</b></p>
 * <ul>
 *     <li>On validation failure → 400 BAD_REQUEST</li>
 *     <li>Error response generated via GlobalExceptionHandler</li>
 *     <li>Error attached to confirmPassword field</li>
 * </ul>
 *
 * <p>
 * This validator operates at DTO level and does not contain business logic.
 * </p>
 */
@Slf4j
@Schema(
        name = "PasswordMatchValidator",
        description = "Validator that enforces matching password and confirmPassword " +
                "fields in authentication-related request DTOs."
)
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, UserRequest> {

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    /**
     * Validates password and confirmPassword equality.
     *
     * @param value   UserRequest object
     * @param context validation context
     * @return true if passwords match or are null (null handled separately)
     */
    @Override
    public boolean isValid(UserRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String password = value.getPassword();
        String confirmPassword = value.getConfirmPassword();
        if (password == null || confirmPassword == null) {
            return true; // Let @NotBlank handle null cases
        }
        boolean valid = password.equals(confirmPassword);
        if (!valid) {
            log.debug("Password validation failed: passwords do not match");
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            context.getDefaultConstraintMessageTemplate()
                    )
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return valid;
    }
}

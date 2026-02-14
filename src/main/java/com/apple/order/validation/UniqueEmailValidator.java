package com.apple.order.validation;

import com.apple.order.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator implementation for {@link UniqueEmail}.
 *
 * <p>
 * Ensures that the provided email address does not already exist
 * in the database before user creation or update.
 * </p>
 *
 * <p><b>Validation Flow:</b></p>
 * <ul>
 *     <li>Receives email value from DTO</li>
 *     <li>Queries UserRepository to check existence</li>
 *     <li>If exists → validation fails</li>
 *     <li>If not exists → validation passes</li>
 * </ul>
 *
 * <p><b>HTTP Behavior:</b></p>
 * <ul>
 *     <li>On validation failure → 400 BAD_REQUEST</li>
 *     <li>Error handled via GlobalExceptionHandler</li>
 *     <li>Error attached to email field</li>
 * </ul>
 *
 * <p>
 * Important: This validation provides early feedback but does not replace
 * database-level unique constraints. A unique index must still exist
 * to prevent race-condition conflicts.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Schema(
        name = "UniqueEmailValidator",
        description = "Validator that checks database to ensure email " +
                "address uniqueness before persisting a new user."
)
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    /**
     * Validates that email does not already exist.
     *
     * @param email the email value from DTO
     * @param context validation context
     * @return true if email is unique, false otherwise
     */
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true; // Let @NotBlank handle null/empty cases
        }
        log.debug("Checking uniqueness for email: {}", email);
        boolean exists = userRepository.existsByEmail(email);
        if (exists) {
            log.warn("Validation failed: email already exists -> {}", email);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}

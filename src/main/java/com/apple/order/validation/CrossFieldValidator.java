package com.apple.order.validation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Validator implementation for {@link CrossFieldValidation}.
 *
 * <p>
 * Performs logical comparison between two fields of a DTO object.
 * </p>
 *
 * <p><b>Common Use Cases:</b></p>
 * <ul>
 *     <li>Password and Confirm Password matching</li>
 *     <li>startDate must equal or precede endDate</li>
 *     <li>Field consistency validations</li>
 * </ul>
 *
 * <p>
 * If validation fails:
 * </p>
 * <ul>
 *     <li>Constraint violation is attached to the matched field</li>
 *     <li>GlobalExceptionHandler returns 400 BAD_REQUEST</li>
 *     <li>ErrorResponse contains validationErrors map</li>
 * </ul>
 *
 * <p>
 * Null values are ignored here and handled separately by field-level
 * annotations like {@code @NotNull}.
 * </p>
 */
@Slf4j
@Schema(
        name = "CrossFieldValidator",
        description = "Internal validator that enforces logical consistency " +
                "between two fields within a request object. " +
                "Used in combination with @CrossFieldValidation annotation."
)
public class CrossFieldValidator implements ConstraintValidator<CrossFieldValidation, Object> {

    /**
     * Primary field to validate.
     */
    private String field;

    /**
     * Secondary field that must match or satisfy condition.
     */
    private String fieldMatch;

    /**
     * Initializes validator with annotation configuration.
     *
     * @param constraintAnnotation annotation instance
     */
    @Override
    public void initialize(CrossFieldValidation constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldMatch = constraintAnnotation.fieldMatch();
    }

    /**
     * Performs validation logic between two fields.
     *
     * @param value   Object being validated
     * @param context Validation context
     * @return true if valid, false otherwise
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        Object fieldValue = new BeanWrapperImpl(value).getPropertyValue(field);
        Object fieldMatchValue = new BeanWrapperImpl(value).getPropertyValue(fieldMatch);

        // If either value is null, let @NotNull handle it
        if (fieldValue == null || fieldMatchValue == null) {
            return true;
        }

        boolean valid = fieldValue.equals(fieldMatchValue);

        if (!valid) {
            log.debug("Cross-field validation failed: {} does not match {}", field, fieldMatch);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(fieldMatch)
                    .addConstraintViolation();
        }

        return valid;
    }
}

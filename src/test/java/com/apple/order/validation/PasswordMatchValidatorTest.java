package com.apple.order.validation;

import com.apple.order.dto.UserRequest;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordMatchValidatorTest {

    private PasswordMatchValidator validator;

    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        validator = new PasswordMatchValidator();

        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeBuilder = mock(
                ConstraintValidatorContext.ConstraintViolationBuilder
                        .NodeBuilderCustomizableContext.class);

        when(context.getDefaultConstraintMessageTemplate())
                .thenReturn("Passwords do not match");

        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(builder);

        when(builder.addPropertyNode(anyString()))
                .thenReturn(nodeBuilder);

        when(nodeBuilder.addConstraintViolation())
                .thenReturn(context);
    }

    // ===============================
    // initialize() coverage
    // ===============================
    @Test
    void initialize_shouldNotThrow() {
        PasswordMatch annotation = mock(PasswordMatch.class);
        assertDoesNotThrow(() -> validator.initialize(annotation));
    }

    // ===============================
    // value == null
    // ===============================
    @Test
    void isValid_shouldReturnTrue_whenValueIsNull() {
        assertTrue(validator.isValid(null, context));
    }

    // ===============================
    // password == null
    // ===============================
    @Test
    void isValid_shouldReturnTrue_whenPasswordIsNull() {
        UserRequest request = mock(UserRequest.class);
        when(request.getPassword()).thenReturn(null);
        when(request.getConfirmPassword()).thenReturn("abc");

        assertTrue(validator.isValid(request, context));
    }

    // ===============================
    // confirmPassword == null
    // ===============================
    @Test
    void isValid_shouldReturnTrue_whenConfirmPasswordIsNull() {
        UserRequest request = mock(UserRequest.class);
        when(request.getPassword()).thenReturn("abc");
        when(request.getConfirmPassword()).thenReturn(null);

        assertTrue(validator.isValid(request, context));
    }

    // ===============================
    // passwords match
    // ===============================
    @Test
    void isValid_shouldReturnTrue_whenPasswordsMatch() {
        UserRequest request = mock(UserRequest.class);
        when(request.getPassword()).thenReturn("secret");
        when(request.getConfirmPassword()).thenReturn("secret");

        assertTrue(validator.isValid(request, context));
    }

    // ===============================
    // passwords do NOT match
    // ===============================
    @Test
    void isValid_shouldReturnFalse_whenPasswordsDoNotMatch() {

        UserRequest request = mock(UserRequest.class);
        when(request.getPassword()).thenReturn("secret");
        when(request.getConfirmPassword()).thenReturn("different");

        boolean result = validator.isValid(request, context);

        assertFalse(result);

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Passwords do not match");
        verify(builder).addPropertyNode("confirmPassword");
        verify(nodeBuilder).addConstraintViolation();
    }
}
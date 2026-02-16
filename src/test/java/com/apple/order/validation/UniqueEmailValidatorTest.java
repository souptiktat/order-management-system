package com.apple.order.validation;

import com.apple.order.repository.UserRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UniqueEmailValidatorTest {

    private UserRepository userRepository;
    private UniqueEmailValidator validator;

    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        validator = new UniqueEmailValidator(userRepository);
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.getDefaultConstraintMessageTemplate())
                .thenReturn("Email already exists");
        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(builder);
        when(builder.addConstraintViolation())
                .thenReturn(context);
    }

    // ======================================
    // initialize() coverage
    // ======================================
    @Test
    void initialize_shouldNotThrow() {
        UniqueEmail annotation = mock(UniqueEmail.class);
        assertDoesNotThrow(() -> validator.initialize(annotation));
    }

    // ======================================
    // email == null
    // ======================================
    @Test
    void isValid_shouldReturnTrue_whenEmailIsNull() {
        assertTrue(validator.isValid(null, context));
        verifyNoInteractions(userRepository);
    }

    // ======================================
    // email is blank
    // ======================================
    @Test
    void isValid_shouldReturnTrue_whenEmailIsBlank() {
        assertTrue(validator.isValid("   ", context));
        verifyNoInteractions(userRepository);
    }

    // ======================================
    // email exists in database
    // ======================================
    @Test
    void isValid_shouldReturnFalse_whenEmailExists() {
        String email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);
        boolean result = validator.isValid(email, context);
        assertFalse(result);
        verify(userRepository).existsByEmail(email);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Email already exists");
        verify(builder).addConstraintViolation();
    }

    // ======================================
    // email does NOT exist
    // ======================================
    @Test
    void isValid_shouldReturnTrue_whenEmailDoesNotExist() {
        String email = "unique@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        boolean result = validator.isValid(email, context);
        assertTrue(result);
        verify(userRepository).existsByEmail(email);
        verify(context, never()).disableDefaultConstraintViolation();
    }

    // ======================================
    // Annotation metadata coverage
    // ======================================
    @Test
    void uniqueEmailAnnotation_defaultsShouldBeCorrect() {
        UniqueEmail annotation =
                TestAnnotatedClass.class
                        .getDeclaredFields()[0]
                        .getAnnotation(UniqueEmail.class);
        assertNotNull(annotation);
        assertEquals("Email already exists", annotation.message());
        assertEquals(0, annotation.groups().length);
        assertEquals(0, annotation.payload().length);
    }

    static class TestAnnotatedClass {
        @UniqueEmail
        private String email;
    }
}
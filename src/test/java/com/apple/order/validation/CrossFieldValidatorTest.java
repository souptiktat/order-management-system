package com.apple.order.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrossFieldValidatorTest {

    private CrossFieldValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        validator = new CrossFieldValidator();
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        when(context.getDefaultConstraintMessageTemplate())
                .thenReturn("Cross field validation failed");
        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(builder);
        // ✅ Correct return type
        when(builder.addPropertyNode(anyString()))
                .thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation())
                .thenReturn(context);
    }

    // ===============================
    // Helper DTO
    // ===============================
    static class TestDto {
        private String password;
        private String confirmPassword;
        public TestDto(String password, String confirmPassword) {
            this.password = password;
            this.confirmPassword = confirmPassword;
        }
        public String getPassword() {
            return password;
        }
        public String getConfirmPassword() {
            return confirmPassword;
        }
    }

    // ===============================
    // Initialize Test
    // ===============================
    @Test
    void initialize_shouldSetFieldNames() {
        CrossFieldValidation annotation = mock(CrossFieldValidation.class);
        when(annotation.field()).thenReturn("password");
        when(annotation.fieldMatch()).thenReturn("confirmPassword");
        validator.initialize(annotation);
        // We indirectly test fields via isValid
        TestDto dto = new TestDto("abc", "abc");
        assertTrue(validator.isValid(dto, context));
    }

    // ===============================
    // Null Value Cases
    // ===============================
    @Test
    void isValid_shouldReturnTrue_whenFieldIsNull() {
        validator.initialize(createAnnotation("password", "confirmPassword"));
        TestDto dto = new TestDto(null, "abc");
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void isValid_shouldReturnTrue_whenFieldMatchIsNull() {
        validator.initialize(createAnnotation("password", "confirmPassword"));
        TestDto dto = new TestDto("abc", null);
        assertTrue(validator.isValid(dto, context));
    }

    // ===============================
    // Matching Values
    // ===============================
    @Test
    void isValid_shouldReturnTrue_whenValuesMatch() {
        validator.initialize(createAnnotation("password", "confirmPassword"));
        TestDto dto = new TestDto("secret", "secret");
        assertTrue(validator.isValid(dto, context));
    }

    // ===============================
    // Non-Matching Values
    // ===============================
    @Test
    void isValid_shouldReturnFalse_whenValuesDoNotMatch() {
        validator.initialize(createAnnotation("password", "confirmPassword"));
        TestDto dto = new TestDto("secret", "different");
        boolean result = validator.isValid(dto, context);
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Cross field validation failed");
        verify(builder).addPropertyNode("confirmPassword");
        verify(nodeBuilder).addConstraintViolation();
    }

    // ===============================
    // Annotation Metadata Coverage
    // ===============================
    @Test
    void crossFieldValidationAnnotation_defaultsShouldBeCorrect() {
        CrossFieldValidation annotation =
                TestAnnotatedClass.class.getAnnotation(CrossFieldValidation.class);
        assertNotNull(annotation);
        assertEquals("password", annotation.field());
        assertEquals("confirmPassword", annotation.fieldMatch());
        assertEquals("Cross field validation failed", annotation.message());
        assertEquals(0, annotation.groups().length);
        assertEquals(0, annotation.payload().length);
    }

    @CrossFieldValidation(
            field = "password",
            fieldMatch = "confirmPassword"
    )
    static class TestAnnotatedClass {
        private String password;
        private String confirmPassword;
    }

    // ===============================
    // Helper Method
    // ===============================
    private CrossFieldValidation createAnnotation(String field, String fieldMatch) {
        CrossFieldValidation annotation = mock(CrossFieldValidation.class);
        when(annotation.field()).thenReturn(field);
        when(annotation.fieldMatch()).thenReturn(fieldMatch);
        return annotation;
    }
}
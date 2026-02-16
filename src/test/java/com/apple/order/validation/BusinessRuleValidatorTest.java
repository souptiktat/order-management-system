package com.apple.order.validation;

import com.apple.order.entity.User;
import com.apple.order.exception.ApiException;
import com.apple.order.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessRuleValidatorTest {

    @Mock
    private UserRepository userRepository;

    private BusinessRuleValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new BusinessRuleValidator(userRepository);
    }

    // ===============================
    // 1️⃣ User Not Found Case (404)
    // ===============================
    @Test
    void validateCreditLimit_shouldThrowNotFound_whenUserDoesNotExist() {

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                validator.validateCreditLimit(1L, 100.0)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("User not found", ex.getMessage());

        verify(userRepository, times(1)).findById(1L);
    }

    // ===============================
    // 2️⃣ Credit Limit Exceeded (422)
    // ===============================
    @Test
    void validateCreditLimit_shouldThrowUnprocessableEntity_whenLimitExceeded() {

        User user = new User();
        user.setCreditLimit(500.0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () ->
                validator.validateCreditLimit(1L, 600.0)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("Credit limit exceeded", ex.getMessage());

        verify(userRepository, times(1)).findById(1L);
    }

    // ===============================
    // 3️⃣ Valid Credit Limit (No Exception)
    // ===============================
    @Test
    void validateCreditLimit_shouldPass_whenWithinCreditLimit() {

        User user = new User();
        user.setCreditLimit(500.0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() ->
                validator.validateCreditLimit(1L, 300.0)
        );

        verify(userRepository, times(1)).findById(1L);
    }
}
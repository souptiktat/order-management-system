package com.apple.order.validation;

import com.apple.order.dto.PaymentRequest;
import com.apple.order.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentConditionalValidatorTest {

    private PaymentConditionalValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PaymentConditionalValidator();
    }

    // ===============================
    // 1️⃣ request == null
    // ===============================
    @Test
    void validate_shouldReturn_whenRequestIsNull() {
        assertDoesNotThrow(() -> validator.validate(null));
    }

    // ===============================
    // 2️⃣ Non CREDIT_CARD payment type
    // ===============================
    @Test
    void validate_shouldPass_whenPaymentTypeIsNotCreditCard() {
        PaymentRequest request = mock(PaymentRequest.class);
        when(request.getPaymentType()).thenReturn(PaymentRequest.PaymentType.UPI);
        assertDoesNotThrow(() -> validator.validate(request));
    }

    // ===============================
    // 3️⃣ CREDIT_CARD but cardNumber == null
    // ===============================
    @Test
    void validate_shouldThrowException_whenCreditCardAndCardNumberIsNull() {

        PaymentRequest request = mock(PaymentRequest.class);

        when(request.getPaymentType())
                .thenReturn(PaymentRequest.PaymentType.CREDIT_CARD);

        when(request.getCardNumber()).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> validator.validate(request));

        assertEquals("Card number required", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ===============================
    // 4️⃣ CREDIT_CARD with cardNumber present
    // ===============================
    @Test
    void validate_shouldPass_whenCreditCardAndCardNumberProvided() {
        PaymentRequest request = mock(PaymentRequest.class);
        when(request.getPaymentType()).thenReturn(PaymentRequest.PaymentType.CREDIT_CARD);
        when(request.getCardNumber()).thenReturn("1234567890123456");
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
package com.apple.order.validation;

import com.apple.order.dto.OrderRequest;
import com.apple.order.dto.PaymentRequest;
import com.apple.order.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderValidationEngineTest {

    private BusinessRuleValidator businessRuleValidator;
    private PaymentConditionalValidator paymentConditionalValidator;
    private OrderValidationEngine orderValidationEngine;

    @BeforeEach
    void setUp() {
        businessRuleValidator = mock(BusinessRuleValidator.class);
        paymentConditionalValidator = mock(PaymentConditionalValidator.class);
        orderValidationEngine =
                new OrderValidationEngine(businessRuleValidator, paymentConditionalValidator);
    }

    // ===============================
    // 1️⃣ Success Scenario
    // ===============================
    @Test
    void validateOrder_shouldCallAllValidators_whenValid() {
        OrderRequest request = mock(OrderRequest.class);
        PaymentRequest payment = mock(PaymentRequest.class);
        when(request.getUserId()).thenReturn(1L);
        when(request.getAmount()).thenReturn(500.0);
        when(request.getPayment()).thenReturn(payment);
        assertDoesNotThrow(() ->
                orderValidationEngine.validateOrder(request)
        );
        verify(businessRuleValidator)
                .validateCreditLimit(1L, 500.0);
        verify(paymentConditionalValidator)
                .validate(payment);
    }

    // ===============================
    // 2️⃣ BusinessRuleValidator Throws
    // ===============================
    @Test
    void validateOrder_shouldPropagateException_whenBusinessRuleFails() {
        OrderRequest request = mock(OrderRequest.class);
        PaymentRequest payment = mock(PaymentRequest.class);
        when(request.getUserId()).thenReturn(1L);
        when(request.getAmount()).thenReturn(500.0);
        when(request.getPayment()).thenReturn(payment);
        doThrow(new ApiException(
                "Credit limit exceeded",
                HttpStatus.UNPROCESSABLE_ENTITY))
                .when(businessRuleValidator)
                .validateCreditLimit(1L, 500.0);
        assertThrows(ApiException.class, () ->
                orderValidationEngine.validateOrder(request)
        );
        verify(businessRuleValidator)
                .validateCreditLimit(1L, 500.0);
        verifyNoInteractions(paymentConditionalValidator);
    }

    // ===============================
    // 3️⃣ PaymentConditionalValidator Throws
    // ===============================
    @Test
    void validateOrder_shouldPropagateException_whenPaymentValidationFails() {
        OrderRequest request = mock(OrderRequest.class);
        PaymentRequest payment = mock(PaymentRequest.class);
        when(request.getUserId()).thenReturn(1L);
        when(request.getAmount()).thenReturn(500.0);
        when(request.getPayment()).thenReturn(payment);
        doThrow(new ApiException(
                "Invalid payment",
                HttpStatus.BAD_REQUEST))   // ✅ FIXED
                .when(paymentConditionalValidator)
                .validate(payment);
        assertThrows(ApiException.class, () ->
                orderValidationEngine.validateOrder(request)
        );
        verify(businessRuleValidator)
                .validateCreditLimit(1L, 500.0);
        verify(paymentConditionalValidator)
                .validate(payment);
    }
}
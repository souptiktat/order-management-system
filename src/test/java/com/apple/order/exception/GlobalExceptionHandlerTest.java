package com.apple.order.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ===============================
    // ApiException Tests
    // ===============================

    @Test
    void handleApiException_shouldReturnCustomStatusAndBody() {

        ApiException ex = new ApiException(
                "Order not found",
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND"
        );

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/orders/1");

        ResponseEntity<ErrorResponse> response =
                handler.handleApiException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("ORDER_NOT_FOUND", body.getError());
        assertEquals("Order not found", body.getMessage());
        assertEquals("/orders/1", body.getPath());
        assertNotNull(body.getTraceId());
        assertNull(body.getValidationErrors());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void apiException_defaultErrorCodeConstructor_shouldUseStatusName() {
        ApiException ex = new ApiException(
                "Bad request",
                HttpStatus.BAD_REQUEST
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("BAD_REQUEST", ex.getErrorCode());
        assertEquals("Bad request", ex.getMessage());
    }

    // ===============================
    // Validation Exception Test
    // ===============================

    @Test
    void handleValidationException_shouldReturnBadRequestWithErrors() {

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "object");

        bindingResult.addError(new FieldError(
                "object", "name", "Name is required"));

        // duplicate field (merge function test)
        bindingResult.addError(new FieldError(
                "object", "name", "Name must not be blank"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/orders");

        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("BAD_REQUEST", body.getError());
        assertEquals("Validation Failed", body.getMessage());
        assertEquals("/orders", body.getPath());
        assertNotNull(body.getTraceId());
        assertNotNull(body.getTimestamp());

        Map<String, String> errors = body.getValidationErrors();
        assertNotNull(errors);
        assertTrue(errors.containsKey("name"));

        // merge function keeps first value
        assertEquals("Name is required", errors.get("name"));
    }

    // ===============================
    // Generic Exception Test
    // ===============================

    @Test
    void handleGenericException_shouldReturnInternalServerError() {

        Exception ex = new RuntimeException("Something went wrong");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/orders");

        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", body.getError());
        assertEquals("Unexpected error occurred", body.getMessage());
        assertEquals("/orders", body.getPath());
        assertNotNull(body.getTraceId());
        assertNull(body.getValidationErrors());
        assertNotNull(body.getTimestamp());
    }
}
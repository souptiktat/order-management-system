package com.apple.order.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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

        HttpServletRequest request = mock(HttpServletRequest.class);
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

        HttpServletRequest request = mock(HttpServletRequest.class);
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

    // ===================================
    // Authorization Denied Exception Test
    // ===================================

    @Test
    void handleAuthorizationDenied_shouldReturn403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
        AuthorizationDeniedException ex =
                new AuthorizationDeniedException("Access denied");
        ResponseEntity<ErrorResponse> response =
                handler.handleAuthorizationDenied(ex, request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.getStatus());
        assertEquals("FORBIDDEN", body.getError());
        assertEquals("Access denied", body.getMessage());
        assertEquals("/api/test", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ========================================
    // HTTP Message Not Readable Exception Test
    // ========================================

    @Test
    void handleHttpMessageNotReadable_shouldReturn400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users");
        HttpInputMessage inputMessage = mock(HttpInputMessage.class);
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Malformed JSON", inputMessage);
        ResponseEntity<ErrorResponse> response =
                handler.handleHttpMessageNotReadable(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("BAD_REQUEST", body.getError());
        assertEquals("Malformed JSON request", body.getMessage());
        assertEquals("/api/users", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ============================================
    // HTTP Media Type Not Supported Exception Test
    // ============================================

    @Test
    void handleHttpMediaTypeNotSupported_shouldReturn415() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users");
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException("Unsupported");
        ResponseEntity<ErrorResponse> response =
                handler.handleHttpMediaTypeNotSupported(ex, request);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(415, body.getStatus());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", body.getError());
        assertEquals("Unsupported media type", body.getMessage());
        assertEquals("/api/users", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ============================
    // Type Mismatch Exception Test
    // ============================

    @Test
    void handleTypeMismatch_shouldReturn400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users/abc");
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException(
                        "abc", Long.class, "id", null, null);
        ResponseEntity<ErrorResponse> response =
                handler.handleTypeMismatch(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("BAD_REQUEST", body.getError());
        assertEquals("Method Argument Type Missing", body.getMessage());
        assertEquals("/api/users/abc", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ===============================
    // Business Exception Test
    // ===============================

    @Test
    void handleBusinessException_shouldReturn422() {
        BusinessException ex = new BusinessException("Business rule violated");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/1");
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(ex, request);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(422, body.getStatus());
        assertEquals("UNPROCESSABLE_ENTITY", body.getError());
        assertEquals("Business rule violated", body.getMessage());
        assertEquals("/api/orders/1", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    // ===============================
    // Access Denied Exception Test
    // ===============================

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/admin");
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(ex, request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.getStatus());
        assertEquals("FORBIDDEN", body.getError());
        assertEquals("Access denied", body.getMessage());
        assertEquals("/api/admin", body.getPath());
        assertNotNull(body.getTraceId());
        assertNull(body.getValidationErrors());
        assertNotNull(body.getTimestamp());
    }

    // ===============================
    // Entity Not Found Exception Test
    // ===============================

    @Test
    void handleEntityNotFound_shouldReturn404() {
        EntityNotFoundException ex = new EntityNotFoundException("Order not found");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/99");
        ResponseEntity<ErrorResponse> response =
                handler.handleEntityNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("NOT_FOUND", body.getError());
        assertEquals("Order not found", body.getMessage());
        assertEquals("/api/orders/99", body.getPath());
        assertNotNull(body.getTraceId());
        assertNull(body.getValidationErrors());
        assertNotNull(body.getTimestamp());
    }

    // ===============================
    // Illegal State Exception Test
    // ===============================

    @Test
    void handleIllegalStateException_shouldReturn409() {
        IllegalStateException ex = new IllegalStateException("Order already processed");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/1");
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalStateException(ex, request);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.getStatus());
        assertEquals("CONFLICT", body.getError());
        assertEquals("Order already processed", body.getMessage());
        assertEquals("/api/orders/1", body.getPath());
        assertNotNull(body.getTraceId());
        assertNull(body.getValidationErrors());
        assertNotNull(body.getTimestamp());
    }

    // ===============================
    // Generic Exception Test
    // ===============================

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new RuntimeException("Something went wrong");
        HttpServletRequest request = mock(HttpServletRequest.class);
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
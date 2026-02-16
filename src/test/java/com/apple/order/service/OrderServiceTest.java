package com.apple.order.service;

import com.apple.order.dto.OrderRequest;
import com.apple.order.dto.PaymentRequest;
import com.apple.order.entity.Order;
import com.apple.order.entity.User;
import com.apple.order.exception.ApiException;
import com.apple.order.repository.OrderRepository;
import com.apple.order.repository.UserRepository;
import com.apple.order.validation.OrderValidationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderValidationEngine validationEngine;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest request;
    private User user;
    private Order order;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .blocked(false)
                .build();

        PaymentRequest payment = new PaymentRequest();
        payment.setPaymentType(PaymentRequest.PaymentType.CREDIT_CARD);
        payment.setCardNumber("1234567890123456");
        payment.setUpiId(null);

        request = new OrderRequest();
        request.setUserId(1L);
        request.setProductName("Laptop");
        request.setAmount(5000.0);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(5));
        request.setPayment(payment);

        order = Order.builder()
                .id(10L)
                .productName("Laptop")
                .amount(5000.0)
                .status(Order.OrderStatus.CREATED)
                .user(user)
                .build();
    }

    // ============================================================
    // CREATE ORDER
    // ============================================================

    @Test
    void createOrder_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals("Laptop", result.getProductName());
        verify(validationEngine).validateOrder(request);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> orderService.createOrder(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void createOrder_blockedUser() {
        user.setBlocked(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> orderService.createOrder(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ============================================================
    // GET ORDER
    // ============================================================

    @Test
    void getOrder_success() {
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrder(10L);

        assertEquals(10L, result.getId());
    }

    @Test
    void getOrder_notFound() {
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> orderService.getOrder(10L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ============================================================
    // UPDATE ORDER
    // ============================================================

    @Test
    void update_success() {
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        Order updated = orderService.update(10L, request);

        assertEquals("Laptop", updated.getProductName());
        verify(validationEngine).validateOrder(request);
    }

    @Test
    void update_shippedOrder() {
        order.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> orderService.update(10L, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ============================================================
    // PATCH ORDER
    // ============================================================

    @Test
    void patch_updateStatus_success() {
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "SHIPPED");

        Order patched = orderService.patch(10L, updates);

        assertEquals(Order.OrderStatus.SHIPPED, patched.getStatus());
    }

    @Test
    void patch_updateAmount_success() {
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        Map<String, Object> updates = new HashMap<>();
        updates.put("amount", "9000");

        Order patched = orderService.patch(10L, updates);

        assertEquals(9000.0, patched.getAmount());
    }

    @Test
    void patch_cancelAfterShipment_shouldFail() {
        order.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "CANCELLED");

        ApiException ex = assertThrows(ApiException.class,
                () -> orderService.patch(10L, updates));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void patch_updateBothFields() {
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "SHIPPED");
        updates.put("amount", "12000");

        Order patched = orderService.patch(10L, updates);

        assertEquals(Order.OrderStatus.SHIPPED, patched.getStatus());
        assertEquals(12000.0, patched.getAmount());
    }

    // ============================================================
    // DELETE ORDER
    // ============================================================

    @Test
    void delete_success() {
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        orderService.delete(10L);

        verify(orderRepository).delete(order);
    }

    @Test
    void delete_shippedOrder_shouldFail() {
        order.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findOrderWithUser(10L)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class,
                () -> orderService.delete(10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
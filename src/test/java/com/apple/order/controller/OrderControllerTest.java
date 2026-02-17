package com.apple.order.controller;

import com.apple.order.config.JwtTokenProvider;
import com.apple.order.config.SecurityConfig;
import com.apple.order.entity.Order;
import com.apple.order.exception.BusinessException;
import com.apple.order.repository.OrderRepository;
import com.apple.order.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OrderRepository orderRepository;

    // ==============================
    // COMMON VALID JSON (OrderRequest + PaymentRequest)
    // ==============================

    private final String VALID_ORDER_JSON = """
        {
          "userId": 1,
          "productName": "MacBook Pro M3",
          "amount": 2500.0,
          "startDate": "2026-02-10",
          "endDate": "2026-02-15",
          "payment": {
            "paymentType": "CREDIT_CARD",
            "cardNumber": "1234567812345678",
            "upiId": null
          }
        }
        """;

    private final String PATCH_JSON = """
        {
           "status": "SHIPPED"
        }
        """;

    // ============================================================
    // CREATE (POST)
    // ============================================================

    @Test
    @WithMockUser
    void create_shouldReturn201_whenValid() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.CREATED);
        when(orderService.createOrder(any())).thenReturn(order);
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"));
        verify(orderService).createOrder(any());
    }

    @Test
    @WithMockUser
    void create_shouldReturn400_whenValidationFails() throws Exception {

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any());
    }

    @Test
    @WithMockUser
    void create_shouldReturn403_whenUserBlocked() throws Exception {

        when(orderService.createOrder(any()))
                .thenThrow(new AccessDeniedException("Blocked"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void create_shouldReturn404_whenUserNotFound() throws Exception {

        when(orderService.createOrder(any()))
                .thenThrow(new EntityNotFoundException());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void create_shouldReturn422_whenBusinessViolation() throws Exception {

        when(orderService.createOrder(any()))
                .thenThrow(new BusinessException("Rule violation"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser
    void create_shouldReturn500_whenUnexpectedError() throws Exception {

        when(orderService.createOrder(any()))
                .thenThrow(new RuntimeException());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void create_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // GET
    // ============================================================

    @Test
    @WithMockUser
    void get_shouldReturn200_whenFound() throws Exception {

        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.CREATED);

        when(orderService.getOrder(1L)).thenReturn(order);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @WithMockUser
    void get_shouldReturn404_whenNotFound() throws Exception {

        when(orderService.getOrder(1L))
                .thenThrow(new EntityNotFoundException());

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_shouldReturn401_whenUnauthenticated() throws Exception {

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // UPDATE (PUT)
    // ============================================================

    @Test
    @WithMockUser
    void update_shouldReturn200_whenValid() throws Exception {

        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.SHIPPED);

        when(orderService.update(eq(1L), any())).thenReturn(order);

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @WithMockUser
    void update_shouldReturn400_whenValidationFails() throws Exception {

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void update_shouldReturn404_whenNotFound() throws Exception {

        when(orderService.update(eq(1L), any()))
                .thenThrow(new EntityNotFoundException());

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void update_shouldReturn409_whenConflict() throws Exception {

        when(orderService.update(eq(1L), any()))
                .thenThrow(new IllegalStateException());

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void update_shouldReturn422_whenBusinessViolation() throws Exception {

        when(orderService.update(eq(1L), any()))
                .thenThrow(new BusinessException("Order update violates business rule"));

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("UNPROCESSABLE_ENTITY"))
                .andExpect(jsonPath("$.message")
                        .value("Order update violates business rule"));
    }

    // ============================================================
    // PATCH
    // ============================================================

    @Test
    @WithMockUser
    void patch_shouldReturn200_whenValid() throws Exception {

        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.SHIPPED);

        when(orderService.patch(eq(1L), any())).thenReturn(order);

        mockMvc.perform(patch("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @WithMockUser
    void patch_shouldReturn404_whenNotFound() throws Exception {

        when(orderService.patch(eq(1L), any()))
                .thenThrow(new EntityNotFoundException());

        mockMvc.perform(patch("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void patch_shouldReturn422_whenBusinessViolation() throws Exception {

        when(orderService.patch(eq(1L), any()))
                .thenThrow(new BusinessException("Business rule violated"));

        mockMvc.perform(patch("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("UNPROCESSABLE_ENTITY"))
                .andExpect(jsonPath("$.message").value("Business rule violated"));
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Test
    @WithMockUser
    void delete_shouldReturn204_whenSuccess() throws Exception {

        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isNoContent());

        verify(orderService).delete(1L);
    }

    @Test
    @WithMockUser
    void delete_shouldReturn404_whenNotFound() throws Exception {

        doThrow(new EntityNotFoundException())
                .when(orderService).delete(1L);

        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void delete_shouldReturn409_whenConflict() throws Exception {

        doThrow(new IllegalStateException())
                .when(orderService).delete(1L);

        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isConflict());
    }
}
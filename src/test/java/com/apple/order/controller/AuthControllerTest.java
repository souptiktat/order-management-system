package com.apple.order.controller;

import com.apple.order.config.JwtTokenProvider;
import com.apple.order.dto.AuthResponse;
import com.apple.order.dto.LoginRequest;
import com.apple.order.dto.RegisterRequest;
import com.apple.order.exception.ApiException;
import com.apple.order.service.AuthService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider; // if security exists

    // =============
    // REGISTER
    // =============

    @Test
    void register_shouldReturn201_whenValidRequest() throws Exception {

        String json = """
            {
              "name": "John",
              "email": "john@example.com",
              "password": "Password@123",
              "creditLimit": 50000,
              "country": "INDIA",
              "aadhaarNumber": "123412341234"
            }
            """;

        AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600L, "john.doe@example.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void register_shouldReturn400_whenValidationFails() throws Exception {

        String json = """
            {
              "name": "",
              "email": "",
              "password": "",
              "creditLimit": 0,
              "country": "",
              "aadhaarNumber": ""
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    void register_shouldReturn409_whenEmailExists() throws Exception {

        String json = """
            {
              "name": "John",
              "email": "existing@example.com",
              "password": "Password@123",
              "creditLimit": 50000,
              "country": "INDIA",
              "aadhaarNumber": "123412341234"
            }
            """;

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ApiException("Email exists", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @Disabled
    void register_shouldReturn422_whenBusinessRuleViolation() throws Exception {

        String json = """
            {
              "name": "John",
              "email": "john@example.com",
              "password": "weak",
              "creditLimit": 50000,
              "country": "INDIA",
              "aadhaarNumber": "123412341234"
            }
            """;

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ApiException("Weak password", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_shouldReturn500_whenUnexpectedError() throws Exception {

        String json = """
            {
              "name": "John",
              "email": "john@example.com",
              "password": "Password@123",
              "creditLimit": 50000,
              "country": "INDIA",
              "aadhaarNumber": "123412341234"
            }
            """;

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }

    // =============
    // LOGIN
    // =============

    @Test
    void login_shouldReturn200_whenValidCredentials() throws Exception {

        String json = """
            {
              "email": "john@example.com",
              "password": "Password@123"
            }
            """;

        AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600L, "john.doe@example.com");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_shouldReturn400_whenValidationFails() throws Exception {

        String json = """
            {
              "email": "",
              "password": ""
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    @Test
    void login_shouldReturn401_whenInvalidCredentials() throws Exception {

        String json = """
            {
              "email": "john@example.com",
              "password": "wrong"
            }
            """;

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn403_whenAccountBlocked() throws Exception {

        String json = """
            {
              "email": "john@example.com",
              "password": "Password@123"
            }
            """;

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ApiException("Account disabled", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_shouldReturn500_whenUnexpectedError() throws Exception {

        String json = """
            {
              "email": "john@example.com",
              "password": "Password@123"
            }
            """;

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Server error"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }

    // =====================================
    // REFRESH TOKEN
    // =====================================

    @Test
    void refresh_shouldReturn200_whenValidToken() throws Exception {

        AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600L, "john.doe@example.com");

        when(authService.refreshToken("valid-token"))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Refresh-Token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void refresh_shouldReturn401_whenInvalidToken() throws Exception {

        when(authService.refreshToken("invalid"))
                .thenThrow(new ApiException("Invalid token", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Refresh-Token", "invalid"))
                .andExpect(status().isUnauthorized());
    }
}
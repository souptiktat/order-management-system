package com.apple.order.controller;

import com.apple.order.config.JwtTokenProvider;
import com.apple.order.config.SecurityConfig;
import com.apple.order.dto.UserRequest;
import com.apple.order.entity.User;
import com.apple.order.exception.ApiException;
import com.apple.order.repository.UserRepository;
import com.apple.order.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider; // if security exists

    @MockitoBean
    private UserRepository userRepository; // 🔥 IMPORTANT FIX

    // =====================================
    // CREATE
    // =====================================
    @Test
    @WithMockUser(roles = "ADMIN")
    void create_shouldReturn201() throws Exception {
        // Make email unique
        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);
        String json = """
                {
                  "name": "Steve Jobs",
                  "email": "steve@apple.com",
                  "password": "Apple@123",
                  "confirmPassword": "Apple@123",
                  "creditLimit": 10000,
                  "country": "INDIA",
                  "aadhaarNumber": "123456789012"
                }
                """;
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("steve@apple.com");
        when(userService.createUser(any(UserRequest.class)))
                .thenReturn(savedUser);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("steve@apple.com"));
        verify(userService).createUser(any(UserRequest.class));
    }

    @Test
    void create_shouldReturn401_whenNotAuthenticated() throws Exception {
        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);
        String json = """
            {
              "name": "Steve Jobs",
              "email": "steve@apple.com",
              "password": "Apple@123",
              "confirmPassword": "Apple@123",
              "creditLimit": 10000,
              "country": "INDIA",
              "aadhaarNumber": "123456789012"
            }
            """;
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
        verify(userService, never()).createUser(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_shouldReturn403_whenNotAdmin() throws Exception {
        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);
        String json = """
            {
              "name": "Steve Jobs",
              "email": "steve@apple.com",
              "password": "Apple@123",
              "confirmPassword": "Apple@123",
              "creditLimit": 10000,
              "country": "INDIA",
              "aadhaarNumber": "123456789012"
            }
            """;
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
        verify(userService, never()).createUser(any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_shouldReturn409_whenEmailConflict() throws Exception {

        String requestJson = """
            {
              "name": "Valid Name",
              "email": "conflict@example.com",
              "password": "Password@123",
              "confirmPassword": "Password@123",
              "creditLimit": 5000,
              "country": "USA",
              "aadhaarNumber": null
            }
            """;
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(new ApiException("Email already exists", HttpStatus.CONFLICT));
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
        verify(userService).createUser(any(UserRequest.class));
    }

    // =====================================
    // GET BY ID
    // =====================================
    @Test
    @WithMockUser(roles = "ADMIN")  // or USER if endpoint allows
    void get_shouldReturnUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userService.getUser(1L)).thenReturn(user);
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
        verify(userService).getUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void get_shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.getUser(1L))
                .thenThrow(new ApiException("User not found", HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isNotFound());
        verify(userService).getUser(1L);
    }

    // =====================================
    // GET ALL
    // =====================================
    @Test
    @WithMockUser(roles = "ADMIN")   // or USER if endpoint allows
    void getAll_shouldReturnList() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userService.getAllUsers()).thenReturn(List.of(user));
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@example.com"));
        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_shouldReturnEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(userService).getAllUsers();
    }

    @Test
    void getAll_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAll_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    // =====================================
    // UPDATE
    // =====================================
    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn200_whenValidRequest() throws Exception {
        String requestJson = """
        {
          "name": "Updated Name",
          "email": "updated@example.com",
          "password": "NewPass@123",
          "confirmPassword": "NewPass@123",
          "creditLimit": 5000,
          "country": "USA"
        }
        """;
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("updated@example.com");
        when(userService.updateUser(eq(1L), any(UserRequest.class)))
                .thenReturn(updatedUser);
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));
        verify(userService).updateUser(eq(1L), any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn400_whenValidationFails() throws Exception {
        String requestJson = """
        {
          "name": "Updated Name",
          "email": "",
          "creditLimit": 5000,
          "country": "USA"
        }
        """;
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn404_whenUserNotFound() throws Exception {
        String requestJson = """
        {
          "name": "Updated Name",
          "email": "updated@example.com",
          "password": "NewPass@123",
          "confirmPassword": "NewPass@123",
          "creditLimit": 5000,
          "country": "USA"
        }
        """;
        when(userService.updateUser(eq(1L), any(UserRequest.class)))
                .thenThrow(new ApiException("User not found", HttpStatus.NOT_FOUND));
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
        verify(userService).updateUser(eq(1L), any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn409_whenEmailConflict() throws Exception {
        String requestJson = """
        {
          "name": "Updated Name",
          "email": "existing@example.com",
          "password": "NewPass@123",
          "confirmPassword": "NewPass@123",
          "creditLimit": 5000,
          "country": "USA"
        }
        """;
        when(userService.updateUser(eq(1L), any(UserRequest.class)))
                .thenThrow(new ApiException("Email already exists", HttpStatus.CONFLICT));
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
        verify(userService).updateUser(eq(1L), any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn400_whenIndianUserWithoutAadhaar() throws Exception {
        String requestJson = """
        {
          "name": "Updated Name",
          "email": "updated@example.com",
          "password": "NewPass@123",
          "confirmPassword": "NewPass@123",
          "creditLimit": 5000,
          "country": "INDIA"
        }
        """;
        when(userService.updateUser(eq(1L), any(UserRequest.class)))
                .thenThrow(new ApiException("Aadhaar required", HttpStatus.BAD_REQUEST));
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
        verify(userService).updateUser(eq(1L), any(UserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn400_whenMalformedJson() throws Exception {
        String invalidJson = "{ invalid json }";
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn415_whenUnsupportedMediaType() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("invalid"))
                .andExpect(status().isUnsupportedMediaType());
        verify(userService, never()).updateUser(any(), any());
    }
    // =====================================
    // DELETE
    // =====================================
    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn204_whenUserDeleted() throws Exception {
        doNothing().when(userService).deleteUser(1L);
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());
        verify(userService).deleteUser(1L);
    }

    @Test
    void delete_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isUnauthorized());
        verify(userService, never()).deleteUser(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isForbidden());
        verify(userService, never()).deleteUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn404_whenUserNotFound() throws Exception {
        doThrow(new ApiException("User not found", HttpStatus.NOT_FOUND))
                .when(userService).deleteUser(1L);
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNotFound());
        verify(userService).deleteUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn400_whenInvalidIdFormat() throws Exception {
        mockMvc.perform(delete("/api/v1/users/abc"))
                .andExpect(status().isBadRequest());
        verify(userService, never()).deleteUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn500_whenUnexpectedError() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(userService).deleteUser(1L);
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isInternalServerError());
        verify(userService).deleteUser(1L);
    }

    @TestConfiguration
    @EnableMethodSecurity   // 🔥 THIS IS THE KEY
    static class TestSecurityConfig {
    }
}
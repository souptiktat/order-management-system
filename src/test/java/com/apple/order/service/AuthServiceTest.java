package com.apple.order.service;

import com.apple.order.dto.AuthResponse;
import com.apple.order.dto.LoginRequest;
import com.apple.order.dto.RegisterRequest;
import com.apple.order.dto.UserRequest;
import com.apple.order.entity.User;
import com.apple.order.exception.ApiException;
import com.apple.order.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {

        registerRequest = RegisterRequest.builder()
                .name("John")
                .email("john@mail.com")
                .password("password")
                .creditLimit(1000.0)
                .country("USA")
                .aadhaarNumber(null)
                .build();

        loginRequest = LoginRequest.builder()
                .email("john@mail.com")
                .password("password")
                .build();

        user = User.builder()
                .id(1L)
                .name("John")
                .email("john@mail.com")
                .password("encodedPassword")
                .blocked(false)
                .build();
    }

    // ==========================================================
    // REGISTER
    // ==========================================================

    @Test
    void register_success() {

        when(userRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.empty());

        when(userService.register(any(UserRequest.class)))
                .thenReturn(user);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@mail.com");
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);

        verify(userService, times(1)).register(any(UserRequest.class));
    }

    @Test
    void register_emailAlreadyExists_shouldThrowConflict() {

        when(userRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.register(registerRequest));

        assertThat(ex.getMessage()).isEqualTo("Email already registered");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ==========================================================
    // LOGIN
    // ==========================================================

    @Test
    void login_success() {

        when(userRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encodedPassword"))
                .thenReturn(true);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@mail.com");
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_userNotFound_shouldThrowUnauthorized() {

        when(userRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login(loginRequest));

        assertThat(ex.getMessage()).isEqualTo("Invalid credentials");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_wrongPassword_shouldThrowUnauthorized() {

        when(userRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encodedPassword"))
                .thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login(loginRequest));

        assertThat(ex.getMessage()).isEqualTo("Invalid credentials");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_blockedUser_shouldThrowForbidden() {

        user.setBlocked(true);

        when(userRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encodedPassword"))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login(loginRequest));

        assertThat(ex.getMessage()).isEqualTo("User account is blocked");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ==========================================================
    // REFRESH TOKEN
    // ==========================================================

    @Test
    void refreshToken_success() {

        AuthResponse response = authService.refreshToken("valid-refresh-token");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("refreshed-user@example.com");
    }

    @Test
    void refreshToken_null_shouldThrowUnauthorized() {

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.refreshToken(null));

        assertThat(ex.getMessage()).isEqualTo("Invalid refresh token");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshToken_blank_shouldThrowUnauthorized() {

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.refreshToken("   "));

        assertThat(ex.getMessage()).isEqualTo("Invalid refresh token");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

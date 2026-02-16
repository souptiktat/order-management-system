package com.apple.order.service;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRequest request;
    private User user;

    @BeforeEach
    void setup() {
        request = new UserRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setCreditLimit(5000.0);
        request.setCountry("USA");
        request.setAadhaarNumber(null);

        user = User.builder()
                .id(1L)
                .name("John")
                .email("john@example.com")
                .password("encoded")
                .country("USA")
                .creditLimit(5000.0)
                .blocked(false)
                .build();
    }

    // ============================================================
    // REGISTER
    // ============================================================

    @Test
    void register_success() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(request);

        assertNotNull(result);
        assertEquals("encoded", result.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_indianUserWithoutAadhaar_shouldFail() {
        request.setCountry("INDIA");
        request.setAadhaarNumber(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.register(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void register_emailAlreadyExists_shouldFail() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.register(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ============================================================
    // CREATE USER (Admin)
    // ============================================================

    @Test
    void createUser_success() {
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser(request);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    // ============================================================
    // GET USER
    // ============================================================

    @Test
    void getUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUser(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getUser_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.getUser(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ============================================================
    // GET ALL USERS
    // ============================================================

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.getAllUsers();

        assertEquals(1, result.size());
    }

    // ============================================================
    // UPDATE USER
    // ============================================================

    @Test
    void updateUser_success_withPasswordUpdate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateUser(1L, request);

        assertEquals("encodedNew", updated.getPassword());
    }

    @Test
    void updateUser_success_withoutPasswordUpdate() {
        request.setPassword("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateUser(1L, request);

        assertEquals("encoded", updated.getPassword());
    }

    @Test
    void updateUser_emailConflictWithAnotherUser_shouldFail() {
        User anotherUser = User.builder().id(2L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(anotherUser));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateUser(1L, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void updateUser_indianWithoutAadhaar_shouldFail() {
        request.setCountry("INDIA");
        request.setAadhaarNumber(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.updateUser(1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ============================================================
    // BLOCK USER
    // ============================================================

    @Test
    void blockUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.blockUser(1L);

        assertTrue(user.isBlocked());
    }

    // ============================================================
    // DELETE USER
    // ============================================================

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> userService.deleteUser(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
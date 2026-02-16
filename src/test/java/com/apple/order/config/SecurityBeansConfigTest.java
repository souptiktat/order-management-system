package com.apple.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityBeansConfigTest {

    @Test
    void passwordEncoder_shouldReturnBCryptPasswordEncoder() {
        SecurityBeansConfig config = new SecurityBeansConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
    }

    @Test
    void passwordEncoder_shouldEncodeAndMatchPassword() {
        SecurityBeansConfig config = new SecurityBeansConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "mySecret123";
        String encodedPassword = encoder.encode(rawPassword);
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void passwordEncoder_shouldNotMatchWrongPassword() {
        SecurityBeansConfig config = new SecurityBeansConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "mySecret123";
        String encodedPassword = encoder.encode(rawPassword);
        assertFalse(encoder.matches("wrongPassword", encodedPassword));
    }
}

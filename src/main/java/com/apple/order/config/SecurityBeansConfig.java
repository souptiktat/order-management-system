package com.apple.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Central security-related bean configuration.
 *
 * Provides PasswordEncoder bean required by:
 *  - AuthService
 *  - UserService
 *
 * Ensures consistent password hashing strategy across application.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * BCrypt password encoder.
     *
     * Strength: 10 (default)
     * Production-ready hashing algorithm.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

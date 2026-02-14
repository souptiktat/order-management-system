package com.apple.order.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ===============================================================
 * SECURITY CONFIGURATION
 * ===============================================================
 *
 * This class defines the Spring Security configuration for the application.
 *
 * Responsibilities:
 *
 * 1. Disable CSRF for stateless REST APIs
 * 2. Configure endpoint authorization rules
 * 3. Register JWT authentication filter
 * 4. Enforce authentication for protected resources
 *
 * Security Architecture:
 *
 *  Client Request
 *        ↓
 *  JwtAuthenticationFilter
 *        ↓
 *  SecurityContext
 *        ↓
 *  Controller
 *
 * Public Endpoints:
 *   - /auth/**           → Authentication APIs
 *   - /swagger-ui/**     → Swagger UI
 *
 * Protected Endpoints:
 *   - All other endpoints require JWT authentication
 *
 * Authentication Type:
 *   - Stateless (JWT based)
 *   - No HTTP Session storage
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Tag(name = "Security Configuration",
    description = "Configures application-wide security rules, authentication mechanisms, " +
        "JWT filter integration, and endpoint access control."
)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    /**
     * ===============================================================
     * Security Filter Chain Configuration
     * ===============================================================
     *
     * Configures:
     *  - CSRF disabled (for REST APIs)
     *  - Public vs secured endpoints
     *  - JWT filter placement in filter chain
     *
     * @param http HttpSecurity configuration object
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @Operation(
            summary = "Configure security filter chain",
            description = "Defines authentication and authorization rules for the application. " +
                    "Registers JWT authentication filter and configures protected endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Security configuration applied successfully"),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access to protected resource"),
            @ApiResponse(responseCode = "403",
                    description = "Access denied due to insufficient permissions"),
            @ApiResponse(responseCode = "500",
                    description = "Internal security configuration error")
    })
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
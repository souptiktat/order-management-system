package com.apple.order.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ===============================================================
 * JWT AUTHENTICATION FILTER
 * ===============================================================
 *
 * This filter intercepts every incoming HTTP request once per request lifecycle.
 *
 * Responsibilities:
 *
 * 1. Extract JWT token from Authorization header.
 * 2. Validate and parse the token.
 * 3. Extract username from token.
 * 4. Create Authentication object.
 * 5. Set authentication in Spring Security Context.
 *
 * Expected Header Format:
 *      Authorization: Bearer <JWT_TOKEN>
 *
 * Security Flow:
 *  Request → Filter → Validate Token → Set SecurityContext → Continue Chain
 *
 * If token is invalid:
 *  - Authentication is not set
 *  - Request proceeds (may be blocked later by SecurityConfig)
 *
 * Enterprise Notes:
 *  - Stateless authentication
 *  - No session storage
 *  - JWT based access control
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Tag(name = "JWT Authentication Filter",
    description = "Internal security filter responsible for validating JWT tokens " +
        "from Authorization header and setting authenticated user in Security Context."
)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Core filtering logic executed once per request.
     *
     * This method:
     *  - Extracts JWT from Authorization header
     *  - Validates token
     *  - Sets Authentication in SecurityContext
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Remaining filters
     */
    @Override
    @Operation(
            summary = "Intercept and validate JWT token",
            description = "Extracts Bearer token from Authorization header, validates it, " +
                    "and sets the authenticated user into Spring Security Context."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Request successfully authenticated and processed"),
            @ApiResponse(responseCode = "401",
                    description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403",
                    description = "Access denied due to insufficient permissions"),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error during authentication process")
    })
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtTokenProvider.getUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT authentication successful for user: {}", username);
                }
            } catch (Exception ex) {
                log.error("Invalid JWT token: {}", ex.getMessage());
                // Optional: You may directly return 401 response here
                // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                // return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
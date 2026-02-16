package com.apple.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ============================================================
    // NO AUTH HEADER
    // ============================================================

    @Test
    void doFilterInternal_noAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ============================================================
    // HEADER WITHOUT BEARER PREFIX
    // ============================================================

    @Test
    void doFilterInternal_invalidHeaderPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ============================================================
    // VALID TOKEN AUTHENTICATION
    // ============================================================

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer validToken");
        when(jwtTokenProvider.getUsername("validToken")).thenReturn("john");
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("john", auth.getPrincipal());
        assertTrue(auth.isAuthenticated());
        verify(filterChain).doFilter(request, response);
    }

    // ============================================================
    // USERNAME NULL (branch coverage)
    // ============================================================

    @Test
    void doFilterInternal_usernameNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer validToken");
        when(jwtTokenProvider.getUsername("validToken")).thenReturn(null);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ============================================================
    // AUTH ALREADY PRESENT (branch coverage)
    // ============================================================

    @Test
    void doFilterInternal_authAlreadyExists() throws Exception {
        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken("existing", null, null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);
        when(request.getHeader("Authorization")).thenReturn("Bearer validToken");
        when(jwtTokenProvider.getUsername("validToken")).thenReturn("john");
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        // Should NOT override existing authentication
        assertEquals("existing",
                SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    // ============================================================
    // EXCEPTION FROM JWT PROVIDER
    // ============================================================

    @Test
    void doFilterInternal_invalidToken_exceptionHandled() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidToken");
        when(jwtTokenProvider.getUsername("invalidToken"))
                .thenThrow(new RuntimeException("Invalid token"));
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        // Authentication should remain null
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
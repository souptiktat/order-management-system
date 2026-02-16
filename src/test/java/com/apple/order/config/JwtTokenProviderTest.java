package com.apple.order.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String base64Secret;
    private long expiration = 1000; // 1 second

    @BeforeEach
    void setup() {
        byte[] keyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        base64Secret = Encoders.BASE64.encode(keyBytes);
        jwtTokenProvider = new JwtTokenProvider(base64Secret, expiration);
        jwtTokenProvider.init();
    }

    // ============================================================
    // INIT
    // ============================================================

    @Test
    void init_shouldThrowIfKeyTooShort() {
        String shortSecret = Encoders.BASE64.encode("shortkey".getBytes());
        JwtTokenProvider provider = new JwtTokenProvider(shortSecret, expiration);
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, provider::init);
        assertTrue(ex.getMessage().contains("256 bits"));
    }

    // ============================================================
    // GENERATE TOKEN
    // ============================================================

    @Test
    void generateAccessToken_success() {
        String token = jwtTokenProvider.generateAccessToken("john");
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    // ============================================================
    // VALID TOKEN
    // ============================================================

    @Test
    void validateToken_validToken() {
        String token = jwtTokenProvider.generateAccessToken("john");
        boolean result = jwtTokenProvider.validateToken(token);
        assertTrue(result);
    }

    // ============================================================
    // EXPIRED TOKEN
    // ============================================================

    @Test
    void validateToken_expiredToken() throws InterruptedException {
        JwtTokenProvider shortProvider =
                new JwtTokenProvider(base64Secret, 1); // 1 ms expiration
        shortProvider.init();
        String token = shortProvider.generateAccessToken("john");
        // Wait more than allowed clock skew (30 seconds)
        Thread.sleep(31000);
        boolean result = shortProvider.validateToken(token);
        assertFalse(result);
    }

    // ============================================================
    // MALFORMED TOKEN
    // ============================================================

    @Test
    void validateToken_malformedToken() {
        boolean result = jwtTokenProvider.validateToken("invalid.token.structure");
        assertFalse(result);
    }

    // ============================================================
    // UNSUPPORTED TOKEN
    // ============================================================

    @Test
    void validateToken_unsupportedToken() {
        String unsupportedToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJqb2huIn0.";
        boolean result = jwtTokenProvider.validateToken(unsupportedToken);
        assertFalse(result);
    }

    // ============================================================
    // INVALID SIGNATURE
    // ============================================================

    @Test
    void validateToken_invalidSignature() {
        byte[] anotherKeyBytes =
                Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        String anotherSecret = Encoders.BASE64.encode(anotherKeyBytes);
        JwtTokenProvider anotherProvider =
                new JwtTokenProvider(anotherSecret, expiration);
        anotherProvider.init();
        String token = anotherProvider.generateAccessToken("john");
        boolean result = jwtTokenProvider.validateToken(token);
        assertFalse(result);
    }

    // ============================================================
    // ILLEGAL ARGUMENT (NULL TOKEN)
    // ============================================================

    @Test
    void validateToken_illegalArgument() {
        boolean result = jwtTokenProvider.validateToken(null);
        assertFalse(result);
    }

    // ============================================================
    // GET USERNAME
    // ============================================================

    @Test
    void getUsername_success() {
        String token = jwtTokenProvider.generateAccessToken("john");
        String username = jwtTokenProvider.getUsername(token);
        assertEquals("john", username);
    }
}
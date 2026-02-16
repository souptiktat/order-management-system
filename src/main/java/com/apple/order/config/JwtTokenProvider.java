package com.apple.order.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

/**
 * ===============================================================
 * ENTERPRISE JWT TOKEN PROVIDER
 * ===============================================================
 *
 * Responsibilities:
 *  - Generate secure JWT access tokens
 *  - Validate token integrity & expiration
 *  - Extract claims safely
 *
 * Security Standards:
 *  - HMAC-SHA256 (HS256)
 *  - Base64 encoded secret key
 *  - Minimum 256-bit secret enforcement
 *  - Clock skew tolerance
 *
 * Production Guidelines:
 *  - Store secret in ENV variables (JWT_SECRET)
 *  - Rotate secret periodically
 *  - Use short-lived access tokens
 *  - Implement refresh token strategy
 */
@Component
@Slf4j
@Tag(
        name = "JWT Security Component",
        description = "Enterprise-grade JWT provider responsible for secure token generation, validation, " +
                "and claim extraction using Base64 encoded HMAC-SHA256 keys."
)
public class JwtTokenProvider {

    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;
    private final String base64Secret;
    private final long expiration;
    private Key signingKey;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration}") long expiration) {
        this.base64Secret = base64Secret;
        this.expiration = expiration;
    }

    /**
     * Initializes signing key from Base64 encoded secret.
     * Enforces minimum 256-bit key size.
     */
    @PostConstruct
    @Operation(
            summary = "Initialize JWT signing key",
            description = "Initializes cryptographic signing key using Base64 encoded secret " +
                    "configured in application.yml. Enforces minimum 256-bit key size."
    )
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret key must be at least 256 bits (32 bytes)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Token Provider initialized successfully with secure key");
    }

    // ===============================================================
    // TOKEN GENERATION
    // ===============================================================

    /**
     * Generates signed JWT access token.
     */
    @Operation(
            summary = "Generate Access Token",
            description = "Generates a signed JWT access token containing subject (username), " +
                    "issued timestamp, and expiration timestamp using HS256 algorithm."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "JWT access token generated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal error during token generation")
    })
    public String generateAccessToken(
            @Parameter(description = "Authenticated username", required = true)
            String username) {
        Instant now = Instant.now();
        log.debug("Generating JWT token for user: {}", username);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expiration)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ===============================================================
    // TOKEN VALIDATION
    // ===============================================================

    /**
     * Validates JWT token integrity and expiration.
     */
    @Operation(
            summary = "Validate JWT Token",
            description = "Validates JWT token signature integrity, expiration, and structure. " +
                    "Returns true if token is valid and not expired."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired"),
            @ApiResponse(responseCode = "500", description = "Internal error during validation")
    })
    public boolean validateToken(
            @Parameter(description = "JWT token string", required = true)
            String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Malformed JWT token: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT token compact of handler are invalid: {}", ex.getMessage());
        }
        return false;
    }

    // ===============================================================
    // CLAIM EXTRACTION
    // ===============================================================

    /**
     * Extracts username (subject) from JWT token.
     */
    @Operation(
            summary = "Extract Username from JWT",
            description = "Parses and validates JWT token, then extracts the username stored " +
                    "as subject claim."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Username extracted successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "500", description = "Internal parsing error")
    })
    public String getUsername(
            @Parameter(description = "Valid JWT token", required = true)
            String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Internal method for parsing JWT claims.
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
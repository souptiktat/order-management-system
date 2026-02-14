package com.apple.order.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * ===============================================================
 * JWT TOKEN PROVIDER
 * ===============================================================
 *
 * This component is responsible for:
 *
 * 1. Generating JWT tokens after successful authentication
 * 2. Extracting username from JWT token
 * 3. Validating token signature and expiration
 *
 * Configuration Source:
 *  - Secret and expiration values are loaded from application.yml
 *
 * Security Design:
 *  - Stateless authentication
 *  - HMAC-SHA signing algorithm
 *  - Expiration-based validation
 *
 * Production Best Practices:
 *  - Store secret in environment variables
 *  - Use Base64 encoded key
 *  - Rotate secret periodically
 *  - Use refresh token strategy
 */
@Component
@Slf4j
@Tag(
        name = "JWT Token Provider",
        description = "Security component responsible for generating and validating JSON Web Tokens (JWT) " +
                "for stateless authentication using secret key configured in application.yml."
)
public class JwtTokenProvider {

    /**
     * Secret key loaded from application.yml
     *
     * app.jwt.secret
     */
    @Value("${app.jwt.secret}")
    private String secret;

    /**
     * Token expiration time in milliseconds.
     *
     * app.jwt.expiration
     */
    @Value("${app.jwt.expiration}")
    private long expiration;

    /**
     * Cryptographic signing key derived from secret.
     */
    private Key signingKey;

    /**
     * Initialize signing key after bean construction.
     * Converts configured secret into HMAC-SHA key.
     */
    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("JWT Token Provider initialized with configured secret and expiration");
    }

    /**
     * ===============================================================
     * Generate JWT Token
     * ===============================================================
     *
     * Generates a signed JWT token containing:
     *  - Subject (username)
     *  - Issued timestamp
     *  - Expiration timestamp
     *
     * @param username authenticated username
     * @return signed JWT token
     */
    @Operation(
            summary = "Generate JWT token",
            description = "Creates a signed JWT token containing username as subject, " +
                    "issue timestamp, and expiration timestamp configured in application.yml."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "JWT token generated successfully"),
            @ApiResponse(responseCode = "500",
                    description = "Internal error while generating token")
    })
    public String generateToken(String username) {

        log.debug("Generating JWT token for user: {}", username);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey)
                .compact();
    }

    /**
     * ===============================================================
     * Extract Username From JWT
     * ===============================================================
     *
     * Parses the JWT token and validates:
     *  - Signature integrity
     *  - Expiration validity
     *
     * If token is invalid or expired, exception is thrown.
     *
     * @param token JWT token string
     * @return extracted username (subject)
     */
    @Operation(
            summary = "Extract username from JWT token",
            description = "Parses JWT token, validates signature and expiration, " +
                    "and extracts the username stored in token payload."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Username successfully extracted from valid token"),
            @ApiResponse(responseCode = "401",
                    description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "500",
                    description = "Internal error while parsing token")
    })
    public String getUsername(String token) {

        log.debug("Validating and extracting username from JWT token");

        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
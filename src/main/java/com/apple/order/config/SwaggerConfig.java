package com.apple.order.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ===============================================================
 * SWAGGER / OPENAPI CONFIGURATION
 * ===============================================================
 *
 * This configuration class customizes the OpenAPI documentation
 * for the Apple Order Management System.
 *
 * Responsibilities:
 *
 * 1. Define API metadata (Title, Description, Version)
 * 2. Configure JWT Bearer authentication scheme
 * 3. Enable global security requirement
 * 4. Integrate Swagger UI "Authorize" button
 *
 * Security Integration:
 *  - Uses HTTP Bearer authentication
 *  - Token format: JWT
 *  - Adds security requirement globally
 *
 * Swagger UI Usage:
 *  1. Click "Authorize"
 *  2. Enter: Bearer <your_token>
 *  3. Execute secured APIs
 *
 * Enterprise Benefits:
 *  - Clear API documentation
 *  - Security visibility
 *  - Production-ready API contract
 *  - Developer-friendly integration
 */
@Configuration
@Tag(name = "Swagger Configuration",
    description = "Configures OpenAPI documentation, API metadata, and JWT security scheme " +
        "for the Apple Order Management System."
)
public class SwaggerConfig {

    /**
     * ===============================================================
     * OpenAPI Bean Configuration
     * ===============================================================
     *
     * Defines:
     *  - API Info metadata
     *  - JWT Bearer security scheme
     *  - Global security requirement
     *
     * @return configured OpenAPI object
     */
    @Bean
    @Operation(
            summary = "Configure OpenAPI documentation",
            description = "Initializes OpenAPI configuration including API metadata, " +
                    "JWT Bearer authentication scheme, and global security requirement."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "OpenAPI configuration initialized successfully"),
            @ApiResponse(responseCode = "500",
                    description = "Internal error during OpenAPI configuration")
    })
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
            // API Metadata
            .info(new Info()
                .title("Apple Order Management API")
                .description("""
                    Enterprise-grade Spring Boot REST API
                  
                    Features:
                    • JWT-based authentication
                    • Custom validation framework
                    • Business rule validation
                    • Cross-field validation
                    • Conditional validation
                    • Role-based security ready
                    • Production-ready architecture
                    
                    Authentication:
                    Use Bearer JWT token in Authorization header.
                    """)
            .version("1.0.0")
            )
            // Global Security Requirement
            .addSecurityItem(new SecurityRequirement()
                    .addList(securitySchemeName)
            )
            // Security Scheme Definition
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                            Enter JWT token in the format:
                            
                            Bearer <your_token>
                            
                            Example:
                            Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                            """)
                )
            );
    }
}
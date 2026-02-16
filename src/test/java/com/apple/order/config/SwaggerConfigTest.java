package com.apple.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwaggerConfigTest {

    @Test
    void openAPI_shouldReturnProperlyConfiguredOpenAPIObject() {

        SwaggerConfig config = new SwaggerConfig();
        OpenAPI openAPI = config.openAPI();
        assertNotNull(openAPI);
        // ===== Verify Info =====
        Info info = openAPI.getInfo();
        assertNotNull(info);
        assertEquals("Apple Order Management API", info.getTitle());
        assertEquals("1.0.0", info.getVersion());
        assertTrue(info.getDescription().contains("JWT-based authentication"));
        // ===== Verify Security Requirement =====
        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
        SecurityRequirement securityRequirement = openAPI.getSecurity().get(0);
        assertTrue(securityRequirement.containsKey("bearerAuth"));
        // ===== Verify Security Scheme =====
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        SecurityScheme scheme =
                openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertNotNull(scheme);
        assertEquals("bearerAuth", scheme.getName());
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
        assertTrue(scheme.getDescription().contains("Bearer <your_token>"));
    }
}
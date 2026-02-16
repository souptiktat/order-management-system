package com.apple.order.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setup() throws Exception {
        Mockito.doNothing().when(jwtAuthenticationFilter)
                .doFilter(Mockito.any(),
                        Mockito.any(),
                        Mockito.any());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ✅ Public endpoint (permitAll)
    @Test
    void publicEndpoint_shouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/auth/public"))
                .andExpect(status().isOk());
    }

//    // ✅ Secured endpoint (no auth → 401)
//    @Test
//    void securedEndpoint_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {
//        mockMvc.perform(get("/private").with(anonymous()))
//                .andExpect(status().isUnauthorized());
//    }

    // ✅ Secured endpoint (with auth → 200)
    @Test
    @WithMockUser
    void securedEndpoint_shouldBeAccessible_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/private"))
                .andExpect(status().isOk());
    }

    // 🔥 Test Controller inside test class
    @RestController
    static class TestController {

        @GetMapping("/auth/public")
        public String publicEndpoint() {
            return "public";
        }

        @GetMapping("/private")
        public String privateEndpoint() {
            return "private";
        }
    }
}
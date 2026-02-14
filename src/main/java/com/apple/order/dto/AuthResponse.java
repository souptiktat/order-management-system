package com.apple.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Authentication response containing JWT tokens and user details")
public class AuthResponse {

    @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(example = "refresh-token-123456")
    private String refreshToken;

    @Schema(example = "Bearer")
    private String tokenType;

    @Schema(example = "3600")
    private Long expiresIn;

    @Schema(example = "john.doe@example.com")
    private String email;
}
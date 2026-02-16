package com.apple.order.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
@Schema(description = "Standard API Error Response")
@Builder
public class ErrorResponse {

    @Schema(example = "2026-02-13T22:50:00")
    private LocalDateTime timestamp;

    @Schema(example = "400")
    private int status;

    @Schema(example = "BAD_REQUEST")
    private String error;

    @Schema(example = "Validation failed")
    private String message;

    @Schema(example = "/api/v1/orders")
    private String path;

    @Schema(example = "a9f4c23e")
    private String traceId;

    private Map<String, String> validationErrors;
}
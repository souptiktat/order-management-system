package com.apple.order.dto;

import com.apple.order.validation.CrossFieldValidation;
import com.apple.order.validation.PasswordMatch;
import com.apple.order.validation.UniqueEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PasswordMatch
@Schema(description = "User registration request payload")
@CrossFieldValidation(
        field = "password",
        fieldMatch = "confirmPassword",
        message = "Passwords do not match"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    @Schema(example = "Steve Jobs")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @UniqueEmail
    @Schema(example = "steve@apple.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(example = "Apple@123")
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Schema(example = "Apple@123")
    private String confirmPassword;

    @Positive(message = "Credit limit must be positive")
    @Schema(example = "10000")
    private Double creditLimit;

    @NotBlank(message = "Country is required")
    @Schema(example = "INDIA")
    private String country;

    @Pattern(regexp = "\\d{12}", message = "Aadhaar must be 12 digits")
    @Schema(example = "123456789012")
    private String aadhaarNumber;
}

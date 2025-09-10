package com.example.gpay.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class OTPRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9\\-\\s\\(\\)]+$", message = "Invalid phone number format")
    private String phoneNumber;

    // Optional password field for additional security
    private String password;
}

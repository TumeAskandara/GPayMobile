package com.example.gpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AuthRequest {
    // More flexible phone number validation for login
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+237|237)?[26][0-9]{8}$", message = "Invalid phone number format. Use: 123456789 or +237123456789")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    private String password;

    public AuthRequest() {}

    public AuthRequest(String phoneNumber, String password) {
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
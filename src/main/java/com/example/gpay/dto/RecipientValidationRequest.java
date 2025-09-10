package com.example.gpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RecipientValidationRequest {

    @NotBlank(message = "Recipient phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @JsonProperty("recipient_phone_number")
    private String recipientPhoneNumber;

    // Constructors
    public RecipientValidationRequest() {}

    public RecipientValidationRequest(String recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    // Getters and Setters
    public String getRecipientPhoneNumber() { return recipientPhoneNumber; }
    public void setRecipientPhoneNumber(String recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    @Override
    public String toString() {
        return "RecipientValidationRequest{" +
                "recipientPhoneNumber='" + recipientPhoneNumber + '\'' +
                '}';
    }
}


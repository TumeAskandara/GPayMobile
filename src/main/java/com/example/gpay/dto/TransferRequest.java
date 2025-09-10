package com.example.gpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class TransferRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum transfer amount is 100 XAF")
    private Double amount;

    @NotBlank(message = "Recipient phone number is required")
    @Pattern(regexp = "^\\+237[0-9]{9}$", message = "Phone number must be in format +237XXXXXXXXX")
    private String recipientPhoneNumber;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
    private String pin;

    private String description;

    // Constructors
    public TransferRequest() {}

    // Getters and Setters
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getRecipientPhoneNumber() { return recipientPhoneNumber; }
    public void setRecipientPhoneNumber(String recipientPhoneNumber) { this.recipientPhoneNumber = recipientPhoneNumber; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
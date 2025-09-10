package com.example.gpay.dto;

import com.example.gpay.model.MobileMoneyProvider;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WithdrawalRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @JsonProperty("amount")
    private Double amount;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4-6 digits")
    @JsonProperty("pin")
    private String pin;

    @NotNull(message = "Provider is required")
    @JsonProperty("provider")
    private MobileMoneyProvider provider;

    // Custom setter to sanitize provider if it's a string
    public void setProvider(MobileMoneyProvider provider) {
        this.provider = provider;
    }

    // If provider comes as string, add this method
    @JsonProperty("provider")
    public void setProviderFromString(String providerString) {
        if (providerString != null) {
            // Sanitize the string before parsing
            String sanitized = providerString.replaceAll("\\p{Cntrl}", "").trim();
            try {
                this.provider = MobileMoneyProvider.valueOf(sanitized.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid provider: " + sanitized);
            }
        }
    }

    // Getters
    public Double getAmount() { return amount; }
    public String getPin() { return pin; }
    public MobileMoneyProvider getProvider() { return provider; }

    // Setters
    public void setAmount(Double amount) { this.amount = amount; }
    public void setPin(String pin) {
        // Sanitize PIN to remove any control characters
        this.pin = pin != null ? pin.replaceAll("\\p{Cntrl}", "").trim() : null;
    }

    @Override
    public String toString() {
        return "WithdrawalRequest{" +
                "amount=" + amount +
                ", provider=" + provider +
                ", pin='****" +
                '}';
    }
}
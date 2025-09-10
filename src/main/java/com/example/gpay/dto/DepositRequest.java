package com.example.gpay.dto;

import com.example.gpay.model.MobileMoneyProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class DepositRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Minimum deposit amount is 1 XAF")
    private Double amount;

    @NotNull(message = "Provider is required")
    private MobileMoneyProvider provider;

    @NotNull(message = "PIN is required")
    private String pin;

    // Constructors
    public DepositRequest() {}

    public DepositRequest(Double amount, MobileMoneyProvider provider, String pin) {
        this.amount = amount;
        this.provider = provider;
        this.pin = pin;
    }

    // Getters and Setters
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public MobileMoneyProvider getProvider() { return provider; }
    public void setProvider(MobileMoneyProvider provider) { this.provider = provider; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    @Override
    public String toString() {
        return "DepositRequest{" +
                "amount=" + amount +
                ", provider=" + provider +
                ", pin='****'" + // Hide PIN for security
                '}';
    }
}
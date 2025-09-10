package com.example.gpay.dto;

import java.time.LocalDateTime;

public class TransferResponse {

    private String transactionReference;
    private String senderPhone;
    private String recipientPhone;
    private String recipientName;
    private Double amount;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private Double senderNewBalance;

    // Constructors
    public TransferResponse() {}

    public TransferResponse(String transactionReference, String senderPhone, String recipientPhone,
                            String recipientName, Double amount, String status, String description,
                            LocalDateTime createdAt, Double senderNewBalance) {
        this.transactionReference = transactionReference;
        this.senderPhone = senderPhone;
        this.recipientPhone = recipientPhone;
        this.recipientName = recipientName;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.senderNewBalance = senderNewBalance;
    }

    // Getters and Setters
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getSenderNewBalance() { return senderNewBalance; }
    public void setSenderNewBalance(Double senderNewBalance) {
        this.senderNewBalance = senderNewBalance;
    }

    // Optional: toString() for logging or debugging
    @Override
    public String toString() {
        return "TransferResponse{" +
                "transactionReference='" + transactionReference + '\'' +
                ", senderPhone='" + senderPhone + '\'' +
                ", recipientPhone='" + recipientPhone + '\'' +
                ", recipientName='" + recipientName + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", senderNewBalance=" + senderNewBalance +
                '}';
    }
}

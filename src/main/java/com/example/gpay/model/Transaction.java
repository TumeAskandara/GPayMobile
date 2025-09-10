package com.example.gpay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id = UUID.randomUUID().toString();

    @Field("user_id")
    private String userId;

    @Field("recipient_id")
    private String recipientId;

    @Field("recipient_phone_number")
    private String recipientPhoneNumber;

    @Field("type")
    private TransactionType type;

    @Field("amount")
    private Double amount;

    @Field("description")
    private String description;

    @Field("status")
    private TransactionStatus status;

    @Field("reference")
    private String reference;

    @Field("external_reference")
    private String externalReference;  // Added missing field

    @Field("provider")
    private MobileMoneyProvider provider;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    // Custom constructor for backward compatibility
    public Transaction(String userId, TransactionType type, Double amount, String description) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.status = TransactionStatus.PENDING;
        this.reference = generateReference();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private String generateReference() {
        return "TXN" + System.currentTimeMillis();
    }

    // Custom setter for status that updates timestamp
    public void setStatus(TransactionStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to update the updatedAt timestamp
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", reference='" + reference + '\'' +
                ", externalReference='" + externalReference + '\'' +
                ", provider=" + provider +
                ", recipientPhoneNumber='" + recipientPhoneNumber + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
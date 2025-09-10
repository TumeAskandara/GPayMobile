package com.example.gpay.dto;

/**
 * Response DTO for recipient validation
 */
public class RecipientValidationResponse {

    private boolean valid;
    private String recipientName;
    private String recipientPhone;
    private String message;

    // Constructors
    public RecipientValidationResponse() {
    }

    public RecipientValidationResponse(boolean valid, String recipientName, String recipientPhone, String message) {
        this.valid = valid;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.message = message;
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "RecipientValidationResponse{" +
                "valid=" + valid +
                ", recipientName='" + recipientName + '\'' +
                ", recipientPhone='" + recipientPhone + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

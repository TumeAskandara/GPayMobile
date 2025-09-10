package com.example.gpay.dto.campay;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CampayDisburseRequest {
    @JsonProperty("amount")
    private String amount;

    @JsonProperty("to")
    private String to;

    @JsonProperty("description")
    private String description;

    @JsonProperty("external_reference")
    private String externalReference;

    public CampayDisburseRequest() {}

    public CampayDisburseRequest(String amount, String to, String description, String externalReference) {
        this.amount = amount;
        this.to = to;
        this.description = sanitizeDescription(description);
        this.externalReference = externalReference;
    }

    // Sanitize description to remove control characters
    private String sanitizeDescription(String description) {
        if (description == null) {
            return null;
        }

        // Remove all control characters (including newlines, tabs, etc.)
        return description.replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
                .trim();
    }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = sanitizeDescription(description);
    }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    @Override
    public String toString() {
        return "CampayDisburseRequest{" +
                "amount='" + amount + '\'' +
                ", to='" + to + '\'' +
                ", description='" + description + '\'' +
                ", externalReference='" + externalReference + '\'' +
                '}';
    }
}
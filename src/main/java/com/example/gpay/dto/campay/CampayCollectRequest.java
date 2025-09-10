package com.example.gpay.dto.campay;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CampayCollectRequest {
    @JsonProperty("amount")
    private String amount;

    @JsonProperty("from")
    private String from;

    @JsonProperty("description")
    private String description;

    @JsonProperty("external_reference")
    private String externalReference;

    public CampayCollectRequest() {}

    public CampayCollectRequest(String amount, String from, String description, String externalReference) {
        this.amount = amount;
        this.from = from;
        this.description = description;
        this.externalReference = externalReference;
    }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
}

package com.example.gpay.dto.campay;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CampayResponse {
    @JsonProperty("reference")
    private String reference;

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("status")
    private String status;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("code")
    private String code;

    @JsonProperty("operator_reference")
    private String operatorReference;

    public CampayResponse() {}

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getOperatorReference() { return operatorReference; }
    public void setOperatorReference(String operatorReference) { this.operatorReference = operatorReference; }
}

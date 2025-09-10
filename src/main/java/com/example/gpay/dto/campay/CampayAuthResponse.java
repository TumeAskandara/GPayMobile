package com.example.gpay.dto.campay;


import com.fasterxml.jackson.annotation.JsonProperty;

public class CampayAuthResponse {
    @JsonProperty("token")
    private String token;

    @JsonProperty("expires_at")
    private String expiresAt;

    public CampayAuthResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}


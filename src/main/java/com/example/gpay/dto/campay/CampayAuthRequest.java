package com.example.gpay.dto.campay;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CampayAuthRequest {
    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    public CampayAuthRequest() {}

    public CampayAuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

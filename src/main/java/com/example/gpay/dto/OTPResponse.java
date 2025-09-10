package com.example.gpay.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OTPResponse {
    private String message;
    private boolean success;
    private Long expiresInMinutes;

    public static OTPResponse success(String message, Long expiresInMinutes) {
        return new OTPResponse(message, true, expiresInMinutes);
    }

    public static OTPResponse failure(String message) {
        return new OTPResponse(message, false, null);
    }
}
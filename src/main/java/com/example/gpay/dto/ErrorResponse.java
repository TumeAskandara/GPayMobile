package com.example.gpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private boolean success = false;
    private long timestamp = System.currentTimeMillis();

    public ErrorResponse(String message) {
        this.message = message;
    }
}
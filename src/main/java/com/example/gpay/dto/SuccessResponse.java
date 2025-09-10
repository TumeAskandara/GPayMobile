package com.example.gpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public
class SuccessResponse {
    private String message;
    private boolean success = true;
    private long timestamp = System.currentTimeMillis();

    public SuccessResponse(String message) {
        this.message = message;
    }
}


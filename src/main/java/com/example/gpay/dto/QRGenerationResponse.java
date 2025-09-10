package com.example.gpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRGenerationResponse {

    private String qrCodeImage; // Base64 encoded image
    private QRCodeData qrData;
    private String message;
    private boolean success;
}
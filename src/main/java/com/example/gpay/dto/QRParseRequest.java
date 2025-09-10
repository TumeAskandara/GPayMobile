package com.example.gpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRParseRequest {

    @NotBlank(message = "QR code content is required")
    private String qrContent;
}
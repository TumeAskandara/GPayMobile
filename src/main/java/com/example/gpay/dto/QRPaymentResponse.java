package com.example.gpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRPaymentResponse {

    private String transactionReference;
    private QRCodeData recipientInfo;
    private Double amount;
    private String status;
    private String message;
    private boolean success;
    private String timestamp;
}
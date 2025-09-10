package com.example.gpay.dto;

import com.example.gpay.dto.QRCodeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRParseResponse {

    private QRCodeData recipientInfo;
    private String message;
    private boolean success;
    private boolean recipientExists;
}
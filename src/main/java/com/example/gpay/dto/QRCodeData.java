
package com.example.gpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;



/**
 * QR Code Data structure for payment transfers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeData {

    @JsonProperty("type")
    @NotBlank(message = "QR code type is required")
    private String type;

    @JsonProperty("phone")
    @NotBlank(message = "Phone number is required")
    private String phone;

    @JsonProperty("name")
    @NotBlank(message = "User name is required")
    private String name;

    @JsonProperty("version")
    private String version;
}
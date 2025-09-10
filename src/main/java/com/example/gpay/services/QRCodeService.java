package com.example.gpay.services;

import com.example.gpay.dto.QRCodeData;
import com.example.gpay.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class QRCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeService.class);
    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    private static final String QR_VERSION = "1.0";

    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * Generate QR code for user's payment information
     */
    public String generateUserQRCode(String phoneNumber) {
        try {
            // Find user by phone number
            User user = userService.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));

            // Create QR code data
            QRCodeData qrData = QRCodeData.builder()
                    .type("gpay_transfer")
                    .phone(user.getPhoneNumber())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .version(QR_VERSION)
                    .build();

            // Convert to JSON string
            String qrDataJson = objectMapper.writeValueAsString(qrData);
            logger.info("Generating QR code for user: {} with data: {}", phoneNumber, qrDataJson);

            // Generate QR code image
            return generateQRCodeImage(qrDataJson);

        } catch (Exception e) {
            logger.error("Error generating QR code for user {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage());
        }
    }

    /**
     * Parse QR code data from scanned content
     */
    public QRCodeData parseQRCode(String qrContent) {
        try {
            logger.info("Parsing QR code content: {}", qrContent);

            QRCodeData qrData = objectMapper.readValue(qrContent, QRCodeData.class);

            // Validate QR code format
            if (!"gpay_transfer".equals(qrData.getType())) {
                throw new RuntimeException("Invalid QR code format. Not a GPay transfer QR code.");
            }

            if (qrData.getPhone() == null || qrData.getPhone().trim().isEmpty()) {
                throw new RuntimeException("Invalid QR code: Missing phone number");
            }

            if (qrData.getName() == null || qrData.getName().trim().isEmpty()) {
                throw new RuntimeException("Invalid QR code: Missing user name");
            }

            logger.info("Successfully parsed QR code for user: {} ({})", qrData.getName(), qrData.getPhone());
            return qrData;

        } catch (Exception e) {
            logger.error("Error parsing QR code: {}", e.getMessage());
            throw new RuntimeException("Invalid QR code format: " + e.getMessage());
        }
    }

    /**
     * Validate if the recipient from QR code exists in the system
     */
    public boolean validateQRRecipient(QRCodeData qrData) {
        try {
            return userService.findByPhoneNumber(qrData.getPhone()).isPresent();
        } catch (Exception e) {
            logger.error("Error validating QR recipient: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate QR code image and return as Base64 string
     */
    private String generateQRCodeImage(String content) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);

        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", outputStream);

        byte[] imageBytes = outputStream.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        logger.info("QR code image generated successfully, size: {} bytes", imageBytes.length);
        return "data:image/png;base64," + base64Image;
    }

    /**
     * Get user info for QR code display
     */
    public User getUserForQRDisplay(String phoneNumber) {
        return userService.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));
    }
}
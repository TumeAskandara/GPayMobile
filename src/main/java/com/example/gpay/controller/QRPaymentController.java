package com.example.gpay.controller;

import com.example.gpay.dto.*;
import com.example.gpay.model.Transaction;
import com.example.gpay.services.QRCodeService;
import com.example.gpay.services.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QRPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(QRPaymentController.class);

    private final QRCodeService qrCodeService;
    private final TransferService transferService;

    /**
     * Generate QR code for user
     * GET /api/qr/generate/{phoneNumber}
     */
    @GetMapping("/generate/{phoneNumber}")
    public ResponseEntity<QRGenerationResponse> generateQRCode(@PathVariable String phoneNumber) {
        try {
            logger.info("Generating QR code for user: {}", phoneNumber);

            String qrCodeImage = qrCodeService.generateUserQRCode(phoneNumber);
            var user = qrCodeService.getUserForQRDisplay(phoneNumber);

            QRCodeData qrData = QRCodeData.builder()
                    .type("gpay_transfer")
                    .phone(user.getPhoneNumber())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .version("1.0")
                    .build();

            QRGenerationResponse response = QRGenerationResponse.builder()
                    .qrCodeImage(qrCodeImage)
                    .qrData(qrData)
                    .message("QR code generated successfully")
                    .success(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error generating QR code for user {}: {}", phoneNumber, e.getMessage());

            QRGenerationResponse errorResponse = QRGenerationResponse.builder()
                    .message("Failed to generate QR code: " + e.getMessage())
                    .success(false)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate QR code using request body
     * POST /api/qr/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<QRGenerationResponse> generateQRCode(@Valid @RequestBody QRGenerationRequest request) {
        return generateQRCode(request.getPhoneNumber());
    }

    /**
     * Parse QR code content and validate recipient
     * POST /api/qr/parse
     */
    @PostMapping("/parse")
    public ResponseEntity<QRParseResponse> parseQRCode(@Valid @RequestBody QRParseRequest request) {
        try {
            logger.info("Parsing QR code content");

            QRCodeData qrData = qrCodeService.parseQRCode(request.getQrContent());
            boolean recipientExists = qrCodeService.validateQRRecipient(qrData);

            QRParseResponse response = QRParseResponse.builder()
                    .recipientInfo(qrData)
                    .recipientExists(recipientExists)
                    .message(recipientExists ?
                            "QR code parsed successfully. Recipient found." :
                            "QR code parsed but recipient not found in system.")
                    .success(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error parsing QR code: {}", e.getMessage());

            QRParseResponse errorResponse = QRParseResponse.builder()
                    .message("Failed to parse QR code: " + e.getMessage())
                    .success(false)
                    .recipientExists(false)
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Process payment using QR code
     * POST /api/qr/pay/{senderPhoneNumber}
     */
    @PostMapping("/pay/{senderPhoneNumber}")
    public ResponseEntity<QRPaymentResponse> processQRPayment(
            @PathVariable String senderPhoneNumber,
            @Valid @RequestBody QRPaymentRequest request) {

        try {
            logger.info("Processing QR payment from user: {} for amount: {}",
                    senderPhoneNumber, request.getAmount());

            // Step 1: Parse QR code to get recipient information
            QRCodeData recipientInfo = qrCodeService.parseQRCode(request.getQrContent());

            // Step 2: Validate recipient exists
            if (!qrCodeService.validateQRRecipient(recipientInfo)) {
                throw new RuntimeException("Recipient not found in system: " + recipientInfo.getPhone());
            }

            // Step 3: Create transfer request
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setRecipientPhoneNumber(recipientInfo.getPhone());
            transferRequest.setAmount(request.getAmount());
            transferRequest.setPin(request.getPin());
            transferRequest.setDescription(request.getDescription() != null ?
                    request.getDescription() :
                    "QR Payment to " + recipientInfo.getName());

            // Step 4: Process transfer using existing TransferService
            Transaction transaction = transferService.processTransfer(senderPhoneNumber, transferRequest);

            // Step 5: Build success response
            QRPaymentResponse response = QRPaymentResponse.builder()
                    .transactionReference(transaction.getReference())
                    .recipientInfo(recipientInfo)
                    .amount(request.getAmount())
                    .status(transaction.getStatus().toString())
                    .message("Payment processed successfully via QR code")
                    .success(true)
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            logger.info("QR payment completed successfully. Reference: {}", transaction.getReference());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("QR payment failed from user {}: {}", senderPhoneNumber, e.getMessage());

            QRPaymentResponse errorResponse = QRPaymentResponse.builder()
                    .message("QR payment failed: " + e.getMessage())
                    .success(false)
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Validate QR code format without processing payment
     * POST /api/qr/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<QRParseResponse> validateQRCode(@Valid @RequestBody QRParseRequest request) {
        try {
            QRCodeData qrData = qrCodeService.parseQRCode(request.getQrContent());
            boolean recipientExists = qrCodeService.validateQRRecipient(qrData);

            QRParseResponse response = QRParseResponse.builder()
                    .recipientInfo(qrData)
                    .recipientExists(recipientExists)
                    .message("QR code is valid")
                    .success(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            QRParseResponse errorResponse = QRParseResponse.builder()
                    .message("Invalid QR code: " + e.getMessage())
                    .success(false)
                    .recipientExists(false)
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Get user info for QR code display
     * GET /api/qr/user-info/{phoneNumber}
     */
    @GetMapping("/user-info/{phoneNumber}")
    public ResponseEntity<?> getUserInfo(@PathVariable String phoneNumber) {
        try {
            var user = qrCodeService.getUserForQRDisplay(phoneNumber);

            QRCodeData userData = QRCodeData.builder()
                    .type("gpay_transfer")
                    .phone(user.getPhoneNumber())
                    .name(user.getFirstName() + " " + user.getLastName())
                    .version("1.0")
                    .build();

            return ResponseEntity.ok(userData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found: " + e.getMessage());
        }
    }
}
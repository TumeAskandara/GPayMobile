package com.example.gpay.controller;

import com.example.gpay.dto.ApiResponse;
import com.example.gpay.dto.TransferRequest;
import com.example.gpay.dto.RecipientValidationRequest;
import com.example.gpay.dto.RecipientValidationResponse;
import com.example.gpay.model.Transaction;
import com.example.gpay.model.User;
import com.example.gpay.services.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/transfers")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transfer", description = "Money transfer operations")
public class TransferController {

    private static final Logger logger = LoggerFactory.getLogger(TransferController.class);

    @Autowired
    private TransferService transferService;

    /**
     * Validate recipient before transfer
     * This endpoint checks if the recipient phone number exists in the database
     */
    @PostMapping("/validate-recipient")
    @Operation(summary = "Validate recipient phone number",
            description = "Check if recipient phone number exists in the system")
    public ResponseEntity<ApiResponse<RecipientValidationResponse>> validateRecipient(
            @Valid @RequestBody RecipientValidationRequest request) {

        logger.info("Validating recipient phone number: {}", request.getRecipientPhoneNumber());

        try {
            boolean isRegistered = transferService.isPhoneNumberRegistered(request.getRecipientPhoneNumber());

            if (isRegistered) {
                // Get recipient details (without sensitive info)
                Optional<User> recipient = transferService.getUserByPhoneNumber(request.getRecipientPhoneNumber());

                if (recipient.isPresent()) {
                    RecipientValidationResponse response = new RecipientValidationResponse();
                    response.setValid(true);
                    response.setRecipientName(recipient.get().getFirstName());
                    response.setRecipientName(recipient.get().getLastName());
                    response.setRecipientPhone(recipient.get().getPhoneNumber());
                    response.setMessage("Recipient found and ready to receive transfer");

                    return ResponseEntity.ok(ApiResponse.success("Recipient validated successfully", response));
                }
            }

            // Recipient not found
            RecipientValidationResponse response = new RecipientValidationResponse();
            response.setValid(false);
            response.setMessage("Recipient phone number not found in our system");

            return ResponseEntity.ok(ApiResponse.success("Validation completed", response));

        } catch (Exception e) {
            logger.error("Error validating recipient {}: {}", request.getRecipientPhoneNumber(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to validate recipient: " + e.getMessage()));
        }
    }

    /**
     * Process money transfer
     * This endpoint handles the complete transfer process including balance validation
     */
    @PostMapping("/send")
    @Operation(summary = "Send money to another user",
            description = "Transfer money from authenticated user to recipient")
    public ResponseEntity<ApiResponse<Transaction>> sendMoney(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        String senderPhoneNumber = authentication.getName();
        logger.info("Transfer request from {} to {} for amount {}",
                senderPhoneNumber, request.getRecipientPhoneNumber(), request.getAmount());

        try {
            // Process the transfer
            Transaction transaction = transferService.processTransfer(senderPhoneNumber, request);

            logger.info("Transfer successful: {}", transaction.getReference());
            return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", transaction));

        } catch (RuntimeException e) {
            logger.error("Transfer failed from {} to {}: {}",
                    senderPhoneNumber, request.getRecipientPhoneNumber(), e.getMessage());

            // Return specific error messages based on the failure reason
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Transfer failed: " + e.getMessage()));
            } else if (e.getMessage().contains("Insufficient balance")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Transfer failed: " + e.getMessage()));
            } else if (e.getMessage().contains("Invalid PIN")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Transfer failed: " + e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Transfer failed: " + e.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Unexpected error during transfer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Transfer failed due to system error"));
        }
    }

    /**
     * Get transfer history
     */
    @GetMapping("/history")
    @Operation(summary = "Get transfer history",
            description = "Retrieve all transfer transactions for authenticated user")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransferHistory(
            Authentication authentication) {

        String phoneNumber = authentication.getName();
        logger.info("Transfer history request from user: {}", phoneNumber);

        try {
            List<Transaction> transfers = transferService.getTransferHistory(phoneNumber);
            return ResponseEntity.ok(ApiResponse.success("Transfer history retrieved successfully", transfers));

        } catch (Exception e) {
            logger.error("Failed to get transfer history for user {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve transfer history: " + e.getMessage()));
        }
    }

    /**
     * Check if phone number is registered
     */
    @GetMapping("/check-recipient/{phoneNumber}")
    @Operation(summary = "Quick recipient check",
            description = "Quickly check if a phone number is registered")
    public ResponseEntity<ApiResponse<Boolean>> checkRecipient(
            @PathVariable String phoneNumber) {

        logger.info("Quick recipient check for: {}", phoneNumber);

        try {
            boolean isRegistered = transferService.isPhoneNumberRegistered(phoneNumber);
            String message = isRegistered ? "Phone number is registered" : "Phone number not found";

            return ResponseEntity.ok(ApiResponse.success(message, isRegistered));

        } catch (Exception e) {
            logger.error("Error checking recipient {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check recipient"));
        }
    }
}
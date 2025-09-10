package com.example.gpay.controller;

import com.example.gpay.dto.ApiResponse;
import com.example.gpay.dto.DepositRequest;
import com.example.gpay.dto.TransferRequest;
import com.example.gpay.dto.WithdrawalRequest;
import com.example.gpay.model.Transaction;
import com.example.gpay.services.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/deposit")
    public Mono<ResponseEntity<ApiResponse<Transaction>>> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {

        String phoneNumber = authentication.getName();
        logger.info("Deposit request from user: {}, amount: {}", phoneNumber, request.getAmount());

        return transactionService.deposit(phoneNumber, request)
                .map(transaction -> {
                    logger.info("Deposit successful: {}", transaction.getReference());
                    return ResponseEntity.ok(ApiResponse.success("Deposit initiated successfully", transaction));
                })
                .onErrorResume(error -> {
                    logger.error("Deposit failed for user {}: {}", phoneNumber, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Deposit failed: " + error.getMessage())));
                });
    }



    @PostMapping("/withdraw")
    public Mono<ResponseEntity<ApiResponse<Transaction>>> withdraw(
            @Valid @RequestBody WithdrawalRequest request,
            Authentication authentication) {

        String phoneNumber = authentication.getName();
        logger.info("Withdrawal request from user: {}, amount: {}", phoneNumber, request.getAmount());

        return transactionService.withdraw(phoneNumber, request)
                .map(transaction -> {
                    logger.info("Withdrawal successful: {}", transaction.getReference());
                    return ResponseEntity.ok(ApiResponse.success("Withdrawal initiated successfully", transaction));
                })
                .onErrorResume(error -> {
                    logger.error("Withdrawal failed for user {}: {}", phoneNumber, error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Withdrawal failed: " + error.getMessage())));
                });
    }


    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Transaction>> transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        String phoneNumber = authentication.getName();
        logger.info("Transfer request from user: {}, to: {}, amount: {}",
                phoneNumber, request.getRecipientPhoneNumber(), request.getAmount());

        try {
            Transaction transaction = transactionService.transfer(phoneNumber, request);
            logger.info("Transfer successful: {}", transaction.getReference());
            return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", transaction));
        } catch (Exception e) {
            logger.error("Transfer failed for user {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Transfer failed: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactionHistory(
            Authentication authentication) {

        String phoneNumber = authentication.getName();
        logger.info("Transaction history request from user: {}", phoneNumber);

        try {
            List<Transaction> transactions = transactionService.getUserTransactions(phoneNumber);
            return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved successfully", transactions));
        } catch (Exception e) {
            logger.error("Failed to get transaction history for user {}: {}", phoneNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to get transaction history: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{reference}")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionStatus(
            @PathVariable String reference) {

        logger.info("Transaction status request for reference: {}", reference);

        try {
            Transaction transaction = transactionService.updateTransactionStatus(reference);
            return ResponseEntity.ok(ApiResponse.success("Transaction status retrieved successfully", transaction));
        } catch (Exception e) {
            logger.error("Failed to get transaction status for reference {}: {}", reference, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Transaction not found"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to get transaction status: " + e.getMessage()));
        }
    }
}
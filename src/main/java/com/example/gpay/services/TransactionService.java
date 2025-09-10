//package com.example.gpay.services;
//
//import com.example.gpay.dto.DepositRequest;
//import com.example.gpay.dto.TransferRequest;
//import com.example.gpay.dto.WithdrawalRequest;
//import com.example.gpay.model.Transaction;
//import com.example.gpay.model.TransactionStatus;
//import com.example.gpay.model.TransactionType;
//import com.example.gpay.model.User;
//import com.example.gpay.repository.TransactionRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.scheduling.annotation.Async;
//import reactor.core.publisher.Mono;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//public class TransactionService {
//
//    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
//
//    @Autowired
//    private TransactionRepository transactionRepository;
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private CampayService campayService;
//
//    @Transactional
//    public Mono<Transaction> deposit(String phoneNumber, DepositRequest request) {
//        try {
//            // Find user by phone number
//            User user = userService.findByPhoneNumber(phoneNumber)
//                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));
//
//            // Validate PIN
//            if (!userService.validatePin(user, request.getPin())) {
//                return Mono.error(new RuntimeException("Invalid PIN"));
//            }
//
//            // Create transaction record
//            Transaction transaction = new Transaction();
//            transaction.setUserId(user.getId());
//            transaction.setType(TransactionType.DEPOSIT);
//            transaction.setAmount(request.getAmount());
//            transaction.setDescription("Deposit via " + request.getProvider());
//            transaction.setStatus(TransactionStatus.PENDING);
//            transaction.setProvider(request.getProvider());
//            transaction.setReference(generateTransactionReference());
//
//            Transaction savedTransaction = transactionRepository.save(transaction);
//            logger.info("Created deposit transaction: {}", savedTransaction.getReference());
//
//            // Call Campay collect service
//            return campayService.collect(
//                    request.getAmount().toString(),
//                    user.getPhoneNumber(),
//                    "Deposit to wallet",
//                    savedTransaction.getReference()
//            ).map(campayResponse -> {
//                logger.info("Campay collect response: {}", campayResponse);
//
//                // Update transaction with external reference and status
//                savedTransaction.setExternalReference(campayResponse.getReference());
//                TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());
//                savedTransaction.setStatus(newStatus);
//                savedTransaction.setUpdatedAt(LocalDateTime.now());
//
//                // Save the transaction first
//                Transaction updatedTransaction = transactionRepository.save(savedTransaction);
//
//                // Start background monitoring for this transaction
//                startTransactionMonitoring(updatedTransaction);
//
//                // If already completed, update balance immediately
//                if (newStatus == TransactionStatus.COMPLETED) {
//                    updateBalanceForCompletedTransaction(updatedTransaction);
//                }
//
//                return updatedTransaction;
//            }).onErrorResume(error -> {
//                logger.error("Deposit failed for user {}: {}", phoneNumber, error.getMessage(), error);
//                savedTransaction.setStatus(TransactionStatus.FAILED);
//                savedTransaction.setUpdatedAt(LocalDateTime.now());
//                transactionRepository.save(savedTransaction);
//                return Mono.error(new RuntimeException("Deposit failed: " + error.getMessage()));
//            });
//
//        } catch (Exception e) {
//            logger.error("Error in deposit method: {}", e.getMessage(), e);
//            return Mono.error(new RuntimeException("Deposit failed: " + e.getMessage()));
//        }
//    }
//
//    @Transactional
//    public Mono<Transaction> withdraw(String phoneNumber, WithdrawalRequest request) {
//        try {
//            // Find user by phone number
//            User user = userService.findByPhoneNumber(phoneNumber)
//                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));
//
//            // Validate PIN
//            if (!userService.validatePin(user, request.getPin())) {
//                return Mono.error(new RuntimeException("Invalid PIN"));
//            }
//
//            // Check sufficient balance
//            if (user.getBalance() < request.getAmount()) {
//                return Mono.error(new RuntimeException("Insufficient balance. Current balance: " + user.getBalance()));
//            }
//
//            // Create transaction record
//            Transaction transaction = new Transaction();
//            transaction.setUserId(user.getId());
//            transaction.setType(TransactionType.WITHDRAWAL);
//            transaction.setAmount(request.getAmount());
//            transaction.setDescription("Withdrawal via " + request.getProvider());
//            transaction.setStatus(TransactionStatus.PENDING);
//            transaction.setProvider(request.getProvider());
//            transaction.setReference(generateTransactionReference());
//
//            Transaction savedTransaction = transactionRepository.save(transaction);
//            logger.info("Created withdrawal transaction: {}", savedTransaction.getReference());
//
//            // Call Campay disburse service
//            return campayService.disburse(
//                    request.getAmount().toString(),
//                    user.getPhoneNumber(),
//                    "Withdrawal from wallet",
//                    savedTransaction.getReference()
//            ).map(campayResponse -> {
//                logger.info("Campay disburse response: {}", campayResponse);
//
//                // Update transaction with external reference and status
//                savedTransaction.setExternalReference(campayResponse.getReference());
//                TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());
//                savedTransaction.setStatus(newStatus);
//                savedTransaction.setUpdatedAt(LocalDateTime.now());
//
//                // Save the transaction first
//                Transaction updatedTransaction = transactionRepository.save(savedTransaction);
//
//                // Start background monitoring for this transaction
//                startTransactionMonitoring(updatedTransaction);
//
//                // If already completed, update balance immediately
//                if (newStatus == TransactionStatus.COMPLETED) {
//                    updateBalanceForCompletedTransaction(updatedTransaction);
//                }
//
//                return updatedTransaction;
//            }).onErrorResume(error -> {
//                logger.error("Withdrawal failed for user {}: {}", phoneNumber, error.getMessage(), error);
//                savedTransaction.setStatus(TransactionStatus.FAILED);
//                savedTransaction.setUpdatedAt(LocalDateTime.now());
//                transactionRepository.save(savedTransaction);
//                return Mono.error(new RuntimeException("Withdrawal failed: " + error.getMessage()));
//            });
//
//        } catch (Exception e) {
//            logger.error("Error in withdraw method: {}", e.getMessage(), e);
//            return Mono.error(new RuntimeException("Withdrawal failed: " + e.getMessage()));
//        }
//    }
//
//    /**
//     * Background monitoring for pending transactions
//     */
//    @Async
//    public void startTransactionMonitoring(Transaction transaction) {
//        if (transaction.getStatus() == TransactionStatus.COMPLETED ||
//                transaction.getStatus() == TransactionStatus.FAILED ||
//                transaction.getExternalReference() == null) {
//            return;
//        }
//
//        logger.info("Starting background monitoring for transaction: {}", transaction.getReference());
//
//        CompletableFuture.runAsync(() -> {
//            monitorTransactionStatus(transaction);
//        });
//    }
//
//    /**
//     * Monitor transaction status and update balance when completed
//     */
//    private void monitorTransactionStatus(Transaction transaction) {
//        final int maxAttempts = 10;
//        final long[] delays = {5000, 10000, 15000, 30000, 60000, 120000, 300000, 600000, 900000, 1800000}; // Progressive delays
//
//        for (int attempt = 0; attempt < maxAttempts; attempt++) {
//            try {
//                // Wait before checking (except first attempt)
//                if (attempt > 0) {
//                    Thread.sleep(delays[Math.min(attempt - 1, delays.length - 1)]);
//                }
//
//                logger.debug("Monitoring attempt {} for transaction: {}", attempt + 1, transaction.getReference());
//
//                // Check current status from database
//                Transaction currentTransaction = transactionRepository.findByReference(transaction.getReference())
//                        .orElse(null);
//
//                if (currentTransaction == null) {
//                    logger.warn("Transaction not found in database: {}", transaction.getReference());
//                    break;
//                }
//
//                // If already completed or failed, stop monitoring
//                if (currentTransaction.getStatus() == TransactionStatus.COMPLETED ||
//                        currentTransaction.getStatus() == TransactionStatus.FAILED) {
//                    logger.info("Transaction {} already in final state: {}",
//                            transaction.getReference(), currentTransaction.getStatus());
//                    break;
//                }
//
//                // Check status from Campay
//                var campayResponse = campayService.getTransactionStatus(currentTransaction.getExternalReference()).block();
//
//                if (campayResponse != null) {
//                    TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());
//                    logger.info("Transaction {} status check: {} -> {}",
//                            transaction.getReference(), currentTransaction.getStatus(), newStatus);
//
//                    if (newStatus != currentTransaction.getStatus()) {
//                        // Update transaction status
//                        currentTransaction.setStatus(newStatus);
//                        currentTransaction.setUpdatedAt(LocalDateTime.now());
//                        transactionRepository.save(currentTransaction);
//
//                        // If newly completed, update balance
//                        if (newStatus == TransactionStatus.COMPLETED) {
//                            updateBalanceForCompletedTransaction(currentTransaction);
//                            logger.info("Transaction {} monitoring completed successfully", transaction.getReference());
//                            break;
//                        }
//
//                        // If failed, stop monitoring
//                        if (newStatus == TransactionStatus.FAILED) {
//                            logger.info("Transaction {} failed, stopping monitoring", transaction.getReference());
//                            break;
//                        }
//                    }
//                }
//
//            } catch (Exception e) {
//                logger.error("Error monitoring transaction {}: {}", transaction.getReference(), e.getMessage());
//
//                // If it's the last attempt, mark as failed
//                if (attempt == maxAttempts - 1) {
//                    try {
//                        Transaction failedTransaction = transactionRepository.findByReference(transaction.getReference())
//                                .orElse(null);
//                        if (failedTransaction != null && failedTransaction.getStatus() == TransactionStatus.PENDING) {
//                            failedTransaction.setStatus(TransactionStatus.FAILED);
//                            failedTransaction.setUpdatedAt(LocalDateTime.now());
//                            transactionRepository.save(failedTransaction);
//                            logger.warn("Transaction {} marked as failed after {} monitoring attempts",
//                                    transaction.getReference(), maxAttempts);
//                        }
//                    } catch (Exception ex) {
//                        logger.error("Error marking transaction as failed: {}", ex.getMessage());
//                    }
//                }
//            }
//        }
//    }
//
//    @Transactional
//    public Transaction transfer(String phoneNumber, TransferRequest request) {
//        try {
//            // Find sender by phone number
//            User sender = userService.findByPhoneNumber(phoneNumber)
//                    .orElseThrow(() -> new RuntimeException("Sender not found with phone: " + phoneNumber));
//
//            // Find recipient by phone number
//            User recipient = userService.findByPhoneNumber(request.getRecipientPhoneNumber())
//                    .orElseThrow(() -> new RuntimeException("Recipient not found with phone: " + request.getRecipientPhoneNumber()));
//
//            // Validate PIN
//            if (!userService.validatePin(sender, request.getPin())) {
//                throw new RuntimeException("Invalid PIN");
//            }
//
//            // Check sufficient balance
//            if (sender.getBalance() < request.getAmount()) {
//                throw new RuntimeException("Insufficient balance. Current balance: " + sender.getBalance());
//            }
//
//            // Prevent self-transfer
//            if (sender.getId().equals(recipient.getId())) {
//                throw new RuntimeException("Cannot transfer to yourself");
//            }
//
//            // Create transaction record
//            Transaction transaction = new Transaction();
//            transaction.setUserId(sender.getId());
//            transaction.setType(TransactionType.TRANSFER);
//            transaction.setAmount(request.getAmount());
//            transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Transfer to " + request.getRecipientPhoneNumber());
//            transaction.setStatus(TransactionStatus.COMPLETED);
//            transaction.setRecipientId(recipient.getId());
//            transaction.setRecipientPhoneNumber(recipient.getPhoneNumber());
//            transaction.setReference(generateTransactionReference());
//
//            Transaction savedTransaction = transactionRepository.save(transaction);
//            logger.info("Created transfer transaction: {}", savedTransaction.getReference());
//
//            // Update balances
//            double senderNewBalance = sender.getBalance() - request.getAmount();
//            double recipientNewBalance = recipient.getBalance() + request.getAmount();
//
//            userService.updateBalance(sender.getId(), senderNewBalance);
//            userService.updateBalance(recipient.getId(), recipientNewBalance);
//
//            logger.info("Transfer completed - Sender balance: {}, Recipient balance: {}",
//                    senderNewBalance, recipientNewBalance);
//
//            return savedTransaction;
//
//        } catch (Exception e) {
//            logger.error("Error in transfer method: {}", e.getMessage(), e);
//            throw new RuntimeException("Transfer failed: " + e.getMessage());
//        }
//    }
//
//    public List<Transaction> getUserTransactions(String phoneNumber) {
//        try {
//            User user = userService.findByPhoneNumber(phoneNumber)
//                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));
//
//            return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
//        } catch (Exception e) {
//            logger.error("Error getting user transactions: {}", e.getMessage(), e);
//            throw new RuntimeException("Failed to get transactions: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Manual transaction status update (kept for backward compatibility)
//     */
//    @Transactional
//    public Transaction updateTransactionStatus(String reference) {
//        try {
//            // Fetch transaction by reference or externalReference
//            Transaction transaction = transactionRepository.findByReference(reference)
//                    .or(() -> transactionRepository.findByExternalReference(reference))
//                    .orElseThrow(() -> new RuntimeException("Transaction not found with reference: " + reference));
//
//            // If transaction has an external reference, check status from Campay
//            if (transaction.getExternalReference() != null) {
//                var campayResponse = campayService.getTransactionStatus(transaction.getExternalReference()).block();
//                if (campayResponse == null) {
//                    throw new RuntimeException("Campay response is null");
//                }
//
//                TransactionStatus oldStatus = transaction.getStatus();
//                TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());
//
//                logger.info("Manual transaction {} status update: {} -> {}",
//                        reference, oldStatus, newStatus);
//
//                transaction.setStatus(newStatus);
//                transaction.setUpdatedAt(LocalDateTime.now());
//
//                // If newly marked as COMPLETED, update user balance
//                if (oldStatus != TransactionStatus.COMPLETED && newStatus == TransactionStatus.COMPLETED) {
//                    updateBalanceForCompletedTransaction(transaction);
//                }
//
//                return transactionRepository.save(transaction);
//            } else {
//                logger.warn("Transaction {} has no external reference", reference);
//                return transaction;
//            }
//
//        } catch (Exception e) {
//            logger.error("Error updating transaction status: {}", e.getMessage(), e);
//            throw new RuntimeException("Failed to update transaction status: " + e.getMessage());
//        }
//    }
//
//    private void updateBalanceForCompletedTransaction(Transaction transaction) {
//        try {
//            logger.info("Updating balance for user {} from transaction {}",
//                    transaction.getUserId(), transaction.getReference());
//
//            User user = userService.findById(transaction.getUserId())
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            double newBalance = user.getBalance();
//
//            if (transaction.getType() == TransactionType.DEPOSIT) {
//                newBalance += transaction.getAmount();
//                logger.info("Adding {} to balance for deposit transaction", transaction.getAmount());
//            } else if (transaction.getType() == TransactionType.WITHDRAWAL) {
//                newBalance -= transaction.getAmount();
//                logger.info("Subtracting {} from balance for withdrawal transaction", transaction.getAmount());
//            }
//
//            userService.updateBalance(user.getId(), newBalance);
//            logger.info("Balance updated successfully for transaction: {} - New balance: {}",
//                    transaction.getReference(), newBalance);
//        } catch (Exception e) {
//            logger.error("Error updating balance for completed transaction: {}", e.getMessage(), e);
//        }
//    }
//
//    private TransactionStatus mapCampayStatus(String campayStatus) {
//        if (campayStatus == null) {
//            return TransactionStatus.PENDING;
//        }
//
//        switch (campayStatus.toUpperCase()) {
//            case "SUCCESSFUL":
//            case "SUCCESS":
//            case "COMPLETED":
//                return TransactionStatus.COMPLETED;
//            case "PENDING":
//                return TransactionStatus.PENDING;
//            case "FAILED":
//            case "FAILURE":
//                return TransactionStatus.FAILED;
//            case "PROCESSING":
//                return TransactionStatus.PROCESSING;
//            default:
//                logger.warn("Unknown Campay status: {}", campayStatus);
//                return TransactionStatus.PROCESSING;
//        }
//    }
//
//    private String generateTransactionReference() {
//        return "TXN" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
//    }
//}


package com.example.gpay.services;

import com.example.gpay.dto.DepositRequest;
import com.example.gpay.dto.TransferRequest;
import com.example.gpay.dto.WithdrawalRequest;
import com.example.gpay.model.Transaction;
import com.example.gpay.model.TransactionStatus;
import com.example.gpay.model.TransactionType;
import com.example.gpay.model.User;
import com.example.gpay.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CampayService campayService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Mono<Transaction> deposit(String phoneNumber, DepositRequest request) {
        try {
            // Find user by phone number
            User user = userService.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));

            // Validate PIN
            if (!userService.validatePin(user, request.getPin())) {
                return Mono.error(new RuntimeException("Invalid PIN"));
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setUserId(user.getId());
            transaction.setType(TransactionType.DEPOSIT);
            transaction.setAmount(request.getAmount());
            transaction.setDescription("Deposit via " + request.getProvider());
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setProvider(request.getProvider());
            transaction.setReference(generateTransactionReference());

            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("Created deposit transaction: {}", savedTransaction.getReference());

            // Send pending deposit notification
            notificationService.sendDepositNotification(user.getPhoneNumber(), request.getAmount(), "PENDING");

            // Call Campay collect service
            return campayService.collect(
                    request.getAmount().toString(),
                    user.getPhoneNumber(),
                    "Deposit to wallet",
                    savedTransaction.getReference()
            ).map(campayResponse -> {
                logger.info("Campay collect response: {}", campayResponse);

                // Update transaction with external reference and status
                savedTransaction.setExternalReference(campayResponse.getReference());
                TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());
                savedTransaction.setStatus(newStatus);
                savedTransaction.setUpdatedAt(LocalDateTime.now());

                // Save the transaction first
                Transaction updatedTransaction = transactionRepository.save(savedTransaction);

                // Start background monitoring for this transaction
                startTransactionMonitoring(updatedTransaction);

                // If already completed, update balance immediately and send notification
                if (newStatus == TransactionStatus.COMPLETED) {
                    updateBalanceForCompletedTransaction(updatedTransaction);
                    notificationService.sendDepositNotification(user.getPhoneNumber(), request.getAmount(), "COMPLETED");
                }

                return updatedTransaction;
            }).onErrorResume(error -> {
                logger.error("Deposit failed for user {}: {}", phoneNumber, error.getMessage(), error);
                savedTransaction.setStatus(TransactionStatus.FAILED);
                savedTransaction.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(savedTransaction);

                // Send failure notification
                notificationService.sendDepositNotification(user.getPhoneNumber(), request.getAmount(), "FAILED");

                return Mono.error(new RuntimeException("Deposit failed: " + error.getMessage()));
            });

        } catch (Exception e) {
            logger.error("Error in deposit method: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Deposit failed: " + e.getMessage()));
        }
    }

    @Transactional
    public Mono<Transaction> withdraw(String phoneNumber, WithdrawalRequest request) {
        try {
            // Find user by phone number
            User user = userService.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));

            // Validate PIN
            if (!userService.validatePin(user, request.getPin())) {
                return Mono.error(new RuntimeException("Invalid PIN"));
            }

            // Check sufficient balance
            if (user.getBalance() < request.getAmount()) {
                return Mono.error(new RuntimeException("Insufficient balance. Current balance: " + user.getBalance()));
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setUserId(user.getId());
            transaction.setType(TransactionType.WITHDRAWAL);
            transaction.setAmount(request.getAmount());
            transaction.setDescription("Withdrawal via " + request.getProvider());
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setProvider(request.getProvider());
            transaction.setReference(generateTransactionReference());

            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("Created withdrawal transaction: {}", savedTransaction.getReference());

            // Send pending withdrawal notification
            notificationService.sendWithdrawalNotification(user.getPhoneNumber(), request.getAmount(), "PENDING");

            // Call Campay disburse service
            return campayService.disburse(
                    request.getAmount().toString(),
                    user.getPhoneNumber(),
                    "Withdrawal from wallet",
                    savedTransaction.getReference()
            ).map(campayResponse -> {
                logger.info("Campay disburse response: {}", campayResponse);

                // Update transaction with external reference and status
                savedTransaction.setExternalReference(campayResponse.getReference());
                TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());
                savedTransaction.setStatus(newStatus);
                savedTransaction.setUpdatedAt(LocalDateTime.now());

                // Save the transaction first
                Transaction updatedTransaction = transactionRepository.save(savedTransaction);

                // Start background monitoring for this transaction
                startTransactionMonitoring(updatedTransaction);

                // If already completed, update balance immediately and send notification
                if (newStatus == TransactionStatus.COMPLETED) {
                    updateBalanceForCompletedTransaction(updatedTransaction);
                    notificationService.sendWithdrawalNotification(user.getPhoneNumber(), request.getAmount(), "COMPLETED");
                }

                return updatedTransaction;
            }).onErrorResume(error -> {
                logger.error("Withdrawal failed for user {}: {}", phoneNumber, error.getMessage(), error);
                savedTransaction.setStatus(TransactionStatus.FAILED);
                savedTransaction.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(savedTransaction);

                // Send failure notification
                notificationService.sendWithdrawalNotification(user.getPhoneNumber(), request.getAmount(), "FAILED");

                return Mono.error(new RuntimeException("Withdrawal failed: " + error.getMessage()));
            });

        } catch (Exception e) {
            logger.error("Error in withdraw method: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Withdrawal failed: " + e.getMessage()));
        }
    }

    /**
     * Background monitoring for pending transactions
     */
    @Async
    public void startTransactionMonitoring(Transaction transaction) {
        if (transaction.getStatus() == TransactionStatus.COMPLETED ||
                transaction.getStatus() == TransactionStatus.FAILED ||
                transaction.getExternalReference() == null) {
            return;
        }

        logger.info("Starting background monitoring for transaction: {}", transaction.getReference());

        CompletableFuture.runAsync(() -> {
            monitorTransactionStatus(transaction);
        });
    }

    /**
     * Monitor transaction status and update balance when completed
     */
    private void monitorTransactionStatus(Transaction transaction) {
        final int maxAttempts = 10;
        final long[] delays = {5000, 10000, 15000, 30000, 60000, 120000, 300000, 600000, 900000, 1800000}; // Progressive delays

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                // Wait before checking (except first attempt)
                if (attempt > 0) {
                    Thread.sleep(delays[Math.min(attempt - 1, delays.length - 1)]);
                }

                logger.debug("Monitoring attempt {} for transaction: {}", attempt + 1, transaction.getReference());

                // Check current status from database
                Transaction currentTransaction = transactionRepository.findByReference(transaction.getReference())
                        .orElse(null);

                if (currentTransaction == null) {
                    logger.warn("Transaction not found in database: {}", transaction.getReference());
                    break;
                }

                // If already completed or failed, stop monitoring
                if (currentTransaction.getStatus() == TransactionStatus.COMPLETED ||
                        currentTransaction.getStatus() == TransactionStatus.FAILED) {
                    logger.info("Transaction {} already in final state: {}",
                            transaction.getReference(), currentTransaction.getStatus());
                    break;
                }

                // Check status from Campay
                var campayResponse = campayService.getTransactionStatus(currentTransaction.getExternalReference()).block();

                if (campayResponse != null) {
                    TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());
                    logger.info("Transaction {} status check: {} -> {}",
                            transaction.getReference(), currentTransaction.getStatus(), newStatus);

                    if (newStatus != currentTransaction.getStatus()) {
                        // Update transaction status
                        currentTransaction.setStatus(newStatus);
                        currentTransaction.setUpdatedAt(LocalDateTime.now());
                        transactionRepository.save(currentTransaction);

                        // Get user for notifications
                        User user = userService.findById(currentTransaction.getUserId()).orElse(null);
                        if (user != null) {
                            // If newly completed, update balance and send notification
                            if (newStatus == TransactionStatus.COMPLETED) {
                                updateBalanceForCompletedTransaction(currentTransaction);
                                sendCompletionNotification(user, currentTransaction);
                                logger.info("Transaction {} monitoring completed successfully", transaction.getReference());
                                break;
                            }

                            // If failed, send notification
                            if (newStatus == TransactionStatus.FAILED) {
                                sendFailureNotification(user, currentTransaction);
                                logger.info("Transaction {} failed, stopping monitoring", transaction.getReference());
                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Error monitoring transaction {}: {}", transaction.getReference(), e.getMessage());

                // If it's the last attempt, mark as failed
                if (attempt == maxAttempts - 1) {
                    try {
                        Transaction failedTransaction = transactionRepository.findByReference(transaction.getReference())
                                .orElse(null);
                        if (failedTransaction != null && failedTransaction.getStatus() == TransactionStatus.PENDING) {
                            failedTransaction.setStatus(TransactionStatus.FAILED);
                            failedTransaction.setUpdatedAt(LocalDateTime.now());
                            transactionRepository.save(failedTransaction);

                            // Send failure notification
                            User user = userService.findById(failedTransaction.getUserId()).orElse(null);
                            if (user != null) {
                                sendFailureNotification(user, failedTransaction);
                            }

                            logger.warn("Transaction {} marked as failed after {} monitoring attempts",
                                    transaction.getReference(), maxAttempts);
                        }
                    } catch (Exception ex) {
                        logger.error("Error marking transaction as failed: {}", ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Send completion notification based on transaction type
     */
    private void sendCompletionNotification(User user, Transaction transaction) {
        try {
            switch (transaction.getType()) {
                case DEPOSIT:
                    notificationService.sendDepositNotification(user.getPhoneNumber(), transaction.getAmount(), "COMPLETED");
                    break;
                case WITHDRAWAL:
                    notificationService.sendWithdrawalNotification(user.getPhoneNumber(), transaction.getAmount(), "COMPLETED");
                    break;
                default:
                    logger.debug("No specific completion notification for transaction type: {}", transaction.getType());
            }
        } catch (Exception e) {
            logger.error("Error sending completion notification: {}", e.getMessage());
        }
    }

    /**
     * Send failure notification based on transaction type
     */
    private void sendFailureNotification(User user, Transaction transaction) {
        try {
            switch (transaction.getType()) {
                case DEPOSIT:
                    notificationService.sendDepositNotification(user.getPhoneNumber(), transaction.getAmount(), "FAILED");
                    break;
                case WITHDRAWAL:
                    notificationService.sendWithdrawalNotification(user.getPhoneNumber(), transaction.getAmount(), "FAILED");
                    break;
                default:
                    notificationService.sendTransactionFailureNotification(user.getPhoneNumber(), "Transaction processing failed");
            }
        } catch (Exception e) {
            logger.error("Error sending failure notification: {}", e.getMessage());
        }
    }

    @Transactional
    public Transaction transfer(String phoneNumber, TransferRequest request) {
        try {
            // Find sender by phone number
            User sender = userService.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("Sender not found with phone: " + phoneNumber));

            // Find recipient by phone number
            User recipient = userService.findByPhoneNumber(request.getRecipientPhoneNumber())
                    .orElseThrow(() -> new RuntimeException("Recipient not found with phone: " + request.getRecipientPhoneNumber()));

            // Validate PIN
            if (!userService.validatePin(sender, request.getPin())) {
                throw new RuntimeException("Invalid PIN");
            }

            // Check sufficient balance
            if (sender.getBalance() < request.getAmount()) {
                throw new RuntimeException("Insufficient balance. Current balance: " + sender.getBalance());
            }

            // Prevent self-transfer
            if (sender.getId().equals(recipient.getId())) {
                throw new RuntimeException("Cannot transfer to yourself");
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setUserId(sender.getId());
            transaction.setType(TransactionType.TRANSFER);
            transaction.setAmount(request.getAmount());
            transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Transfer to " + request.getRecipientPhoneNumber());
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setRecipientId(recipient.getId());
            transaction.setRecipientPhoneNumber(recipient.getPhoneNumber());
            transaction.setReference(generateTransactionReference());

            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("Created transfer transaction: {}", savedTransaction.getReference());

            // Update balances
            double senderNewBalance = sender.getBalance() - request.getAmount();
            double recipientNewBalance = recipient.getBalance() + request.getAmount();

            userService.updateBalance(sender.getId(), senderNewBalance);
            userService.updateBalance(recipient.getId(), recipientNewBalance);

            logger.info("Transfer completed - Sender balance: {}, Recipient balance: {}",
                    senderNewBalance, recipientNewBalance);

            // Send notifications to both sender and recipient
            notificationService.sendTransferSentNotification(sender.getPhoneNumber(), request.getAmount(), recipient.getPhoneNumber());
            notificationService.sendTransferReceivedNotification(recipient.getPhoneNumber(), request.getAmount(), sender.getPhoneNumber());

            // Send balance update notifications
            notificationService.sendBalanceUpdateNotification(sender.getPhoneNumber(), senderNewBalance);
            notificationService.sendBalanceUpdateNotification(recipient.getPhoneNumber(), recipientNewBalance);

            return savedTransaction;

        } catch (Exception e) {
            logger.error("Error in transfer method: {}", e.getMessage(), e);
            throw new RuntimeException("Transfer failed: " + e.getMessage());
        }
    }

    public List<Transaction> getUserTransactions(String phoneNumber) {
        try {
            User user = userService.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + phoneNumber));

            return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        } catch (Exception e) {
            logger.error("Error getting user transactions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get transactions: " + e.getMessage());
        }
    }

    /**
     * Manual transaction status update (kept for backward compatibility)
     */
    @Transactional
    public Transaction updateTransactionStatus(String reference) {
        try {
            // Fetch transaction by reference or externalReference
            Transaction transaction = transactionRepository.findByReference(reference)
                    .or(() -> transactionRepository.findByExternalReference(reference))
                    .orElseThrow(() -> new RuntimeException("Transaction not found with reference: " + reference));

            // If transaction has an external reference, check status from Campay
            if (transaction.getExternalReference() != null) {
                var campayResponse = campayService.getTransactionStatus(transaction.getExternalReference()).block();
                if (campayResponse == null) {
                    throw new RuntimeException("Campay response is null");
                }

                TransactionStatus oldStatus = transaction.getStatus();
                TransactionStatus newStatus = mapCampayStatus(campayResponse.getStatus());

                logger.info("Manual transaction {} status update: {} -> {}",
                        reference, oldStatus, newStatus);

                transaction.setStatus(newStatus);
                transaction.setUpdatedAt(LocalDateTime.now());

                // If newly marked as COMPLETED, update user balance and send notification
                if (oldStatus != TransactionStatus.COMPLETED && newStatus == TransactionStatus.COMPLETED) {
                    updateBalanceForCompletedTransaction(transaction);

                    // Send completion notification
                    User user = userService.findById(transaction.getUserId()).orElse(null);
                    if (user != null) {
                        sendCompletionNotification(user, transaction);
                    }
                }

                return transactionRepository.save(transaction);
            } else {
                logger.warn("Transaction {} has no external reference", reference);
                return transaction;
            }

        } catch (Exception e) {
            logger.error("Error updating transaction status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update transaction status: " + e.getMessage());
        }
    }

    private void updateBalanceForCompletedTransaction(Transaction transaction) {
        try {
            logger.info("Updating balance for user {} from transaction {}",
                    transaction.getUserId(), transaction.getReference());

            User user = userService.findById(transaction.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            double newBalance = user.getBalance();

            if (transaction.getType() == TransactionType.DEPOSIT) {
                newBalance += transaction.getAmount();
                logger.info("Adding {} to balance for deposit transaction", transaction.getAmount());
            } else if (transaction.getType() == TransactionType.WITHDRAWAL) {
                newBalance -= transaction.getAmount();
                logger.info("Subtracting {} from balance for withdrawal transaction", transaction.getAmount());
            }

            userService.updateBalance(user.getId(), newBalance);
            logger.info("Balance updated successfully for transaction: {} - New balance: {}",
                    transaction.getReference(), newBalance);
        } catch (Exception e) {
            logger.error("Error updating balance for completed transaction: {}", e.getMessage(), e);
        }
    }

    private TransactionStatus mapCampayStatus(String campayStatus) {
        if (campayStatus == null) {
            return TransactionStatus.PENDING;
        }

        switch (campayStatus.toUpperCase()) {
            case "SUCCESSFUL":
            case "SUCCESS":
            case "COMPLETED":
                return TransactionStatus.COMPLETED;
            case "PENDING":
                return TransactionStatus.PENDING;
            case "FAILED":
            case "FAILURE":
                return TransactionStatus.FAILED;
            case "PROCESSING":
                return TransactionStatus.PROCESSING;
            default:
                logger.warn("Unknown Campay status: {}", campayStatus);
                return TransactionStatus.PROCESSING;
        }
    }

    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}
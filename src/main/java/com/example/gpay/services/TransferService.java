package com.example.gpay.services;

import com.example.gpay.dto.TransferRequest;
import com.example.gpay.model.Transaction;
import com.example.gpay.model.TransactionStatus;
import com.example.gpay.model.TransactionType;
import com.example.gpay.model.User;
import com.example.gpay.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    private final TransactionRepository transactionRepository;

    private final UserService userService;

    private final NotificationService notificationService;

    /**
     * Process money transfer between users
     * This method implements all the requirements:
     * 1. Check sender's balance
     * 2. Verify recipient exists in database
     * 3. Deduct money from sender
     * 4. Credit money to recipient
     * 5. Create transaction records
     */
    @Transactional
    public Transaction processTransfer(String senderPhoneNumber, TransferRequest request) {
        logger.info("Processing transfer from {} to {} for amount {}",
                senderPhoneNumber, request.getRecipientPhoneNumber(), request.getAmount());

        try {
            // Step 1: Find and validate sender
            User sender = findUserByPhoneNumber(senderPhoneNumber);
            logger.info("Sender found: {}", sender.getPhoneNumber());

            // Step 2: Find and validate recipient
            User recipient = findUserByPhoneNumber(request.getRecipientPhoneNumber());
            logger.info("Recipient found: {}", recipient.getPhoneNumber());

            // Step 3: Validate PIN
            validateUserPin(sender, request.getPin());

            // Step 4: Check sufficient balance
            validateSufficientBalance(sender, request.getAmount());

            // Step 5: Prevent self-transfer
            validateNotSelfTransfer(sender, recipient);

            // Step 6: Create transaction record
            Transaction transaction = createTransactionRecord(sender, recipient, request);

            // Step 7: Process the actual transfer (debit sender, credit recipient)
            processBalanceTransfer(sender, recipient, request.getAmount());

            // Step 8: Save transaction and update status
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setUpdatedAt(LocalDateTime.now());
            Transaction savedTransaction = transactionRepository.save(transaction);

            // Step 9: Send notifications
            sendTransferNotifications(sender, recipient, request.getAmount(), savedTransaction.getReference());

            logger.info("Transfer completed successfully. Reference: {}", savedTransaction.getReference());
            return savedTransaction;

        } catch (Exception e) {
            logger.error("Transfer failed from {} to {}: {}",
                    senderPhoneNumber, request.getRecipientPhoneNumber(), e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        }
    }

    /**
     * Find user by phone number with proper error handling
     */
    private User findUserByPhoneNumber(String phoneNumber) {
        return userService.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found with phone number: " + phoneNumber));
    }

    /**
     * Validate user PIN
     */
    private void validateUserPin(User user, String pin) {
        if (!userService.validatePin(user, pin)) {
            logger.warn("Invalid PIN attempt for user: {}", user.getPhoneNumber());
            throw new RuntimeException("Invalid PIN provided");
        }
    }

    /**
     * Check if sender has sufficient balance
     */
    private void validateSufficientBalance(User sender, Double amount) {
        if (sender.getBalance() < amount) {
            logger.warn("Insufficient balance for user {}. Required: {}, Available: {}",
                    sender.getPhoneNumber(), amount, sender.getBalance());
            throw new RuntimeException(String.format(
                    "Insufficient balance. Required: %.2f, Available: %.2f",
                    amount, sender.getBalance()));
        }
    }

    /**
     * Prevent users from transferring to themselves
     */
    private void validateNotSelfTransfer(User sender, User recipient) {
        if (sender.getId().equals(recipient.getId())) {
            logger.warn("Self-transfer attempt by user: {}", sender.getPhoneNumber());
            throw new RuntimeException("Cannot transfer money to yourself");
        }
    }

    /**
     * Create transaction record with all necessary details
     */
    private Transaction createTransactionRecord(User sender, User recipient, TransferRequest request) {
        Transaction transaction = new Transaction();
        transaction.setUserId(sender.getId());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription() != null ?
                request.getDescription() : "Transfer to " + recipient.getPhoneNumber());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setRecipientId(recipient.getId());
        transaction.setRecipientPhoneNumber(recipient.getPhoneNumber());
        transaction.setReference(generateTransactionReference());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    /**
     * Process the actual balance transfer
     */
    private void processBalanceTransfer(User sender, User recipient, Double amount) {
        try {
            // Calculate new balances
            double senderNewBalance = sender.getBalance() - amount;
            double recipientNewBalance = recipient.getBalance() + amount;

            // Update sender's balance (debit)
            userService.updateBalance(sender.getId(), senderNewBalance);
            logger.info("Debited {} from sender {}. New balance: {}",
                    amount, sender.getPhoneNumber(), senderNewBalance);

            // Update recipient's balance (credit)
            userService.updateBalance(recipient.getId(), recipientNewBalance);
            logger.info("Credited {} to recipient {}. New balance: {}",
                    amount, recipient.getPhoneNumber(), recipientNewBalance);

        } catch (Exception e) {
            logger.error("Failed to update balances during transfer: {}", e.getMessage());
            throw new RuntimeException("Failed to process balance transfer", e);
        }
    }

    /**
     * Send notifications to both sender and recipient
     */
    private void sendTransferNotifications(User sender, User recipient, Double amount, String reference) {
        try {
            // Notify sender
            notificationService.sendTransferNotification(
                    sender.getPhoneNumber(),
                    String.format("Transfer successful! You sent %.2f to %s. Reference: %s",
                            amount, recipient.getPhoneNumber(), reference),
                    "TRANSFER_SENT"
            );

            // Notify recipient
            notificationService.sendTransferNotification(
                    recipient.getPhoneNumber(),
                    String.format("Money received! You received %.2f from %s. Reference: %s",
                            amount, sender.getPhoneNumber(), reference),
                    "TRANSFER_RECEIVED"
            );

        } catch (Exception e) {
            logger.warn("Failed to send transfer notifications: {}", e.getMessage());
            // Don't fail the transfer if notifications fail
        }
    }

    /**
     * Generate unique transaction reference
     */
    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    /**
     * Get transfer history for a user
     */
    public List<Transaction> getTransferHistory(String phoneNumber) {
        try {
            User user = findUserByPhoneNumber(phoneNumber);
            return transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    user.getId(), TransactionType.TRANSFER);
        } catch (Exception e) {
            logger.error("Failed to get transfer history for user {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to get transfer history", e);
        }
    }

    /**
     * Check if a phone number is registered (for recipient validation)
     */
    public boolean isPhoneNumberRegistered(String phoneNumber) {
        return userService.findByPhoneNumber(phoneNumber).isPresent();
    }

    /**
     * Get user details by phone number (for recipient verification)
     */
    public Optional<User> getUserByPhoneNumber(String phoneNumber) {
        return userService.findByPhoneNumber(phoneNumber);
    }
}
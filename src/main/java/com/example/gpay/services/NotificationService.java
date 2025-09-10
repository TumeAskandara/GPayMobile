package com.example.gpay.services;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @Value("${twilio.enabled:true}")
    private boolean smsEnabled;

    // In-memory OTP storage (use Redis in production)
    private final Map<String, OTPData> otpStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // OTP Configuration
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_MINUTES = 5;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Generate and send OTP for login
     */
    public String generateAndSendOTP(String phoneNumber) {
        try {
            // Generate 6-digit OTP
            String otp = String.format("%06d", random.nextInt(1000000));

            // Store OTP with expiry
            OTPData otpData = new OTPData(otp, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            otpStorage.put(phoneNumber, otpData);

            // Send OTP via SMS
            String message = String.format("Your GPay login OTP is: %s. Valid for %d minutes. Do not share this code.",
                    otp, OTP_EXPIRY_MINUTES);

            if (smsEnabled) {
                sendSmsNotification(phoneNumber, message);
                logger.info("OTP sent successfully to {}", phoneNumber);
            } else {
                logger.info("SMS disabled. OTP for {}: {}", phoneNumber, otp);
            }

            return otp; // Return OTP only for testing/development
        } catch (Exception e) {
            logger.error("Failed to generate and send OTP to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send OTP", e);
        }
    }

    /**
     * Verify OTP for login
     */
    public boolean verifyOTP(String phoneNumber, String providedOtp) {
        try {
            OTPData otpData = otpStorage.get(phoneNumber);

            if (otpData == null) {
                logger.warn("No OTP found for phone number: {}", phoneNumber);
                return false;
            }

            // Check if OTP is expired
            if (LocalDateTime.now().isAfter(otpData.getExpiryTime())) {
                logger.warn("OTP expired for phone number: {}", phoneNumber);
                otpStorage.remove(phoneNumber); // Clean up expired OTP
                return false;
            }

            // Verify OTP
            boolean isValid = otpData.getOtp().equals(providedOtp);

            if (isValid) {
                logger.info("OTP verified successfully for {}", phoneNumber);
                otpStorage.remove(phoneNumber); // Remove OTP after successful verification
            } else {
                logger.warn("Invalid OTP provided for {}", phoneNumber);
            }

            return isValid;
        } catch (Exception e) {
            logger.error("Error verifying OTP for {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Send transfer notification asynchronously
     */
    @Async
    public CompletableFuture<Void> sendTransferNotification(String phoneNumber, String message, String type) {
        try {
            logger.info("Sending {} notification to {}", type, phoneNumber);

            if (smsEnabled) {
                sendSmsNotification(phoneNumber, message);
            } else {
                // Log notification instead of sending SMS (for testing)
                logger.info("SMS disabled. Notification for {}: {}", phoneNumber, message);
            }

            // Store notification in database for user's notification history
            storeNotificationRecord(phoneNumber, message, type);

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to send notification to {}: {}", phoneNumber, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send SMS notification via Twilio
     */
    private void sendSmsNotification(String phoneNumber, String message) {
        try {
            // Ensure phone number has proper format
            String formattedPhoneNumber = formatPhoneNumber(phoneNumber);

            Message.creator(
                    new PhoneNumber(formattedPhoneNumber),  // To
                    new PhoneNumber(fromPhoneNumber),       // From (Twilio number)
                    message
            ).create();

            logger.info("SMS sent successfully to {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("SMS sending failed", e);
        }
    }

    /**
     * Format phone number for Twilio (ensure it starts with country code)
     */
    private String formatPhoneNumber(String phoneNumber) {
        // Remove any spaces, dashes, or parentheses
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // If it doesn't start with +, assume it's a Cameroon number
        if (!cleaned.startsWith("+")) {
            if (cleaned.startsWith("237")) {
                cleaned = "+" + cleaned;
            } else if (cleaned.startsWith("6")) {
                cleaned = "+237" + cleaned;
            } else {
                cleaned = "+237" + cleaned;
            }
        }

        return cleaned;
    }

    /**
     * Store notification record in database
     */
    private void storeNotificationRecord(String phoneNumber, String message, String type) {
        try {
            // Create notification record
            NotificationRecord record = new NotificationRecord();
            record.setPhoneNumber(phoneNumber);
            record.setMessage(message);
            record.setType(type);
            record.setStatus("SENT");
            record.setCreatedAt(LocalDateTime.now());

            // Save to database (you'll need to create NotificationRepository)
            // notificationRepository.save(record);

            logger.debug("Notification record stored for {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Failed to store notification record: {}", e.getMessage());
            // Don't fail the notification if storage fails
        }
    }

    /**
     * Send deposit notification
     */
    @Async
    public CompletableFuture<Void> sendDepositNotification(String phoneNumber, Double amount, String status) {
        String message;
        if ("COMPLETED".equals(status)) {
            message = String.format("Deposit successful! Amount: %.2f XAF has been added to your wallet. Thank you for using GPay.", amount);
        } else if ("FAILED".equals(status)) {
            message = String.format("Deposit failed! Your deposit of %.2f XAF could not be processed. Please try again or contact support.", amount);
        } else {
            message = String.format("Deposit pending! Your deposit of %.2f XAF is being processed. You will be notified once completed.", amount);
        }
        return sendTransferNotification(phoneNumber, message, "DEPOSIT_" + status);
    }

    /**
     * Send withdrawal notification
     */
    @Async
    public CompletableFuture<Void> sendWithdrawalNotification(String phoneNumber, Double amount, String status) {
        String message;
        if ("COMPLETED".equals(status)) {
            message = String.format("Withdrawal successful! Amount: %.2f XAF has been withdrawn from your wallet.", amount);
        } else if ("FAILED".equals(status)) {
            message = String.format("Withdrawal failed! Your withdrawal of %.2f XAF could not be processed. Please try again or contact support.", amount);
        } else {
            message = String.format("Withdrawal pending! Your withdrawal of %.2f XAF is being processed. You will be notified once completed.", amount);
        }
        return sendTransferNotification(phoneNumber, message, "WITHDRAWAL_" + status);
    }

    /**
     * Send transfer notification (sender)
     */
    @Async
    public CompletableFuture<Void> sendTransferSentNotification(String phoneNumber, Double amount, String recipientPhone) {
        String message = String.format("Transfer sent! You have successfully sent %.2f XAF to %s.", amount, recipientPhone);
        return sendTransferNotification(phoneNumber, message, "TRANSFER_SENT");
    }

    /**
     * Send transfer notification (recipient)
     */
    @Async
    public CompletableFuture<Void> sendTransferReceivedNotification(String phoneNumber, Double amount, String senderPhone) {
        String message = String.format("Money received! You have received %.2f XAF from %s.", amount, senderPhone);
        return sendTransferNotification(phoneNumber, message, "TRANSFER_RECEIVED");
    }

    /**
     * Send balance update notification
     */
    @Async
    public CompletableFuture<Void> sendBalanceUpdateNotification(String phoneNumber, Double newBalance) {
        String message = String.format("Your account balance has been updated. New balance: %.2f XAF", newBalance);
        return sendTransferNotification(phoneNumber, message, "BALANCE_UPDATE");
    }

    /**
     * Send low balance warning
     */
    @Async
    public CompletableFuture<Void> sendLowBalanceWarning(String phoneNumber, Double currentBalance) {
        String message = String.format("Low balance warning! Your current balance is %.2f XAF. Please top up your wallet.", currentBalance);
        return sendTransferNotification(phoneNumber, message, "LOW_BALANCE");
    }

    /**
     * Send transaction failure notification
     */
    @Async
    public CompletableFuture<Void> sendTransactionFailureNotification(String phoneNumber, String reason) {
        String message = String.format("Transaction failed: %s. Please try again or contact support.", reason);
        return sendTransferNotification(phoneNumber, message, "TRANSACTION_FAILED");
    }

    /**
     * Clean up expired OTPs (call this periodically)
     */
    public void cleanupExpiredOTPs() {
        LocalDateTime now = LocalDateTime.now();
        otpStorage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiryTime()));
        logger.debug("Cleaned up expired OTPs");
    }

    /**
     * Inner class for OTP data
     */
    private static class OTPData {
        private final String otp;
        private final LocalDateTime expiryTime;

        public OTPData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }

    /**
     * Inner class for notification record
     */
    public static class NotificationRecord {
        private String id;
        private String phoneNumber;
        private String message;
        private String type;
        private String status;
        private LocalDateTime createdAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
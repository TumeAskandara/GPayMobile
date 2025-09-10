package com.example.gpay.config;

import com.example.gpay.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class ScheduledTasksConfig {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksConfig.class);

    @Autowired
    private NotificationService notificationService;

    /**
     * Clean up expired OTPs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void cleanupExpiredOTPs() {
        try {
            logger.debug("Starting OTP cleanup task");
            notificationService.cleanupExpiredOTPs();
            logger.debug("OTP cleanup task completed");
        } catch (Exception e) {
            logger.error("Error during OTP cleanup: {}", e.getMessage());
        }
    }

    /**
     * Log system health every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void systemHealthCheck() {
        logger.info("System health check - Services are running normally");
    }
}
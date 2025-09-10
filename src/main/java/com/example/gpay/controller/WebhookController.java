package com.example.gpay.controller;

import com.example.gpay.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final TransactionService transactionService;

    @PostMapping("/campay")
    public ResponseEntity<String> handleCampayWebhook(@RequestBody Map<String, Object> payload) {
        try {
            logger.info("Received Campay webhook: {}", payload);

            String reference = (String) payload.get("reference");
            String status = (String) payload.get("status");

//            if (reference != null) {
//                transactionService.updateTransactionStatus(reference).subscribe();
//            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            logger.error("Error processing Campay webhook", e);
            return ResponseEntity.badRequest().body("Error processing webhook");
        }
    }
}

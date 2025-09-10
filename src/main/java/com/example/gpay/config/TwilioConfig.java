package com.example.gpay.config;


import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
public class TwilioConfig {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.api-key}")
    private String apiKey;

    @Value("${twilio.api-secret}")
    private String apiSecret;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(apiKey, apiSecret, accountSid);
    }
}

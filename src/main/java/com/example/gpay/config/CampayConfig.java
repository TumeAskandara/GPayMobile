//package com.example.gpay.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.ExchangeStrategies;
//import java.time.Duration;
//
//@Configuration
//public class CampayConfig {
//
//    @Value("${campay.api-url}")
//    private String apiUrl;
//
//    @Value("${campay.timeout:30000}")
//    private int timeout;
//
//    @Bean
//    public WebClient campayWebClient() {
//        return WebClient.builder()
//                .baseUrl(apiUrl)
//                .exchangeStrategies(ExchangeStrategies.builder()
//                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
//                        .build())
//                .build();
//    }
//}

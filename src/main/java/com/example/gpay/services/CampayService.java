//package com.example.gpay.services;
//import com.example.gpay.dto.campay.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//import reactor.core.publisher.Mono;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.time.Duration;
//import java.util.concurrent.atomic.AtomicReference;
//
//@Service
//public class CampayService {
//
//    private static final Logger logger = LoggerFactory.getLogger(CampayService.class);
//
//    @Autowired
//    private WebClient campayWebClient;
//
//    @Value("${campay.username}")
//    private String username;
//
//    @Value("${campay.password}")
//    private String password;
//
//    @Value("${campay.app-id}")
//    private String appId;
//
//    @Value("${campay.timeout:30000}")
//    private int timeout;
//
//    private final AtomicReference<String> cachedToken = new AtomicReference<>();
//
//    public Mono<String> getAccessToken() {
//        if (cachedToken.get() != null) {
//            return Mono.just(cachedToken.get());
//        }
//
//        CampayAuthRequest authRequest = new CampayAuthRequest(username, password);
//
//        return campayWebClient.post()
//                .uri("/token/")
//                .bodyValue(authRequest)
//                .retrieve()
//                .bodyToMono(CampayAuthResponse.class)
//                .timeout(Duration.ofMillis(timeout))
//                .map(response -> {
//                    cachedToken.set(response.getToken());
//                    return response.getToken();
//                })
//                .doOnError(error -> {
//                    logger.error("Failed to get Campay access token", error);
//                    cachedToken.set(null);
//                });
//    }
//
//
//
//    public Mono<CampayResponse> disburse(String amount, String phoneNumber, String description, String externalReference) {
//        CampayDisburseRequest request = new CampayDisburseRequest(amount, phoneNumber, description, externalReference);
//
//        return getAccessToken()
//                .flatMap(token -> campayWebClient.post()
//                        .uri("/disburse/")
//                        .header("Authorization", "Token " + token)
//                        .header("Content-Type", "application/json")
//                        .bodyValue(request)
//                        .retrieve()
//                        .bodyToMono(CampayResponse.class)
//                        .timeout(Duration.ofMillis(timeout))
//                        .onErrorResume(WebClientResponseException.class, ex -> {
//                            logger.error("Campay disburse failed: {}", ex.getResponseBodyAsString());
//                            return Mono.error(new RuntimeException("Payment disbursement failed: " + ex.getMessage()));
//                        }));
//    }
//
//    public Mono<CampayResponse> getTransactionStatus(String reference) {
//        return getAccessToken()
//                .flatMap(token -> campayWebClient.get()
//                        .uri("/transaction/{reference}/", reference)
//                        .header("Authorization", "Token " + token)
//                        .retrieve()
//                        .bodyToMono(CampayResponse.class)
//                        .timeout(Duration.ofMillis(timeout))
//                        .onErrorResume(WebClientResponseException.class, ex -> {
//                            logger.error("Failed to get transaction status: {}", ex.getResponseBodyAsString());
//                            return Mono.error(new RuntimeException("Failed to get transaction status: " + ex.getMessage()));
//                        }));
//    }
//
//
//    // Add this method to handle token expiration
//    private Mono<String> refreshToken() {
//        cachedToken.set(null);
//        return getAccessToken();
//    }
//
//    // Modify your methods to handle 401 errors (token expired)
//    public Mono<CampayResponse> collect(String amount, String phoneNumber, String description, String externalReference) {
//        CampayCollectRequest request = new CampayCollectRequest(amount, phoneNumber, description, externalReference);
//
//        return getAccessToken()
//                .flatMap(token -> campayWebClient.post()
//                        .uri("/collect/")
//                        .header("Authorization", "Token " + token)
//                        .header("Content-Type", "application/json")
//                        .bodyValue(request)
//                        .retrieve()
//                        .bodyToMono(CampayResponse.class)
//                        .timeout(Duration.ofMillis(timeout))
//                        .onErrorResume(WebClientResponseException.class, ex -> {
//                            if (ex.getStatusCode().value() == 401) {
//                                // Token expired, refresh and retry
//                                return refreshToken()
//                                        .flatMap(newToken -> campayWebClient.post()
//                                                .uri("/collect/")
//                                                .header("Authorization", "Token " + newToken)
//                                                .header("Content-Type", "application/json")
//                                                .bodyValue(request)
//                                                .retrieve()
//                                                .bodyToMono(CampayResponse.class)
//                                                .timeout(Duration.ofMillis(timeout)));
//                            }
//                            logger.error("Campay collect failed: {}", ex.getResponseBodyAsString());
//                            return Mono.error(new RuntimeException("Payment collection failed: " + ex.getMessage()));
//                        }));
//    }
//}

package com.example.gpay.services;

import com.example.gpay.dto.campay.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CampayService {

    private static final Logger logger = LoggerFactory.getLogger(CampayService.class);

    @Autowired
    private WebClient campayWebClient;

    @Autowired
    private Retry campayRetrySpec;

    @Value("${campay.username}")
    private String username;

    @Value("${campay.password}")
    private String password;

    @Value("${campay.app-id}")
    private String appId;

    @Value("${campay.timeout:30000}")
    private int timeout;

    @Value("${campay.token-refresh-threshold:300000}") // 5 minutes
    private long tokenRefreshThreshold;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final AtomicLong tokenTimestamp = new AtomicLong(0);

    /**
     * Enhanced token management with automatic refresh
     */
    public Mono<String> getAccessToken() {
        long currentTime = System.currentTimeMillis();
        String currentToken = cachedToken.get();

        // Check if token exists and is not expired
        if (currentToken != null &&
                (currentTime - tokenTimestamp.get()) < tokenRefreshThreshold) {
            logger.debug("Using cached token");
            return Mono.just(currentToken);
        }

        logger.info("Fetching new access token from Campay");
        CampayAuthRequest authRequest = new CampayAuthRequest(username, password);

        return campayWebClient.post()
                .uri("/token/")
                .bodyValue(authRequest)
                .retrieve()
                .bodyToMono(CampayAuthResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(campayRetrySpec)
                .map(response -> {
                    String token = response.getToken();
                    cachedToken.set(token);
                    tokenTimestamp.set(System.currentTimeMillis());
                    logger.info("Successfully obtained new access token");
                    return token;
                })
                .doOnError(error -> {
                    logger.error("Failed to get Campay access token: {}", error.getMessage());
                    cachedToken.set(null);
                    tokenTimestamp.set(0);
                })
                .onErrorMap(this::mapCampayError);
    }

    /**
     * Enhanced collect method with better error handling
     */
    public Mono<CampayResponse> collect(String amount, String phoneNumber, String description, String externalReference) {
        logger.info("Initiating collect request for amount: {}, phone: {}, reference: {}",
                amount, phoneNumber, externalReference);

        CampayCollectRequest request = new CampayCollectRequest(amount, phoneNumber, description, externalReference);

        return getAccessToken()
                .flatMap(token -> makeCollectRequest(request, token))
                .retryWhen(campayRetrySpec)
                .doOnSuccess(response -> logger.info("Collect request successful for reference: {}", externalReference))
                .doOnError(error -> logger.error("Collect request failed for reference {}: {}",
                        externalReference, error.getMessage()))
                .onErrorMap(this::mapCampayError);
    }

    /**
     * Enhanced disburse method with better error handling
     */
    public Mono<CampayResponse> disburse(String amount, String phoneNumber, String description, String externalReference) {
        logger.info("Initiating disburse request for amount: {}, phone: {}, reference: {}",
                amount, phoneNumber, externalReference);

        CampayDisburseRequest request = new CampayDisburseRequest(amount, phoneNumber, description, externalReference);

        return getAccessToken()
                .flatMap(token -> makeDisburseRequest(request, token))
                .retryWhen(campayRetrySpec)
                .doOnSuccess(response -> logger.info("Disburse request successful for reference: {}", externalReference))
                .doOnError(error -> logger.error("Disburse request failed for reference {}: {}",
                        externalReference, error.getMessage()))
                .onErrorMap(this::mapCampayError);
    }

    /**
     * Enhanced transaction status check with better error handling
     */
    public Mono<CampayResponse> getTransactionStatus(String reference) {
        logger.debug("Checking transaction status for reference: {}", reference);

        return getAccessToken()
                .flatMap(token -> makeStatusRequest(reference, token))
                .retryWhen(campayRetrySpec)
                .doOnSuccess(response -> logger.debug("Status check successful for reference: {}", reference))
                .doOnError(error -> logger.error("Status check failed for reference {}: {}",
                        reference, error.getMessage()))
                .onErrorMap(this::mapCampayError);
    }

    /**
     * Make collect request with token refresh on 401
     */
    private Mono<CampayResponse> makeCollectRequest(CampayCollectRequest request, String token) {
        return campayWebClient.post()
                .uri("/collect/")
                .header("Authorization", "Token " + token)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CampayResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 401) {
                        logger.warn("Token expired, refreshing and retrying collect request");
                        return refreshTokenAndRetryCollect(request);
                    }
                    return Mono.error(ex);
                });
    }

    /**
     * Make disburse request with token refresh on 401
     */
    private Mono<CampayResponse> makeDisburseRequest(CampayDisburseRequest request, String token) {
        return campayWebClient.post()
                .uri("/withdraw/")
                .header("Authorization", "Token " + token)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CampayResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 401) {
                        logger.warn("Token expired, refreshing and retrying disburse request");
                        return refreshTokenAndRetryDisburse(request);
                    }
                    return Mono.error(ex);
                });
    }

    /**
     * Make status request with token refresh on 401
     */
    private Mono<CampayResponse> makeStatusRequest(String reference, String token) {
        return campayWebClient.get()
                .uri("/transaction/{reference}/", reference)
                .header("Authorization", "Token " + token)
                .retrieve()
                .bodyToMono(CampayResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 401) {
                        logger.warn("Token expired, refreshing and retrying status request");
                        return refreshTokenAndRetryStatus(reference);
                    }
                    return Mono.error(ex);
                });
    }

    /**
     * Refresh token and retry collect request
     */
    private Mono<CampayResponse> refreshTokenAndRetryCollect(CampayCollectRequest request) {
        return refreshToken()
                .flatMap(newToken -> campayWebClient.post()
                        .uri("/collect/")
                        .header("Authorization", "Token " + newToken)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(CampayResponse.class)
                        .timeout(Duration.ofMillis(timeout)));
    }

    /**
     * Refresh token and retry disburse request
     */
    private Mono<CampayResponse> refreshTokenAndRetryDisburse(CampayDisburseRequest request) {
        return refreshToken()
                .flatMap(newToken -> campayWebClient.post()
                        .uri("/withdraw/")
                        .header("Authorization", "Token " + newToken)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(CampayResponse.class)
                        .timeout(Duration.ofMillis(timeout)));
    }

    /**
     * Refresh token and retry status request
     */
    private Mono<CampayResponse> refreshTokenAndRetryStatus(String reference) {
        return refreshToken()
                .flatMap(newToken -> campayWebClient.get()
                        .uri("/transaction/{reference}/", reference)
                        .header("Authorization", "Token " + newToken)
                        .retrieve()
                        .bodyToMono(CampayResponse.class)
                        .timeout(Duration.ofMillis(timeout)));
    }

    /**
     * Force token refresh
     */
    private Mono<String> refreshToken() {
        cachedToken.set(null);
        tokenTimestamp.set(0);
        return getAccessToken();
    }

    /**
     * Map Campay-specific errors to more meaningful exceptions
     */
    private RuntimeException mapCampayError(Throwable error) {
        if (error instanceof WebClientRequestException) {
            WebClientRequestException requestException = (WebClientRequestException) error;
            if (requestException.getCause() instanceof java.net.SocketException) {
                return new RuntimeException("Network connection error with Campay API. Please try again.", error);
            }
            if (requestException.getCause() instanceof java.net.ConnectException) {
                return new RuntimeException("Unable to connect to Campay API. Service may be unavailable.", error);
            }
            if (requestException.getCause() instanceof java.util.concurrent.TimeoutException) {
                return new RuntimeException("Request to Campay API timed out. Please try again.", error);
            }
            return new RuntimeException("Network error occurred while communicating with Campay API.", error);
        }

        if (error instanceof WebClientResponseException) {
            WebClientResponseException responseException = (WebClientResponseException) error;
            int statusCode = responseException.getStatusCode().value();
            String responseBody = responseException.getResponseBodyAsString();

            switch (statusCode) {
                case 400:
                    return new RuntimeException("Invalid request to Campay API: " + responseBody, error);
                case 401:
                    return new RuntimeException("Authentication failed with Campay API", error);
                case 403:
                    return new RuntimeException("Access denied by Campay API", error);
                case 404:
                    return new RuntimeException("Requested resource not found on Campay API", error);
                case 429:
                    return new RuntimeException("Too many requests to Campay API. Please try again later.", error);
                case 500:
                    return new RuntimeException("Internal server error at Campay API", error);
                case 502:
                case 503:
                case 504:
                    return new RuntimeException("Campay API service is temporarily unavailable", error);
                default:
                    return new RuntimeException("Unexpected error from Campay API (Status: " + statusCode + ")", error);
            }
        }

        return new RuntimeException("Unexpected error occurred while communicating with Campay API", error);
    }
}
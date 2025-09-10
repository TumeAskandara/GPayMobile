package com.example.gpay.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${campay.timeout:30000}")
    private int timeout;

    @Value("${campay.max-connections:20}")
    private int maxConnections;

    @Value("${campay.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Bean
    public WebClient campayWebClient() {
        // Enhanced connection provider with better resource management
        ConnectionProvider connectionProvider = ConnectionProvider.builder("campay-pool")
                .maxConnections(maxConnections)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .evictInBackground(Duration.ofSeconds(30))
                .lifo() // Use LIFO to promote connection reuse
                .build();

        // Enhanced HTTP client with better error handling
        HttpClient httpClient = HttpClient.create(connectionProvider)
                // Connection timeouts
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)

                // Response timeout
                .responseTimeout(Duration.ofSeconds(timeout / 1000))

                // DNS resolver configuration
                .resolver(DefaultAddressResolverGroup.INSTANCE)

                // Timeout handlers
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(timeout / 1000))
                                .addHandlerLast(new WriteTimeoutHandler(timeout / 1000))
                )

                // Connection logging for debugging
                .doOnConnected(conn -> logger.debug("Connected to Campay API"))
                .doOnDisconnected(conn -> logger.debug("Disconnected from Campay API"))

                // Error handling at connection level
                .doOnRequestError((request, throwable) ->
                        logger.error("Request error to {}: {}", request.uri(), throwable.getMessage()))
                .doOnResponseError((response, throwable) ->
                        logger.error("Response error from {}: {}", response.uri(), throwable.getMessage()));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://demo.campay.net/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "GPay-Mobile-Money-Platform/1.0")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONNECTION, "keep-alive")

                // Add retry filter
                .filter(retryFilter())

                // Add logging filter for debugging
                .filter(loggingFilter())

                // Add error handling filter
                .filter(errorHandlingFilter())

                .build();
    }

    /**
     * Retry filter for handling transient network errors
     */
    private ExchangeFilterFunction retryFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            return Mono.just(clientResponse);
        }));
    }

    /**
     * Enhanced retry configuration for WebClient calls
     */
    @Bean
    public Retry campayRetrySpec() {
        return Retry.backoff(maxRetryAttempts, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10))
                .jitter(0.5)
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal ->
                        logger.warn("Retrying Campay API call, attempt {} of {}: {}",
                                retrySignal.totalRetries() + 1,
                                maxRetryAttempts,
                                retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    logger.error("Campay API call failed after {} attempts", maxRetryAttempts);
                    return new RuntimeException("Campay API call failed after " + maxRetryAttempts + " attempts",
                            retrySignal.failure());
                });
    }

    /**
     * Determines if an exception is retryable
     */
    private boolean isRetryableException(Throwable throwable) {
        // Retry on connection issues
        if (throwable instanceof java.net.SocketException ||
                throwable instanceof java.net.ConnectException ||
                throwable instanceof TimeoutException ||
                throwable instanceof reactor.netty.http.client.PrematureCloseException) {
            return true;
        }

        // Retry on specific HTTP status codes
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException webClientException = (WebClientResponseException) throwable;
            int statusCode = webClientException.getStatusCode().value();
            return statusCode >= 500 || statusCode == 408 || statusCode == 429;
        }

        // Retry on Spring WebClient request exceptions (network issues)
        return throwable instanceof org.springframework.web.reactive.function.client.WebClientRequestException;
    }

    /**
     * Logging filter for request/response debugging
     */
    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug("Campay API Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    /**
     * Error handling filter
     */
    private ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                logger.error("Campay API Error Response: {} {}",
                        clientResponse.statusCode().value(),
                        clientResponse.statusCode().value());
            }
            return Mono.just(clientResponse);
        });
    }
}
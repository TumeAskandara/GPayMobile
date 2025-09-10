package com.example.gpay.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only log for transaction endpoints
        if (request.getRequestURI().contains("/api/transactions/")) {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

            try {
                filterChain.doFilter(wrappedRequest, wrappedResponse);
            } finally {
                logRequestDetails(wrappedRequest, wrappedResponse);
                wrappedResponse.copyBodyToResponse();
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void logRequestDetails(ContentCachingRequestWrapper request,
                                   ContentCachingResponseWrapper response) {
        try {
            String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

            logger.info("=== REQUEST DETAILS ===");
            logger.info("URI: {}", request.getRequestURI());
            logger.info("Method: {}", request.getMethod());
            logger.info("Content-Type: {}", request.getContentType());
            logger.info("Raw Request Body: {}", requestBody);

            // Log any control characters found
            if (requestBody.matches(".*\\p{Cntrl}.*")) {
                logger.warn("⚠️  CONTROL CHARACTERS DETECTED IN REQUEST BODY!");
                logger.warn("Request body contains control characters: {}",
                        requestBody.replaceAll("\\p{Cntrl}", "[CTRL]"));
            }

            logger.info("Response Status: {}", response.getStatus());
            logger.info("========================");

        } catch (Exception e) {
            logger.error("Error logging request details", e);
        }
    }
}
//package com.example.gpay.config;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.http.HttpServletRequestWrapper;
//import jakarta.servlet.ServletInputStream;
//import jakarta.servlet.ReadListener;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//@Component
//@Order(1) // Execute this filter first
//public class JsonSanitizingFilter extends OncePerRequestFilter {
//
//    private static final Logger logger = LoggerFactory.getLogger(JsonSanitizingFilter.class);
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        // Only process JSON requests to transaction endpoints
//        if (isJsonRequest(request) && request.getRequestURI().contains("/api/transactions/")) {
//            logger.debug("Sanitizing JSON request to: {}", request.getRequestURI());
//
//            // Read the original request body
//            String originalBody = readRequestBody(request);
//
//            if (originalBody != null && !originalBody.isEmpty()) {
//                // Check if it contains control characters
//                if (originalBody.matches(".*\\p{Cntrl}.*")) {
//                    logger.warn("Control characters detected in request to: {}", request.getRequestURI());
//
//                    // Sanitize the JSON by removing control characters
//                    String sanitizedBody = sanitizeJson(originalBody);
//
//                    logger.info("Original body: {}", originalBody);
//                    logger.info("Sanitized body: {}", sanitizedBody);
//
//                    // Create a new request with sanitized body
//                    SanitizedRequestWrapper wrappedRequest = new SanitizedRequestWrapper(request, sanitizedBody);
//                    filterChain.doFilter(wrappedRequest, response);
//                    return;
//                }
//            }
//        }
//
//        // Continue with original request if no sanitization needed
//        filterChain.doFilter(request, response);
//    }
//
//    private boolean isJsonRequest(HttpServletRequest request) {
//        String contentType = request.getContentType();
//        return contentType != null && contentType.toLowerCase().contains("application/json");
//    }
//
//    private String readRequestBody(HttpServletRequest request) throws IOException {
//        try (ServletInputStream inputStream = request.getInputStream()) {
//            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//        }
//    }
//
//    private String sanitizeJson(String json) {
//        if (json == null) {
//            return null;
//        }
//
//        // Replace control characters with escaped versions or remove them
//        return json.replaceAll("\\p{Cntrl}", " ")  // Replace control chars with space
//                .replaceAll("\\s+", " ")          // Replace multiple spaces with single space
//                .trim();
//    }
//
//    // Custom request wrapper to provide sanitized content
//    private static class SanitizedRequestWrapper extends HttpServletRequestWrapper {
//        private final String sanitizedBody;
//        private final byte[] sanitizedBytes;
//
//        public SanitizedRequestWrapper(HttpServletRequest request, String sanitizedBody) {
//            super(request);
//            this.sanitizedBody = sanitizedBody;
//            this.sanitizedBytes = sanitizedBody.getBytes(StandardCharsets.UTF_8);
//        }
//
//        @Override
//        public ServletInputStream getInputStream() throws IOException {
//            return new ServletInputStream() {
//                private final ByteArrayInputStream byteArrayInputStream =
//                        new ByteArrayInputStream(sanitizedBytes);
//
//                @Override
//                public boolean isFinished() {
//                    return byteArrayInputStream.available() == 0;
//                }
//
//                @Override
//                public boolean isReady() {
//                    return true;
//                }
//
//                @Override
//                public void setReadListener(ReadListener readListener) {
//                    // Not implemented for this use case
//                }
//
//                @Override
//                public int read() throws IOException {
//                    return byteArrayInputStream.read();
//                }
//            };
//        }
//
//        @Override
//        public int getContentLength() {
//            return sanitizedBytes.length;
//        }
//
//        @Override
//        public long getContentLengthLong() {
//            return sanitizedBytes.length;
//        }
//    }
//}
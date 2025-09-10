package com.example.gpay.config;

import com.example.gpay.dto.ApiResponse;
import com.fasterxml.jackson.core.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        logger.error("JSON parsing error on request to: {}", request.getDescription(false));
        logger.error("Error details: {}", ex.getMessage());

        String errorMessage = "Invalid JSON format in request";

        // Check if it's specifically a control character error
        if (ex.getMessage() != null && ex.getMessage().contains("CTRL-CHAR")) {
            errorMessage = "Request contains invalid control characters. Please ensure all text fields are properly formatted.";
            logger.error("Control character detected in JSON request");
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorMessage));
    }

    @ExceptionHandler(JsonParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Object>> handleJsonParseException(
            JsonParseException ex, WebRequest request) {

        logger.error("JSON parse exception on request to: {}", request.getDescription(false));
        logger.error("Parse error: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid JSON format in request"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(
            Exception ex, WebRequest request) {

        logger.error("Unexpected error on request to: {}", request.getDescription(false));
        logger.error("Error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }
}
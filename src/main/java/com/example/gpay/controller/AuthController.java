//package com.example.gpay.controller;
//
//import com.example.gpay.dto.AuthRequest;
//import com.example.gpay.dto.AuthResponse;
//import com.example.gpay.dto.RegisterRequest;
//import com.example.gpay.security.AuthService;
//import com.example.gpay.utils.PhoneNumberUtils;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.validation.BindingResult;
//import jakarta.validation.Valid;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/auth")
//@CrossOrigin(origins = "*", maxAge = 3600)
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//    private final PhoneNumberUtils phoneNumberUtils;
//
//    @PostMapping("/register")
//    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult result) {
//        // Check for validation errors
//        if (result.hasErrors()) {
//            Map<String, String> errors = new HashMap<>();
//            result.getFieldErrors().forEach(error ->
//                    errors.put(error.getField(), error.getDefaultMessage())
//            );
//            return ResponseEntity.badRequest().body(errors);
//        }
//
//        try {
//            AuthResponse response = authService.register(request);
//            return ResponseEntity.ok(response);
//        } catch (RuntimeException e) {
//            Map<String, String> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            return ResponseEntity.badRequest().body(error);
//        } catch (Exception e) {
//            Map<String, String> error = new HashMap<>();
//            error.put("error", "Registration failed: " + e.getMessage());
//            return ResponseEntity.badRequest().body(error);
//        }
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, BindingResult result) {
//        // Check for validation errors
//        if (result.hasErrors()) {
//            Map<String, String> errors = new HashMap<>();
//            result.getFieldErrors().forEach(error ->
//                    errors.put(error.getField(), error.getDefaultMessage())
//            );
//            return ResponseEntity.badRequest().body(errors);
//        }
//
//        // Validate phone number format (accept both local and international formats)
//        String normalizedPhone = phoneNumberUtils.getPhoneNumberForLookup(request.getPhoneNumber());
//        if (!phoneNumberUtils.isValidCameroonNumber(normalizedPhone)) {
//            Map<String, String> error = new HashMap<>();
//            error.put("phoneNumber", "Invalid phone number format. Use format: 123456789 or +237123456789");
//            return ResponseEntity.badRequest().body(error);
//        }
//
//        try {
//            AuthResponse response = authService.authenticate(request);
//            return ResponseEntity.ok(response);
//        } catch (RuntimeException e) {
//            Map<String, String> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            return ResponseEntity.badRequest().body(error);
//        } catch (Exception e) {
//            Map<String, String> error = new HashMap<>();
//            error.put("error", "Login failed: " + e.getMessage());
//            return ResponseEntity.badRequest().body(error);
//        }
//    }
//}






package com.example.gpay.controller;

import com.example.gpay.dto.*;
import com.example.gpay.security.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    /**
     * Traditional registration
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            logger.info("User registered successfully: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("Registration failed"));
        }
    }

    /**
     * Traditional login with password only
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request);
            logger.info("User logged in successfully: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("Login failed"));
        }
    }

    /**
     * Step 1: Request OTP for login
     */
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestLoginOTP(@Valid @RequestBody OTPRequest request) {
        try {
            String message = authService.requestLoginOTP(request);
            logger.info("OTP requested for: {}", request.getPhoneNumber());
            return ResponseEntity.ok(OTPResponse.success(message, 5L));
        } catch (RuntimeException e) {
            logger.error("OTP request failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(OTPResponse.failure(e.getMessage()));
        } catch (Exception e) {
            logger.error("OTP request error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(OTPResponse.failure("Failed to send OTP"));
        }
    }

    /**
     * Step 2: Verify OTP and complete login
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyLoginOTP(@Valid @RequestBody OTPVerificationRequest request) {
        try {
            AuthResponse response = authService.verifyLoginOTP(request);
            logger.info("User logged in successfully with OTP: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("OTP verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("OTP verification error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("OTP verification failed"));
        }
    }

    /**
     * Combined login with password and OTP (alternative approach)
     */
    @PostMapping("/login-with-otp")
    public ResponseEntity<?> authenticateWithOTP(@Valid @RequestBody AuthWithOTPRequest request) {
        try {
            AuthResponse response = authService.authenticateWithOTP(
                    request.getPhoneNumber(),
                    request.getPassword(),
                    request.getOtp()
            );
            logger.info("User authenticated with password and OTP: {}", request.getPhoneNumber());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Combined authentication failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Combined authentication error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("Authentication failed"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body(new SuccessResponse("Auth service is running"));
    }
}





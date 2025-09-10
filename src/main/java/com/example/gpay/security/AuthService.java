//package com.example.gpay.security;
//
//import com.example.gpay.dto.AuthRequest;
//import com.example.gpay.dto.AuthResponse;
//import com.example.gpay.dto.RegisterRequest;
//import com.example.gpay.model.User;
//import com.example.gpay.services.UserService;
//import com.example.gpay.utils.PhoneNumberUtils;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//    private final UserService userService;
//    private final JwtService jwtService;
//    private final AuthenticationManager authenticationManager;
//    private final UserDetailsService userDetailsService;
//    private final PhoneNumberUtils phoneNumberUtils;
//
//    public AuthResponse register(RegisterRequest request) {
//        try {
//            User user = userService.registerUser(request);
//
//            // Load user details for token generation
//            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
//            String token = jwtService.generateToken(userDetails);
//
//            return new AuthResponse(token, user);
//        } catch (RuntimeException e) {
//            // Re-throw runtime exceptions (like duplicate phone/email)
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException("Registration failed: " + e.getMessage());
//        }
//    }
//
//    public AuthResponse authenticate(AuthRequest request) {
//        try {
//            // Normalize phone number for authentication
//            String normalizedPhoneNumber = phoneNumberUtils.getPhoneNumberForLookup(request.getPhoneNumber());
//
//            // Authenticate user with normalized phone number
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(
//                            normalizedPhoneNumber,
//                            request.getPassword()
//                    )
//            );
//
//            // Find user with normalized phone number
//            User user = userService.findByPhoneNumber(request.getPhoneNumber())
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            // Generate token
//            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
//            String token = jwtService.generateToken(userDetails);
//
//            return new AuthResponse(token, user);
//        } catch (BadCredentialsException e) {
//            throw new RuntimeException("Invalid phone number or password");
//        } catch (AuthenticationException e) {
//            throw new RuntimeException("Authentication failed: " + e.getMessage());
//        } catch (Exception e) {
//            throw new RuntimeException("Login failed: " + e.getMessage());
//        }
//    }
//}


package com.example.gpay.security;

import com.example.gpay.dto.AuthRequest;
import com.example.gpay.dto.AuthResponse;
import com.example.gpay.dto.RegisterRequest;
import com.example.gpay.dto.OTPRequest;
import com.example.gpay.dto.OTPVerificationRequest;
import com.example.gpay.model.User;
import com.example.gpay.services.UserService;
import com.example.gpay.services.NotificationService;
import com.example.gpay.utils.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final PhoneNumberUtils phoneNumberUtils;
    private final NotificationService notificationService;

    public AuthResponse register(RegisterRequest request) {
        try {
            User user = userService.registerUser(request);

            // Load user details for token generation
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
            String token = jwtService.generateToken(userDetails);

            return new AuthResponse(token, user);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions (like duplicate phone/email)
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Traditional login with password only
     */
    public AuthResponse authenticate(AuthRequest request) {
        try {
            // Normalize phone number for authentication
            String normalizedPhoneNumber = phoneNumberUtils.getPhoneNumberForLookup(request.getPhoneNumber());

            // Authenticate user with normalized phone number
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedPhoneNumber,
                            request.getPassword()
                    )
            );

            // Find user with normalized phone number
            User user = userService.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
            String token = jwtService.generateToken(userDetails);

            return new AuthResponse(token, user);
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid phone number or password");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    /**
     * Step 1: Request OTP for login
     */
    public String requestLoginOTP(OTPRequest request) {
        try {
            // Normalize phone number
            String normalizedPhoneNumber = phoneNumberUtils.getPhoneNumberForLookup(request.getPhoneNumber());

            // Check if user exists
            User user = userService.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + request.getPhoneNumber()));

            // Validate password first (optional - you can remove this if you want OTP-only login)
            if (request.getPassword() != null && !userService.validatePassword(user, request.getPassword())) {
                throw new RuntimeException("Invalid password");
            }

            // Generate and send OTP
            String otp = notificationService.generateAndSendOTP(user.getPhoneNumber());

            logger.info("OTP requested successfully for user: {}", user.getPhoneNumber());

            // Return success message (don't return actual OTP in production)
            return "OTP sent successfully to " + user.getPhoneNumber();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error requesting OTP: {}", e.getMessage());
            throw new RuntimeException("Failed to request OTP: " + e.getMessage());
        }
    }

    /**
     * Step 2: Verify OTP and complete login
     */
    public AuthResponse verifyLoginOTP(OTPVerificationRequest request) {
        try {
            // Normalize phone number
            String normalizedPhoneNumber = phoneNumberUtils.getPhoneNumberForLookup(request.getPhoneNumber());

            // Find user
            User user = userService.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new RuntimeException("User not found with phone: " + request.getPhoneNumber()));

            // Verify OTP
            boolean isOTPValid = notificationService.verifyOTP(user.getPhoneNumber(), request.getOtp());

            if (!isOTPValid) {
                throw new RuntimeException("Invalid or expired OTP");
            }

            // Generate JWT token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
            String token = jwtService.generateToken(userDetails);

            logger.info("User {} logged in successfully with OTP", user.getPhoneNumber());

            return new AuthResponse(token, user);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error verifying OTP: {}", e.getMessage());
            throw new RuntimeException("Failed to verify OTP: " + e.getMessage());
        }
    }

    /**
     * Combined login with password and OTP verification
     */
    public AuthResponse authenticateWithOTP(String phoneNumber, String password, String otp) {
        try {
            // Normalize phone number for authentication
            String normalizedPhoneNumber = phoneNumberUtils.getPhoneNumberForLookup(phoneNumber);

            // Find user
            User user = userService.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate password
            if (!userService.validatePassword(user, password)) {
                throw new RuntimeException("Invalid password");
            }

            // Verify OTP
            boolean isOTPValid = notificationService.verifyOTP(user.getPhoneNumber(), otp);

            if (!isOTPValid) {
                throw new RuntimeException("Invalid or expired OTP");
            }

            // Generate token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());
            String token = jwtService.generateToken(userDetails);

            logger.info("User {} authenticated successfully with password and OTP", user.getPhoneNumber());

            return new AuthResponse(token, user);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error in combined authentication: {}", e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
}
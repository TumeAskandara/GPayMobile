package com.example.gpay.services;

import com.example.gpay.dto.RegisterRequest;
import com.example.gpay.model.User;
import com.example.gpay.repository.UserRepository;
import com.example.gpay.utils.PhoneNumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PhoneNumberUtils phoneNumberUtils;

    public User registerUser(RegisterRequest request) {
        // Normalize phone number to include country code
        String normalizedPhoneNumber = phoneNumberUtils.normalizePhoneNumber(request.getPhoneNumber());

        // Validate the phone number
        if (!phoneNumberUtils.isValidCameroonNumber(normalizedPhoneNumber)) {
            throw new RuntimeException("Invalid Cameroon phone number format");
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
            throw new RuntimeException("Phone number already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        try {
            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(normalizedPhoneNumber); // Store normalized phone number
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setPin(passwordEncoder.encode(request.getPin()));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            // Add default role
            user.getRoles().add("USER");

            return userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        // Normalize the phone number for lookup
        String normalizedPhoneNumber = phoneNumberUtils.getPhoneNumberForLookup(phoneNumber);
        return userRepository.findByPhoneNumber(normalizedPhoneNumber);
    }


    public boolean validatePin(User user, String pin) {
        return passwordEncoder.matches(pin, user.getPin());
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    // Additional method for updating balance by userId directly
    public User updateBalance(String userId, double newBalance) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }


    // Method to get user by ID (non-optional version for internal use)
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    // Method to authenticate user with phone number and password
    public Optional<User> authenticateUser(String phoneNumber, String password) {
        Optional<User> userOpt = findByPhoneNumber(phoneNumber);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // Method to validate password
    public boolean validatePassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPassword());
    }

    // Method to update user PIN
    public void updatePin(String userId, String newPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPin(passwordEncoder.encode(newPin));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }



    // Method to check if user exists by phone number
    public boolean existsByPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = phoneNumberUtils.getPhoneNumberForLookup(phoneNumber);
        return userRepository.existsByPhoneNumber(normalizedPhoneNumber);
    }



    // Method to check if user exists by email
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
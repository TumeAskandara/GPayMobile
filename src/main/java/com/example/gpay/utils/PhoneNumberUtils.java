package com.example.gpay.utils;

import org.springframework.stereotype.Component;

@Component
public class PhoneNumberUtils {

    private static final String CAMEROON_COUNTRY_CODE = "237";
    private static final String CAMEROON_COUNTRY_CODE_WITH_PLUS = "+237";

    /**
     * Normalizes a phone number to include the country code
     * @param phoneNumber The input phone number
     * @return Normalized phone number with country code
     */
    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }

        // Remove any whitespace
        phoneNumber = phoneNumber.trim();

        // If it already starts with +237, return as is
        if (phoneNumber.startsWith(CAMEROON_COUNTRY_CODE_WITH_PLUS)) {
            return phoneNumber;
        }

        // If it starts with 237, add the + prefix
        if (phoneNumber.startsWith(CAMEROON_COUNTRY_CODE)) {
            return "+" + phoneNumber;
        }

        // If it's a local number (starts with 2 or 6), add the country code
        if (phoneNumber.matches("^[26][0-9]{8}$")) {
            return CAMEROON_COUNTRY_CODE_WITH_PLUS + phoneNumber;
        }

        // Return as is if doesn't match expected patterns
        return phoneNumber;
    }

    /**
     * Extracts the local phone number without country code
     * @param phoneNumber The full phone number
     * @return Local phone number without country code
     */
    public String extractLocalNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }

        phoneNumber = phoneNumber.trim();

        // Remove +237 prefix
        if (phoneNumber.startsWith(CAMEROON_COUNTRY_CODE_WITH_PLUS)) {
            return phoneNumber.substring(4);
        }

        // Remove 237 prefix
        if (phoneNumber.startsWith(CAMEROON_COUNTRY_CODE)) {
            return phoneNumber.substring(3);
        }

        // Return as is if no country code found
        return phoneNumber;
    }

    /**
     * Validates if a phone number is a valid Cameroon number
     * @param phoneNumber The phone number to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCameroonNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        String normalized = normalizePhoneNumber(phoneNumber);
        return normalized.matches("^\\+237[26][0-9]{8}$");
    }

    /**
     * Finds a phone number in the database by trying different formats
     * @param inputNumber The phone number input by user
     * @return The normalized phone number that should be used for database lookup
     */
    public String getPhoneNumberForLookup(String inputNumber) {
        if (inputNumber == null || inputNumber.trim().isEmpty()) {
            return inputNumber;
        }

        // Always normalize to the full format for database lookup
        return normalizePhoneNumber(inputNumber);
    }
}
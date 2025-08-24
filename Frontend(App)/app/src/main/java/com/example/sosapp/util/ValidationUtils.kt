package com.example.sosapp.util

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        return cleanPhone.length >= 10 && cleanPhone.all { it.isDigit() || it == '+' }
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    fun isValidOTP(otp: String): Boolean {
        return otp.length == Constants.OTP_LENGTH && otp.all { it.isDigit() }
    }

    fun formatPhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "")
    }

    fun validateEmergencyContacts(contacts: List<String>): ValidationResult {
        if (contacts.isEmpty()) {
            return ValidationResult(false, "At least one emergency contact is required")
        }

        if (contacts.size > Constants.MAX_EMERGENCY_CONTACTS) {
            return ValidationResult(false, "Maximum ${Constants.MAX_EMERGENCY_CONTACTS} emergency contacts allowed")
        }

        contacts.forEach { contact ->
            if (!isValidPhone(contact)) {
                return ValidationResult(false, "Invalid phone number: $contact")
            }
        }

        return ValidationResult(true)
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)
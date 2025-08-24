package com.example.sosapp.domain.usecase

import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.model.UserData
import com.example.sosapp.domain.model.AppError
import com.example.sosapp.domain.repository.AuthRepository
import com.example.sosapp.util.ValidationUtils
import javax.inject.Inject

class AuthUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun login(email: String, password: String): AuthResult<UserData> {
        if (!ValidationUtils.isValidEmail(email)) {
            return AuthResult.Error(AppError.ValidationError("Invalid email format"))
        }
        if (password.length < 6) {
            return AuthResult.Error(AppError.ValidationError("Password must be at least 6 characters"))
        }
        return authRepository.login(email, password)
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        emergencyContacts: List<String>
    ): AuthResult<Unit> {
        if (fullName.isBlank()) {
            return AuthResult.Error(AppError.ValidationError("Full name is required"))
        }
        if (!ValidationUtils.isValidEmail(email)) {
            return AuthResult.Error(AppError.ValidationError("Invalid email format"))
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            return AuthResult.Error(AppError.ValidationError("Invalid phone number"))
        }
        if (password.length < 6) {
            return AuthResult.Error(AppError.ValidationError("Password must be at least 6 characters"))
        }
        if (emergencyContacts.isEmpty()) {
            return AuthResult.Error(AppError.ValidationError("At least one emergency contact is required"))
        }

        val validContacts = emergencyContacts.filter { ValidationUtils.isValidPhone(it) }
        if (validContacts.size != emergencyContacts.size) {
            return AuthResult.Error(AppError.ValidationError("All emergency contacts must be valid phone numbers"))
        }

        return authRepository.register(fullName, email, password, phone, validContacts)
    }

    suspend fun verifyOTP(email: String, otp: String): AuthResult<UserData> {
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            return AuthResult.Error(AppError.ValidationError("OTP must be 6 digits"))
        }
        return authRepository.verifyOTP(email, otp)
    }

    suspend fun updateEmergencyContacts(contacts: List<String>): AuthResult<UserData> {
        if (contacts.isEmpty()) {
            return AuthResult.Error(AppError.ValidationError("At least one emergency contact is required"))
        }
        if (contacts.size > 3) {
            return AuthResult.Error(AppError.ValidationError("Maximum 3 emergency contacts allowed"))
        }

        val validContacts = contacts.filter { ValidationUtils.isValidPhone(it) }
        if (validContacts.size != contacts.size) {
            return AuthResult.Error(AppError.ValidationError("All emergency contacts must be valid phone numbers"))
        }

        return authRepository.updateEmergencyContacts(validContacts)
    }
}
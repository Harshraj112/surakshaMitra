package com.example.sosapp.domain.repository

import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.model.UserData

interface AuthRepository {
    suspend fun getToken(): String?
    suspend fun login(email: String, password: String): AuthResult<UserData>
    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        emergencyContacts: List<String>
    ): AuthResult<Unit>
    suspend fun verifyOTP(email: String, otp: String): AuthResult<UserData>
    suspend fun resendOTP(email: String): AuthResult<Unit>
    suspend fun getCurrentUser(): UserData?
    suspend fun refreshUserData(): AuthResult<UserData>
    suspend fun updateEmergencyContacts(contacts: List<String>): AuthResult<UserData>
    suspend fun logout(): AuthResult<Unit>
}
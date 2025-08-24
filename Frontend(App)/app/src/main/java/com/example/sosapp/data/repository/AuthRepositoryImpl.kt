package com.example.sosapp.data.repository

import com.example.sosapp.data.local.cache.UserCache
import com.example.sosapp.data.local.preferences.TokenManager
import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.model.UserData
import com.example.sosapp.data.remote.api.AuthApiService
import com.example.sosapp.data.remote.dto.local.LoginRequest
import com.example.sosapp.data.remote.dto.local.OtpRequest
import com.example.sosapp.data.remote.dto.local.RegisterRequest
import com.example.sosapp.data.remote.dto.local.UpdateEmergencyContactsRequest
import com.example.sosapp.domain.model.AppError
import com.example.sosapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager,
    private val userCache: UserCache
) : AuthRepository {

    companion object {
        private const val API_TIMEOUT_MS = 30000L // 30 seconds
    }

    override suspend fun getToken(): String? {
        return tokenManager.token.first()
    }

    private suspend fun getUserId(): String? {
        return tokenManager.userId.first()
    }

    private suspend fun getAuthHeader(): String {
        val token = getToken()
        return token?.let { "Bearer $it" } ?: throw Exception("Missing authentication token")
    }

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): AuthResult<T> {
        return try {
            // Add timeout wrapper
            val response = withTimeoutOrNull(API_TIMEOUT_MS) {
                apiCall()
            }

            if (response == null) {
                return AuthResult.Error(AppError.NetworkError("Request timed out. Please check your internet connection and try again."))
            }

            if (response.isSuccessful && response.body() != null) {
                AuthResult.Success(response.body()!!)
            } else {
                val errorMessage = try {
                    when (response.code()) {
                        400 -> "Invalid request. Please check your input."
                        401 -> "Invalid credentials. Please check your email and password."
                        403 -> "Access denied. Your account may be suspended."
                        404 -> "Service not found. Please try again later."
                        409 -> "Account already exists with this email."
                        422 -> "Invalid data format. Please check your input."
                        500 -> "Server error. Please try again later."
                        502, 503 -> "Service temporarily unavailable. Please try again later."
                        else -> response.errorBody()?.string() ?: "Unknown error occurred (${response.code()})"
                    }
                } catch (e: Exception) {
                    "Network error occurred. Please try again."
                }
                AuthResult.Error(AppError.ApiError(response.code(), errorMessage))
            }
        } catch (e: SocketTimeoutException) {
            AuthResult.Error(AppError.NetworkError("Connection timed out. Please check your internet connection and try again."))
        } catch (e: UnknownHostException) {
            AuthResult.Error(AppError.NetworkError("No internet connection. Please check your network settings."))
        } catch (e: IOException) {
            AuthResult.Error(AppError.NetworkError("Network error occurred. Please check your internet connection."))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Request timed out. Please check your internet connection and try again."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error occurred. Please check your internet connection."
                e.message?.contains("connection", ignoreCase = true) == true ->
                    "Connection failed. Please check your internet connection."
                else -> e.message ?: "An unexpected error occurred. Please try again."
            }
            AuthResult.Error(AppError.NetworkError(errorMessage))
        }
    }

    override suspend fun login(email: String, password: String): AuthResult<UserData> {
        // Validate input
        if (email.isBlank() || password.isBlank()) {
            return AuthResult.Error(AppError.ValidationError("Email and password are required"))
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult.Error(AppError.ValidationError("Please enter a valid email address"))
        }

        return when (val result = safeApiCall {
            api.login(LoginRequest(email.trim(), password))
        }) {
            is AuthResult.Success -> {
                try {
                    val loginResponse = result.data

                    // Validate response data
                    if (loginResponse.token.isBlank() || loginResponse.userId.isBlank()) {
                        return AuthResult.Error(AppError.ApiError(500, "Invalid response from server. Please try again."))
                    }

                    // Save both token and userId from the response
                    tokenManager.saveAuthData(loginResponse.token, loginResponse.userId)

                    val userData = UserData(
                        id = loginResponse.userId,
                        fullName = loginResponse.user.fullname,
                        email = loginResponse.user.email,
                        phone = loginResponse.user.phone,
                        emergencyContacts = loginResponse.user.emergencyContacts,
                        createdAt = if (loginResponse.user.createdAt) "true" else "false"
                    )

                    userCache.saveUser(userData)
                    AuthResult.Success(userData)
                } catch (e: Exception) {
                    AuthResult.Error(AppError.UnknownError("Failed to process login response. Please try again."))
                }
            }
            is AuthResult.Error -> result
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        emergencyContacts: List<String>
    ): AuthResult<Unit> {
        // Validate input
        if (fullName.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            return AuthResult.Error(AppError.ValidationError("All fields are required"))
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult.Error(AppError.ValidationError("Please enter a valid email address"))
        }

        if (password.length < 6) {
            return AuthResult.Error(AppError.ValidationError("Password must be at least 6 characters"))
        }

        if (emergencyContacts.isEmpty()) {
            return AuthResult.Error(AppError.ValidationError("At least one emergency contact is required"))
        }

        val request = RegisterRequest(
            fullname = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            password = password,
            emergencyContacts = emergencyContacts.map { it.trim() }.filter { it.isNotEmpty() }
        )

        return when (val result = safeApiCall { api.sendOtp(request) }) {
            is AuthResult.Success -> AuthResult.Success(Unit)
            is AuthResult.Error -> result
        }
    }

    override suspend fun verifyOTP(email: String, otp: String): AuthResult<UserData> {
        // Validate input
        if (email.isBlank() || otp.isBlank()) {
            return AuthResult.Error(AppError.ValidationError("Email and OTP are required"))
        }

        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            return AuthResult.Error(AppError.ValidationError("OTP must be 6 digits"))
        }

        return when (val result = safeApiCall {
            api.verifyOtp(OtpRequest(email.trim(), otp))
        }) {
            is AuthResult.Success -> {
                try {
                    val otpResponse = result.data

                    // Validate response data
                    if (otpResponse.token.isBlank() || otpResponse.userId.isBlank()) {
                        return AuthResult.Error(AppError.ApiError(500, "Invalid response from server. Please try again."))
                    }

                    // Save both token and userId from OTP verification
                    tokenManager.saveAuthData(otpResponse.token, otpResponse.userId)

                    // Verify that token and userId are saved correctly
                    val savedToken = tokenManager.token.first()
                    val savedUserId = tokenManager.userId.first()
                    if (savedToken.isNullOrBlank() || savedUserId.isNullOrBlank()) {
                        return AuthResult.Error(AppError.UnknownError("Failed to save authentication data."))
                    }

                    // Now fetch the user profile using the userId
                    refreshUserData()
                } catch (e: Exception) {
                    AuthResult.Error(AppError.UnknownError("Failed to process OTP verification. Please try again."))
                }
            }
            is AuthResult.Error -> result
        }
    }

    override suspend fun resendOTP(email: String): AuthResult<Unit> {
        // Validate input
        if (email.isBlank()) {
            return AuthResult.Error(AppError.ValidationError("Email is required"))
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult.Error(AppError.ValidationError("Please enter a valid email address"))
        }

        return when (val result = safeApiCall {
            api.resendOtp(OtpRequest(email.trim(), ""))
        }) {
            is AuthResult.Success -> AuthResult.Success(Unit)
            is AuthResult.Error -> result
        }
    }

    override suspend fun getCurrentUser(): UserData? {
        return userCache.getUser()
    }

    override suspend fun refreshUserData(): AuthResult<UserData> {
        val token = try {
            getAuthHeader()
        } catch (e: Exception) {
            userCache.clearUser()
            return AuthResult.Error(AppError.AuthenticationError("No authentication token"))
        }

        val userId = try {
            getUserId() ?: throw Exception("No user ID found")
        } catch (e: Exception) {
            userCache.clearUser()
            return AuthResult.Error(AppError.AuthenticationError("No user ID found"))
        }

        return when (val result = safeApiCall { api.getCurrentUser(token, userId) }) {
            is AuthResult.Success -> {
                try {
                    val userResponse = result.data
                    val userData = UserData(
                        fullName = userResponse.user.fullname,
                        email = userResponse.user.email,
                        phone = userResponse.user.phone,
                        emergencyContacts = userResponse.user.emergencyContacts,
                        isVerified = userResponse.user.isVerified,
                        createdAt = userResponse.user.createdAt
                    )

                    userCache.saveUser(userData)
                    AuthResult.Success(userData)
                } catch (e: Exception) {
                    AuthResult.Error(AppError.UnknownError("Failed to process user data. Please try again."))
                }
            }
            is AuthResult.Error -> {
                if (result.error is AppError.ApiError && result.error.code == 401) {
                    tokenManager.clearAll()
                    userCache.clearUser()
                }
                result
            }
        }
    }

    override suspend fun updateEmergencyContacts(contacts: List<String>): AuthResult<UserData> {
        // Validate authentication
        val token: String
        try {
            token = getAuthHeader()
        } catch (e: Exception) {
            return AuthResult.Error(AppError.AuthenticationError("No authentication token"))
        }

        try {
            getUserId() ?: return AuthResult.Error(AppError.AuthenticationError("No user ID found"))
        } catch (e: Exception) {
            return AuthResult.Error(AppError.AuthenticationError("Invalid user ID"))
        }

        // Validate contacts
        if (contacts.isEmpty()) {
            return AuthResult.Error(AppError.ValidationError("At least one emergency contact is required"))
        }

        if (contacts.size > 3) {
            return AuthResult.Error(AppError.ValidationError("Maximum 3 emergency contacts allowed"))
        }

        // Clean contacts
        val cleanContacts = contacts.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanContacts.size != contacts.size) {
            return AuthResult.Error(AppError.ValidationError("All emergency contacts must be valid"))
        }

        // Prepare request
        val request = UpdateEmergencyContactsRequest(emergencyContacts = cleanContacts)

        // Make API call
        val response = safeApiCall {
            api.updateEmergencyContact(token, request)
        }

        return when (response) {
            is AuthResult.Success -> {
                // Update local cache with new contacts
                val updatedUser = userCache.getUser()?.copy(emergencyContacts = cleanContacts)
                updatedUser?.let { user ->
                    userCache.saveUser(user)
                    AuthResult.Success(user)
                } ?: run {
                    // If local cache is somehow empty, refresh from server
                    refreshUserData()
                }
            }
            is AuthResult.Error -> {
                // Handle specific error cases
                when {
                    response.error is AppError.ApiError && response.error.code == 401 -> {
                        tokenManager.clearAll()
                        userCache.clearUser()
                        AuthResult.Error(AppError.AuthenticationError("Session expired. Please login again."))
                    }
                    response.error is AppError.ApiError && response.error.code == 400 -> {
                        AuthResult.Error(AppError.ValidationError("Invalid emergency contact data"))
                    }
                    response.error is AppError.NetworkError -> {
                        AuthResult.Error(AppError.NetworkError("Network error. Please check your connection and try again."))
                    }
                    else -> response
                }
            }
        }
    }

    override suspend fun logout(): AuthResult<Unit> {
        return try {
            tokenManager.clearAll()
            userCache.clearUser()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(AppError.UnknownError("Failed to logout properly. Please restart the app."))
        }
    }
}
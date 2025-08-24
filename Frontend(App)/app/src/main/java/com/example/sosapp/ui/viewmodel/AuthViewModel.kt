package com.example.sosapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sosapp.data.model.AuthMode
import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.data.model.AuthUiState
import com.example.sosapp.data.model.OtpUiState
import com.example.sosapp.data.model.ProfileUiState
import com.example.sosapp.data.model.UserData
import com.example.sosapp.domain.usecase.AuthUseCase
import com.example.sosapp.domain.repository.AuthRepository
import com.example.sosapp.domain.model.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCase: AuthUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _otpUiState = MutableStateFlow(OtpUiState())
    val otpUiState: StateFlow<OtpUiState> = _otpUiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserData?>(null)
    val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())

    private val _userData = MutableStateFlow<UserData?>(null)

    // Retry counters for better UX
    private var loginRetryCount = 0
    private var registerRetryCount = 0
    private var otpVerifyRetryCount = 0

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                _currentUser.value = user
                _authUiState.value = _authUiState.value.copy(
                    isLoggedIn = user != null,
                    isInitializing = false
                )
            } catch (e: Exception) {
                _authUiState.value = _authUiState.value.copy(
                    isLoggedIn = false,
                    isInitializing = false
                )
            }
        }
    }

    fun refreshCurrentUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // First try to get cached user data
                val cachedUser = authRepository.getCurrentUser()
                if (cachedUser != null) {
                    _userData.value = cachedUser
                    _currentUser.value = cachedUser
                }

                // Then refresh from server
                when (val result = authRepository.refreshUserData()) {
                    is AuthResult.Success -> {
                        _userData.value = result.data
                        _currentUser.value = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = getErrorMessage(result.error)
                        )
                        // If we have cached data and server fails, keep using cached data
                        if (cachedUser == null) {
                            _userData.value = null
                            _currentUser.value = null
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "An unexpected error occurred. Please try again."
                )
            }
        }
    }

    private fun getErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.LocationError -> error.message
            is AppError.PermissionError -> error.message
            is AppError.NetworkError -> error.message
            is AppError.ApiError -> error.message
            is AppError.ValidationError -> error.message
            is AppError.AuthenticationError -> error.message
            is AppError.UnknownError -> error.message
        }
    }

    fun setAuthMode(mode: AuthMode) {
        _authUiState.value = _authUiState.value.copy(
            authMode = mode,
            errorMessage = null
        )
        // Reset retry counters when switching modes
        loginRetryCount = 0
        registerRetryCount = 0
    }

    // Login field updates with validation
    fun updateLoginEmail(email: String) {
        _authUiState.value = _authUiState.value.copy(
            loginEmail = email,
            errorMessage = null // Clear error when user starts typing
        )
    }

    fun updateLoginPassword(password: String) {
        _authUiState.value = _authUiState.value.copy(
            loginPassword = password,
            errorMessage = null // Clear error when user starts typing
        )
    }

    // Register field updates with validation
    fun updateRegisterFullName(fullName: String) {
        _authUiState.value = _authUiState.value.copy(
            registerFullName = fullName,
            errorMessage = null
        )
    }

    fun updateRegisterEmail(email: String) {
        _authUiState.value = _authUiState.value.copy(
            registerEmail = email,
            errorMessage = null
        )
    }

    fun updateRegisterPhone(phone: String) {
        _authUiState.value = _authUiState.value.copy(
            registerPhone = phone,
            errorMessage = null
        )
    }

    fun updateRegisterPassword(password: String) {
        _authUiState.value = _authUiState.value.copy(
            registerPassword = password,
            errorMessage = null
        )
    }

    fun updateEmergencyContact1(contact: String) {
        _authUiState.value = _authUiState.value.copy(
            emergencyContact1 = contact,
            errorMessage = null
        )
    }

    fun updateEmergencyContact2(contact: String) {
        _authUiState.value = _authUiState.value.copy(
            emergencyContact2 = contact,
            errorMessage = null
        )
    }

    fun updateEmergencyContact3(contact: String) {
        _authUiState.value = _authUiState.value.copy(
            emergencyContact3 = contact,
            errorMessage = null
        )
    }

    // OTP updates with better validation
    fun updateOtpValue(index: Int, value: String) {
        val currentValues = _otpUiState.value.otpValues.toMutableList()
        if (index in 0 until currentValues.size) {
            // Handle single digit input
            if (value.length <= 1) {
                currentValues[index] = value
            } else if (value.length == 6 && value.all { it.isDigit() }) {
                // Handle paste of full OTP
                value.forEachIndexed { i, char ->
                    if (i < currentValues.size) {
                        currentValues[i] = char.toString()
                    }
                }
            }
        }
        _otpUiState.value = _otpUiState.value.copy(
            otpValues = currentValues,
            errorMessage = null
        )
    }

    fun updateResendTimer() {
        val currentTimer = _otpUiState.value.resendTimer
        if (currentTimer > 0) {
            _otpUiState.value = _otpUiState.value.copy(
                resendTimer = currentTimer - 1,
                canResend = currentTimer - 1 <= 0
            )
        }
    }

    // Enhanced login with retry logic
    fun login() {
        val state = _authUiState.value
        if (state.isLoading) return

        // Client-side validation
        if (state.loginEmail.isBlank() || state.loginPassword.isBlank()) {
            _authUiState.value = state.copy(
                errorMessage = "Please enter both email and password"
            )
            return
        }

        _authUiState.value = state.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            performLoginWithRetry()
        }
    }

    private suspend fun performLoginWithRetry() {
        val state = _authUiState.value

        when (val result = authUseCase.login(state.loginEmail.trim(), state.loginPassword)) {
            is AuthResult.Success -> {
                loginRetryCount = 0 // Reset on success
                _currentUser.value = result.data
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    errorMessage = null
                )
            }
            is AuthResult.Error -> {
                val shouldRetry = loginRetryCount < MAX_RETRY_ATTEMPTS &&
                        (result.error is AppError.NetworkError ||
                                (result.error is AppError.ApiError && result.error.code >= 500))

                if (shouldRetry) {
                    loginRetryCount++
                    delay(RETRY_DELAY_MS * loginRetryCount) // Exponential backoff
                    performLoginWithRetry()
                } else {
                    val errorMessage = if (loginRetryCount >= MAX_RETRY_ATTEMPTS) {
                        "Login failed after multiple attempts. Please check your connection and try again."
                    } else {
                        getErrorMessage(result.error)
                    }

                    loginRetryCount = 0

                    _authUiState.value = _authUiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    // Enhanced register with retry logic
    fun register() {
        val state = _authUiState.value
        if (state.isRegistering) return

        // Client-side validation
        if (state.registerFullName.isBlank() || state.registerEmail.isBlank() ||
            state.registerPassword.isBlank() || state.registerPhone.isBlank()) {
            _authUiState.value = state.copy(
                errorMessage = "Please fill in all required fields"
            )
            return
        }

        val emergencyContacts = listOfNotNull(
            state.emergencyContact1.takeIf { it.isNotBlank() },
            state.emergencyContact2.takeIf { it.isNotBlank() },
            state.emergencyContact3.takeIf { it.isNotBlank() }
        )

        if (emergencyContacts.isEmpty()) {
            _authUiState.value = state.copy(
                errorMessage = "Please provide at least one emergency contact"
            )
            return
        }

        _authUiState.value = state.copy(
            isRegistering = true,
            errorMessage = null
        )

        viewModelScope.launch {
            performRegisterWithRetry(state, emergencyContacts)
        }
    }

    private suspend fun performRegisterWithRetry(state: AuthUiState, emergencyContacts: List<String>) {
        when (val result = authUseCase.register(
            fullName = state.registerFullName.trim(),
            email = state.registerEmail.trim(),
            password = state.registerPassword,
            phone = state.registerPhone.trim(),
            emergencyContacts = emergencyContacts
        )) {
            is AuthResult.Success -> {
                registerRetryCount = 0 // Reset on success
                _authUiState.value = _authUiState.value.copy(
                    isRegistering = false,
                    errorMessage = null
                )
                // Set up OTP state
                _otpUiState.value = _otpUiState.value.copy(
                    email = state.registerEmail.trim(),
                    resendTimer = 60,
                    canResend = false,
                    otpValues = List(6) { "" }, // Reset OTP values
                    errorMessage = null
                )
            }
            is AuthResult.Error -> {
                val shouldRetry = registerRetryCount < MAX_RETRY_ATTEMPTS &&
                        (result.error is AppError.NetworkError ||
                                (result.error is AppError.ApiError && result.error.code >= 500))

                if (shouldRetry) {
                    registerRetryCount++
                    delay(RETRY_DELAY_MS * registerRetryCount)
                    performRegisterWithRetry(state, emergencyContacts)
                } else {
                    val errorMessage = if (registerRetryCount >= MAX_RETRY_ATTEMPTS) {
                        "Registration failed after multiple attempts. Please check your connection and try again."
                    } else {
                        getErrorMessage(result.error)
                    }

                    registerRetryCount = 0 // Reset counter

                    _authUiState.value = _authUiState.value.copy(
                        isRegistering = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    // Enhanced OTP verification
    fun verifyOtp() {
        val otpState = _otpUiState.value
        if (otpState.isVerifying) return

        val otp = otpState.otpValues.joinToString("")
        if (otp.length != 6) {
            _otpUiState.value = otpState.copy(
                errorMessage = "Please enter complete 6-digit OTP"
            )
            return
        }

        if (!otp.all { it.isDigit() }) {
            _otpUiState.value = otpState.copy(
                errorMessage = "OTP must contain only numbers"
            )
            return
        }

        _otpUiState.value = otpState.copy(
            isVerifying = true,
            errorMessage = null
        )

        viewModelScope.launch {
            performOtpVerificationWithRetry(otpState.email, otp)
        }
    }

    private suspend fun performOtpVerificationWithRetry(email: String, otp: String) {
        when (val result = authUseCase.verifyOTP(email, otp)) {
            is AuthResult.Success -> {
                otpVerifyRetryCount = 0 // Reset on success
                _currentUser.value = result.data
                _otpUiState.value = _otpUiState.value.copy(
                    isVerifying = false,
                    isVerified = true,
                    errorMessage = null
                )
                _authUiState.value = _authUiState.value.copy(
                    isLoggedIn = true
                )
            }
            is AuthResult.Error -> {
                val shouldRetry = otpVerifyRetryCount < MAX_RETRY_ATTEMPTS &&
                        (result.error is AppError.NetworkError ||
                                (result.error is AppError.ApiError && result.error.code >= 500))

                if (shouldRetry) {
                    otpVerifyRetryCount++
                    delay(RETRY_DELAY_MS * otpVerifyRetryCount)
                    performOtpVerificationWithRetry(email, otp)
                } else {
                    val errorMessage = if (otpVerifyRetryCount >= MAX_RETRY_ATTEMPTS) {
                        "OTP verification failed after multiple attempts. Please try again or request a new OTP."
                    } else {
                        getErrorMessage(result.error)
                    }

                    otpVerifyRetryCount = 0 // Reset counter

                    _otpUiState.value = _otpUiState.value.copy(
                        isVerifying = false,
                        errorMessage = errorMessage
                    )
                }
            }
        }
    }

    fun resendOtp() {
        val otpState = _otpUiState.value
        if (!otpState.canResend) return

        viewModelScope.launch {
            when (val result = authRepository.resendOTP(otpState.email)) {
                is AuthResult.Success -> {
                    _otpUiState.value = _otpUiState.value.copy(
                        resendTimer = 60,
                        canResend = false,
                        errorMessage = null,
                        otpValues = List(6) { "" } // Clear previous OTP
                    )
                }
                is AuthResult.Error -> {
                    _otpUiState.value = _otpUiState.value.copy(
                        errorMessage = getErrorMessage(result.error)
                    )
                }
            }
        }
    }

    fun updateEmergencyContacts(contacts: List<String>) {
        if (_authUiState.value.isUpdatingEmergencyContacts) return

        _authUiState.value = _authUiState.value.copy(
            isUpdatingEmergencyContacts = true,
            emergencyContactsUpdateSuccess = null,
            emergencyContactsUpdateError = null
        )

        viewModelScope.launch {
            when (val result = authUseCase.updateEmergencyContacts(contacts)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.data
                    _authUiState.value = _authUiState.value.copy(
                        isUpdatingEmergencyContacts = false,
                        emergencyContactsUpdateSuccess = true,
                        emergencyContactsUpdateError = null
                    )
                }
                is AuthResult.Error -> {
                    _authUiState.value = _authUiState.value.copy(
                        isUpdatingEmergencyContacts = false,
                        emergencyContactsUpdateSuccess = false,
                        emergencyContactsUpdateError = getErrorMessage(result.error)
                    )
                }
            }
        }
    }

    fun clearEmergencyContactsUpdateState() {
        _authUiState.value = _authUiState.value.copy(
            emergencyContactsUpdateSuccess = null,
            emergencyContactsUpdateError = null
        )
    }

    fun clearAuthError() {
        _authUiState.value = _authUiState.value.copy(errorMessage = null)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _currentUser.value = null
                _authUiState.value = AuthUiState() // Reset to initial state
                _otpUiState.value = OtpUiState() // Reset OTP state
                _uiState.value = ProfileUiState() // Reset profile state
                _userData.value = null

                // Reset retry counters
                loginRetryCount = 0
                registerRetryCount = 0
                otpVerifyRetryCount = 0
            } catch (e: Exception) {
                // Even if logout fails, clear local state
                _currentUser.value = null
                _authUiState.value = AuthUiState()
                _otpUiState.value = OtpUiState()
                _uiState.value = ProfileUiState()
                _userData.value = null
            }
        }
    }
}
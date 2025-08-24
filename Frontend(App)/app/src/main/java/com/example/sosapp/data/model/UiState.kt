package com.example.sosapp.data.model

enum class AuthMode {
    LOGIN, REGISTER
}

data class AuthUiState(
    val authMode: AuthMode = AuthMode.LOGIN,
    val loginEmail: String = "",
    val loginPassword: String = "",
    val registerFullName: String = "",
    val registerEmail: String = "",
    val registerPhone: String = "",
    val registerPassword: String = "",
    val emergencyContact1: String = "",
    val emergencyContact2: String = "",
    val emergencyContact3: String = "",
    val isInitializing: Boolean = true,
    val isLoading: Boolean = false,
    val isRegistering: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isUpdatingEmergencyContacts: Boolean = false,
    val emergencyContactsUpdateSuccess: Boolean? = null,
    val emergencyContactsUpdateError: String? = null
)

data class OtpUiState(
    val email: String = "",
    val otpValues: List<String> = List(6) { "" },
    val isVerifying: Boolean = false,
    val errorMessage: String? = null,
    val resendTimer: Int = 60,
    val canResend: Boolean = false,
    val isVerified: Boolean = false
)

data class EmergencyUiState(
    val isPanicActive: Boolean = false,
    val isLocationSharing: Boolean = false,
    val isNotifyingContacts: Boolean = false,
    val emergencyContacts: List<String> = emptyList(),
    val deviceStatus: DeviceStatus = DeviceStatus(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
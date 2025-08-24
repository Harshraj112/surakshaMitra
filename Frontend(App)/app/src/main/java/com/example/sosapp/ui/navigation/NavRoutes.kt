package com.example.sosapp.ui.navigation

sealed class NavRoutes(val route: String, val title: String) {
    data object Login : NavRoutes("login", "Login")
    data object OTPVerification : NavRoutes("otp_verification", "OTP Verification")
    data object Panic : NavRoutes("panic", "Emergency Alert")
    data object EmergencyContacts : NavRoutes("emergency_contacts", "Emergency Contacts")
    data object EmergencyDirectory : NavRoutes("emergency_directory", "Emergency Directory")
    data object Profile : NavRoutes("profile", "Profile")

    companion object {
        val drawerScreens = listOf(
            Panic,
            EmergencyContacts,
            EmergencyDirectory,
            Profile
        )
    }
}
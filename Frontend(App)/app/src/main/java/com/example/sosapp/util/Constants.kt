package com.example.sosapp.util

import com.example.sosapp.data.model.EmergencyContact

object Constants {
    // Notification Channels
    const val EMERGENCY_CHANNEL_ID = "emergency_channel"
    const val GENERAL_CHANNEL_ID = "general_channel"

    // Notification IDs
    const val EMERGENCY_NOTIFICATION_ID = 1001

    // Location Constants
    const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
    const val LOCATION_UPDATE_DISTANCE = 10f   // 10 meters

    // Validation Constants
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_EMERGENCY_CONTACTS = 3
    const val OTP_LENGTH = 6

    // Emergency Numbers by Country
    private val indianEmergencyNumbers = listOf(
        EmergencyContact(
            label = "POLICE",
            number = "100",
            description = "Police Emergency Services"
        ),
        EmergencyContact(
            label = "FIRE",
            number = "101",
            description = "Fire Emergency Services"
        ),
        EmergencyContact(
            label = "AMBULANCE",
            number = "102",
            description = "Medical Emergency Services"
        ),
        EmergencyContact(
            label = "TRAFFIC POLICE",
            number = "103",
            description = "Traffic Police Services"
        ),
        EmergencyContact(
            label = "WOMEN HELPLINE",
            number = "1091",
            description = "Women in Distress"
        ),
        EmergencyContact(
            label = "CHILD HELPLINE",
            number = "1098",
            description = "Child Emergency Services"
        ),
        EmergencyContact(
            label = "RAILWAY HELPLINE",
            number = "138",
            description = "Railway Emergency Services"
        )
    )

    fun getEmergencyNumbers(countryCode: String = "IN"): List<EmergencyContact> {
        return when (countryCode.uppercase()) {
            "IN" -> indianEmergencyNumbers
            // Add more countries as needed
            else -> indianEmergencyNumbers
        }
    }
}
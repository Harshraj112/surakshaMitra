package com.example.sosapp.data.remote.dto.remote

data class UserProfileResponse(
    val status: String,
    val message: String,
    val user: UserProfileData
)

data class UserProfileData(
    val fullname: String,
    val email: String,
    val phone: String,
    val emergencyContacts: List<String>,
    val isVerified: Boolean,
    val createdAt: String,
    val alertsCount: Int,
    val locationHistoryCount: Int
)

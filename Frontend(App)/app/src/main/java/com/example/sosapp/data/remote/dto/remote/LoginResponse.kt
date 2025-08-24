package com.example.sosapp.data.remote.dto.remote

class LoginResponse (
    val status: String,
    val message: String,
    val token: String,
    val user: LoginUserData,
    val userId: String
)

data class LoginUserData(
    val fullname: String,
    val email: String,
    val phone: String,
    val emergencyContacts: List<String>,
    val createdAt: Boolean
)
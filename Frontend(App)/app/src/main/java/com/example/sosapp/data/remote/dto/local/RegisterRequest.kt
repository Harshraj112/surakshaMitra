package com.example.sosapp.data.remote.dto.local

data class RegisterRequest(
    val fullname: String,
    val email: String,
    val phone: String,
    val password: String,
    val emergencyContacts: List<String> = emptyList()
)
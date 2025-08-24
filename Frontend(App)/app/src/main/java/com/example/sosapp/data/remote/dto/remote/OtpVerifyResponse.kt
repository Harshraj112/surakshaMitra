package com.example.sosapp.data.remote.dto.remote

data class OtpVerifyResponse(
    val status: String,
    val message: String,
    val userId: String,
    val token: String
)

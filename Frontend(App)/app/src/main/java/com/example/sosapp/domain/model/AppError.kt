package com.example.sosapp.domain.model

sealed class AppError(open val message: String) {
    data class NetworkError(override val message: String) : AppError(message)
    data class ApiError(val code: Int, override val message: String) : AppError(message)
    data class AuthenticationError(override val message: String) : AppError(message)
    data class ValidationError(override val message: String) : AppError(message)
    data class LocationError(override val message: String) : AppError(message)
    data class PermissionError(override val message: String) : AppError(message)
    data class UnknownError(override val message: String) : AppError(message)
}
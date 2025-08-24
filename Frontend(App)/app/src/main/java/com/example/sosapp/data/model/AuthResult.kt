package com.example.sosapp.data.model

import com.example.sosapp.domain.model.AppError

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val error: AppError) : AuthResult<Nothing>() {
        constructor(message: String) : this(AppError.UnknownError(message))
    }
}
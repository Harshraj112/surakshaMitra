package com.example.sosapp.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.example.sosapp.data.model.AuthResult
import com.example.sosapp.domain.model.AppError

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}

fun <T> Flow<T>.asResource(): Flow<Resource<T>> {
    return this
        .map<T, Resource<T>> { Resource.Success(it) }
        .onStart { emit(Resource.Loading()) }
        .catch { emit(Resource.Error(it.message ?: "Unknown error")) }
}

fun <T> AuthResult<T>.toResource(): Resource<T> {
    return when (this) {
        is AuthResult.Success -> Resource.Success(data)
        is AuthResult.Error -> Resource.Error(error.message)
    }
}

fun String.isValidEmail(): Boolean = ValidationUtils.isValidEmail(this)
fun String.isValidPhone(): Boolean = ValidationUtils.isValidPhone(this)
fun String.formatPhone(): String = ValidationUtils.formatPhoneNumber(this)
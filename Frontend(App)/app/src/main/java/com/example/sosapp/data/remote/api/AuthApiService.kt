package com.example.sosapp.data.remote.api

import com.example.sosapp.data.remote.dto.local.UpdateEmergencyContactsRequest
import com.example.sosapp.data.remote.dto.local.LoginRequest
import com.example.sosapp.data.remote.dto.local.OtpRequest
import com.example.sosapp.data.remote.dto.local.RegisterRequest
import com.example.sosapp.data.remote.dto.remote.LoginResponse
import com.example.sosapp.data.remote.dto.remote.OtpResponse
import com.example.sosapp.data.remote.dto.remote.OtpVerifyResponse
import com.example.sosapp.data.remote.dto.remote.UpdateEmergencyContactsResponse
import com.example.sosapp.data.remote.dto.remote.UserProfileResponse
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("users/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("users/send-otp")
    suspend fun sendOtp(@Body request: RegisterRequest): Response<OtpResponse>

    @POST("users/verify-otp")
    suspend fun verifyOtp(@Body request: OtpRequest): Response<OtpVerifyResponse>

    @POST("users/resend-otp")
    suspend fun resendOtp(@Body request: OtpRequest): Response<OtpResponse>

    @GET("users/profile/{id}")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<UserProfileResponse>

    @PUT("users/update/emergency-contacts")
    suspend fun updateEmergencyContact(
        @Header("Authorization") token: String,
        @Body request: UpdateEmergencyContactsRequest
    ): Response<UpdateEmergencyContactsResponse>
}
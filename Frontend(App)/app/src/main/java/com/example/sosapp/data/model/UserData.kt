package com.example.sosapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.sosapp.data.local.db.Converters

@Entity(tableName = "user_data")
@TypeConverters(Converters::class)
data class UserData(
    @PrimaryKey
    val id: String = "",
    val fullName: String = "User Name",
    val email: String = "user@example.com",
    val phone: String = "+917318794439",
    val emergencyContacts: List<String> = emptyList(),
    val isVerified: Boolean = true,
    val profilePictureUrl: String? = null,
    val createdAt: String = "January 2024",
    val lastLoginAt: String = "Today"
)
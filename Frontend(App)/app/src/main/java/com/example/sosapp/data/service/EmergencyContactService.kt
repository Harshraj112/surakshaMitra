package com.example.sosapp.data.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactService @Inject constructor() {

    fun hasCallPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun makeEmergencyCall(context: Context, phoneNumber: String): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {

                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:$phoneNumber".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } else {
                // If no permission, open dialer instead
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = "tel:$phoneNumber".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                false // Indicates that we opened dialer instead of making direct call
            }
        } catch (e: Exception) {
            false
        }
    }

}
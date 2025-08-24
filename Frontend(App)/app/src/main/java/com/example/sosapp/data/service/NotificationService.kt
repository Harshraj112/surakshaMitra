package com.example.sosapp.data.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sosapp.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val emergencyChannel = NotificationChannel(
            Constants.EMERGENCY_CHANNEL_ID,
            "Emergency Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical emergency notifications"
            enableVibration(true)
            enableLights(true)
        }

        val generalChannel = NotificationChannel(
            Constants.GENERAL_CHANNEL_ID,
            "General Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General app notifications"
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannels(listOf(emergencyChannel, generalChannel))
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showEmergencyActiveNotification() {
        val notification = NotificationCompat.Builder(context, Constants.EMERGENCY_CHANNEL_ID)
            .setContentTitle("Emergency SOS Active")
            .setContentText("Emergency alert has been triggered. Contacts have been notified.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(Constants.EMERGENCY_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle notification permission not granted
        }
    }

    fun hideEmergencyNotification() {
        NotificationManagerCompat.from(context)
            .cancel(Constants.EMERGENCY_NOTIFICATION_ID)
    }
}
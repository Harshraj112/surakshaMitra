package com.example.sosapp.data.model

data class DeviceStatus(
    val bandConnected: Boolean = false,
    val locationEnabled: Boolean = false,
    val callPermissionGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val lastUpdate: String = "Never"
)
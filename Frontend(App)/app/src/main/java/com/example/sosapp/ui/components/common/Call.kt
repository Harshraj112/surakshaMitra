package com.example.sosapp.ui.components.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CallPermissionDialog(
    phoneNumber: String,
    onRequestPermission: () -> Unit,
    onUseDial: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Call Permission Required")
        },
        text = {
            Text("This app needs permission to make direct calls to $phoneNumber. " +
                    "You can either grant permission for direct calling or use the dialer.")
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onUseDial) {
                Text("Use Dialer")
            }
        }
    )
}
package com.example.sosapp.ui.components.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sosapp.data.model.ContactValidationState
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.CustomTextField
import com.example.sosapp.ui.components.common.InfoCard
import com.example.sosapp.ui.components.common.SectionTitle
import com.example.sosapp.ui.theme.EmergencyColors

@Composable
fun EmergencyHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            text = "Emergency Contacts",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )
        BodyText(
            text = "Add your emergency contacts for quick access during critical situations",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EmergencyContactsList(
    contacts: List<String>,
    onCallClick: (String) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            text = "Your Emergency Contacts",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp
        )
        contacts.forEachIndexed { index, contact ->
            EmergencyContactCard(
                contactNumber = contact,
                contactName = when (index) {
                    0 -> "Primary Contact"
                    1 -> "Secondary Contact"
                    2 -> "Tertiary Contact"
                    else -> "Contact ${index + 1}"
                },
                onCallClick = { onCallClick(contact) },
                onDeleteClick = { onDeleteClick(index) }
            )
        }
    }
}

@Composable
fun EmergencyContactCard(
    contactNumber: String,
    contactName: String,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                BodyText(text = contactName, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                BodyText(text = contactNumber, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            }
            Row {
                IconButton(onClick = onCallClick) {
                    Icon(Icons.Default.Phone, contentDescription = "Call")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun EmergencyContactFields(
    contact1: String,
    contact2: String,
    contact3: String,
    contact1Validation: ContactValidationState,
    contact2Validation: ContactValidationState,
    contact3Validation: ContactValidationState,
    onContact1Change: (String) -> Unit,
    onContact2Change: (String) -> Unit,
    onContact3Change: (String) -> Unit,
    hasExistingContacts: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionTitle(
            text = if (hasExistingContacts) "Update Contacts" else "Add Contact Information",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp
        )
        ContactField(
            label = "Primary Contact (Required)",
            value = contact1,
            onValueChange = onContact1Change,
            validation = contact1Validation
        )
        ContactField(
            label = "Secondary Contact (Optional)",
            value = contact2,
            onValueChange = onContact2Change,
            validation = contact2Validation
        )
        ContactField(
            label = "Tertiary Contact (Optional)",
            value = contact3,
            onValueChange = onContact3Change,
            validation = contact3Validation
        )
    }
}

@Composable
fun ContactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    validation: ContactValidationState
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BodyText(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
        CustomTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "Enter phone number (e.g., +1234567890)",
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!validation.isValid) {
            BodyText(
                text = validation.errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun EmergencyInfoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BodyText(
            text = "ℹ️ Important Information",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        BodyText(
            text = "• At least one contact is required\n• These contacts will receive emergency alerts\n• Include country code for international numbers\n• Make sure the numbers are active and reachable\n• Tap the call button to test contacts",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ArduinoConnectionCard(
    isConnected: Boolean,
    connectionStatus: String,
    lastUpdate: String?,
    onConnectClick: () -> Unit,
    onTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    text = "Arduino Connection",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )

                // Connection status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isConnected) EmergencyColors.SafeGreen else MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    )
                    BodyText(
                        text = if (isConnected) "Connected" else "Disconnected",
                        color = if (isConnected) EmergencyColors.SafeGreen else MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Connection status details
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = "Status:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    BodyText(
                        text = connectionStatus,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }

                if (lastUpdate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyText(
                            text = "Last Update:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        BodyText(
                            text = lastUpdate,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connect/Reconnect button
                OutlinedButton(
                    onClick = onConnectClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    BodyText(
                        text = if (isConnected) "Reconnect" else "Connect",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Test connection button
                OutlinedButton(
                    onClick = onTestClick,
                    enabled = isConnected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BodyText(
                        text = "Test",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Connection info
            if (!isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BodyText(
                        text = "Make sure your HC-05 Arduino module is paired and nearby. The app will automatically send location and SOS signals to Arduino when connected.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = EmergencyColors.SafeGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BodyText(
                        text = "✓ Arduino connected! Emergency signals and location data will be automatically sent to your device.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
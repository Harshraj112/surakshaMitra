package com.example.sosapp.ui.components.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.InfoCard
import com.example.sosapp.ui.components.common.SectionTitle
import com.example.sosapp.ui.theme.EmergencyColors

@Composable
fun VoiceDetectionCard(
    isActive: Boolean,
    isInitialized: Boolean,
    lastDetectedText: String,
    detectedKeywords: List<String>,
    emergencyKeywords: List<String>,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (isActive) "Microphone active" else "Microphone inactive",
                            tint = if (isActive) EmergencyColors.SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        SectionTitle(
                            text = "Voice Detection",
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    BodyText(
                        text = when {
                            !isInitialized -> "Initializing..."
                            isActive -> "🎤 Listening for emergency keywords"
                            else -> "Tap to start voice detection"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = { if (isInitialized) onToggle() },
                    enabled = isInitialized
                )
            }

            // Status indicators
            if (isActive) {
                VoiceDetectionStatus(
                    lastDetectedText = lastDetectedText,
                    detectedKeywords = detectedKeywords
                )
            }

            // Emergency keywords info
            if (!isActive) {
                EmergencyKeywordsInfo(keywords = emergencyKeywords)
            }
        }
    }
}

@Composable
private fun VoiceDetectionStatus(
    lastDetectedText: String,
    detectedKeywords: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live detection indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pulsing microphone indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = EmergencyColors.SafeGreen,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            BodyText(
                text = "Listening... Say 'danger' or 'emergency' to trigger panic",
                fontSize = 13.sp,
                color = EmergencyColors.SafeGreen,
                fontWeight = FontWeight.Medium
            )
        }

        // Last detected text
        if (lastDetectedText.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    BodyText(
                        text = "Last detected:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Fixed: Use Text composable directly for italic style
                    Text(
                        text = "\"$lastDetectedText\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Detected emergency keywords
        if (detectedKeywords.isNotEmpty()) {
            Column {
                BodyText(
                    text = "Recent emergency keywords:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detectedKeywords.takeLast(5)) { keyword ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            BodyText(
                                text = keyword,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyKeywordsInfo(keywords: List<String>) {
    Column {
        BodyText(
            text = "Emergency keywords that will trigger panic:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show first few keywords as examples
        val displayKeywords = keywords.take(8)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(displayKeywords) { keyword ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    BodyText(
                        text = keyword,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            if (keywords.size > 8) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        BodyText(
                            text = "+${keywords.size - 8} more",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceDetectionPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = "Microphone permission required",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            SectionTitle(
                text = "Microphone Permission Required",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            BodyText(
                text = "Voice detection requires microphone permission to listen for emergency keywords and automatically trigger panic mode.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant Permission")
            }
        }
    }
}
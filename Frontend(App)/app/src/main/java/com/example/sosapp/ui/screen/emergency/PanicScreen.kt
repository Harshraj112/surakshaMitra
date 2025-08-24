package com.example.sosapp.ui.screen.emergency

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sosapp.R
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.InfoCard
import com.example.sosapp.ui.components.common.PanicButton
import com.example.sosapp.ui.components.common.SectionTitle
import com.example.sosapp.ui.components.emergency.VoiceDetectionCard
import com.example.sosapp.ui.components.emergency.VoiceDetectionPermissionCard
import com.example.sosapp.ui.theme.EmergencyColors
import com.example.sosapp.ui.viewmodel.EmergencyViewModel

@Composable
fun PanicScreen(
    viewModel: EmergencyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Voice detection states
    val isVoiceDetectionInitialized by viewModel.isVoiceDetectionInitialized.collectAsStateWithLifecycle()
    val isVoiceDetectionActive by viewModel.isVoiceDetectionActive.collectAsStateWithLifecycle()
    val lastDetectedText by viewModel.lastDetectedText.collectAsStateWithLifecycle()
    val detectedKeywords by viewModel.detectedKeywords.collectAsStateWithLifecycle()

    // Use mutable state for permission that updates when permission changes
    var hasAudioPermission by remember { mutableStateOf(false) }

    // Check permission initially and after permission launcher
    LaunchedEffect(context) {
        hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Permission launcher for microphone
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Update permission state when result comes back
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startVoiceDetection()
        }
    }

    // Show snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (uiState.isPanicActive) Color.Red.copy(alpha = 0.2f)
                else EmergencyColors.EmergencyBackground
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Voice Detection Card
            when {
                isVoiceDetectionInitialized && hasAudioPermission -> {
                    VoiceDetectionCard(
                        isActive = isVoiceDetectionActive,
                        isInitialized = isVoiceDetectionInitialized,
                        lastDetectedText = lastDetectedText,
                        detectedKeywords = detectedKeywords,
                        emergencyKeywords = viewModel.getEmergencyKeywords(),
                        onToggle = {
                            if (isVoiceDetectionActive) {
                                viewModel.stopVoiceDetection()
                            } else {
                                viewModel.startVoiceDetection()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                isVoiceDetectionInitialized && !hasAudioPermission -> {
                    VoiceDetectionPermissionCard(
                        onRequestPermission = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                !isVoiceDetectionInitialized -> {
                    // Show loading card while initializing
                    InfoCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            BodyText(
                                text = "Initializing voice detection...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Device Status Card
            InfoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionTitle(
                        text = "Device Status",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DeviceStatusRow(
                        label = "Location Services:",
                        isConnected = uiState.deviceStatus.locationEnabled
                    )

                    DeviceStatusRow(
                        label = "Voice Detection:",
                        isConnected = isVoiceDetectionActive && hasAudioPermission
                    )

                    BodyText(
                        text = "Last updated: ${uiState.deviceStatus.lastUpdate}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Panic Button Section
            InfoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    SectionTitle(
                        text = if (uiState.isPanicActive) "EMERGENCY ACTIVE" else "Emergency Alert",
                        color = if (uiState.isPanicActive)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PanicButton(
                        isActive = uiState.isPanicActive,
                        onClick = {
                            if (uiState.isPanicActive) {
                                viewModel.stopEmergency()
                            } else {
                                viewModel.triggerEmergency()
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    )

                    if (uiState.isPanicActive) {
                        BodyText(
                            text = "🚨 Emergency alert is active!\nEmergency contacts have been notified.\nTap the button again to stop.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BodyText(
                                text = "Press the panic button in case of emergency.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isVoiceDetectionActive) {
                                BodyText(
                                    text = "Or say 'help', 'emergency', 'stop', or other emergency keywords.",
                                    color = EmergencyColors.SafeGreen,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }

                            BodyText(
                                text = "This will immediately alert your emergency contacts and share your location.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (uiState.isNotifyingContacts) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            BodyText(
                                text = "Notifying emergency contacts...",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Instructions Card - only show when not in panic mode
            if (!uiState.isPanicActive) {
                InfoCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionTitle(
                            text = "How it works",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BodyText(
                                text = "• Press the panic button to send emergency alerts",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )

                            if (isVoiceDetectionActive) {
                                BodyText(
                                    text = "• Say emergency keywords to automatically trigger alerts",
                                    color = EmergencyColors.SafeGreen,
                                    fontSize = 14.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }

                            BodyText(
                                text = "• Your location will be shared automatically",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            BodyText(
                                text = "• All emergency contacts will be notified",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            BodyText(
                                text = "• Emergency services can be contacted if needed",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DeviceStatusRow(
    label: String,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BodyText(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isConnected)
                            EmergencyColors.SafeGreen
                        else
                            MaterialTheme.colorScheme.error,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            BodyText(
                text = if (isConnected) "Connected" else "Disconnected",
                color = if (isConnected)
                    EmergencyColors.SafeGreen
                else
                    MaterialTheme.colorScheme.error,
                fontSize = 16.sp
            )
        }
    }
}
package com.example.sosapp.ui.screen.emergency

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sosapp.R
import com.example.sosapp.ui.components.common.AppTitle
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.CallPermissionDialog
import com.example.sosapp.ui.components.common.EmergencyButton
import com.example.sosapp.ui.theme.EmergencyColors
import com.example.sosapp.ui.theme.EmergencyTextStyles
import com.example.sosapp.ui.viewmodel.EmergencyViewModel

@Composable
fun EmergencyDirectoryScreen(
    viewModel: EmergencyViewModel = hiltViewModel(),
    countryCode: String = "IN"
) {
    val emergencyNumbers by viewModel.emergencyDirectory.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showCallPermissionDialog by viewModel.showCallPermissionDialog.collectAsStateWithLifecycle()
    val pendingCall by viewModel.pendingCall.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launcher for calling
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onCallPermissionGranted()
        } else {
            viewModel.onCallPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadEmergencyDirectory(countryCode)
    }

    // Handle success/error messages from emergency viewmodel
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar("Error: $error")
        }
    }

    // Show call permission dialog
    if (showCallPermissionDialog && pendingCall != null) {
        CallPermissionDialog(
            phoneNumber = pendingCall!!,
            onRequestPermission = {
                viewModel.dismissCallPermissionDialog()
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            },
            onUseDial = {
                viewModel.onCallPermissionDenied()
            },
            onDismiss = {
                viewModel.dismissCallPermissionDialog()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmergencyColors.EmergencyBackground),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                EmergencyDirectoryHeader()
                Spacer(modifier = Modifier.height(32.dp))
            }

            items(emergencyNumbers) { contact ->
                EmergencyButton(
                    label = contact.label,
                    number = contact.number,
                    onClick = {
                        viewModel.makeEmergencyCall(contact.number)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                EmergencyDirectoryFooter()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun EmergencyDirectoryHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppTitle(
            text = "EMERGENCY",
            color = EmergencyColors.EmergencyText,
            style = EmergencyTextStyles.EmergencyTitle
        )
        AppTitle(
            text = "DIRECTORY",
            color = EmergencyColors.EmergencyText,
            style = EmergencyTextStyles.EmergencyTitle
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    BodyText(
        text = "Tap on any number to call immediately",
        color = EmergencyColors.EmergencyText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EmergencyDirectoryFooter() {
    Spacer(modifier = Modifier.height(16.dp))

    BodyText(
        text = "Note: These are emergency numbers for India. For other countries, please check local emergency services.",
        color = EmergencyColors.NeutralGray,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
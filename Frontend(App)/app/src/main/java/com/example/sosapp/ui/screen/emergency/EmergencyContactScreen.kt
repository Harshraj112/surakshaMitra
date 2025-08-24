package com.example.sosapp.ui.screen.emergency

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sosapp.R
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.CallPermissionDialog
import com.example.sosapp.ui.components.common.InfoCard
import com.example.sosapp.ui.components.common.PrimaryButton
import com.example.sosapp.ui.components.emergency.EmergencyContactFields
import com.example.sosapp.ui.components.emergency.EmergencyContactsList
import com.example.sosapp.ui.components.emergency.EmergencyHeader
import com.example.sosapp.ui.components.emergency.EmergencyInfoSection
import com.example.sosapp.ui.theme.EmergencyColors
import com.example.sosapp.ui.viewmodel.AuthViewModel
import com.example.sosapp.ui.viewmodel.EmergencyViewModel

@Composable
fun EmergencyContactScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    emergencyViewModel: EmergencyViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val authState by authViewModel.authUiState.collectAsStateWithLifecycle()
    val emergencyUiState by emergencyViewModel.uiState.collectAsState()
    val showCallPermissionDialog by emergencyViewModel.showCallPermissionDialog.collectAsState()
    val pendingCall by emergencyViewModel.pendingCall.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val contact1 by emergencyViewModel.contact1.collectAsState()
    val contact2 by emergencyViewModel.contact2.collectAsState()
    val contact3 by emergencyViewModel.contact3.collectAsState()
    val contact1Validation by emergencyViewModel.contact1Validation.collectAsState()
    val contact2Validation by emergencyViewModel.contact2Validation.collectAsState()
    val contact3Validation by emergencyViewModel.contact3Validation.collectAsState()
    val isInitialized by emergencyViewModel.isInitialized.collectAsState()

    // Permission launcher for calling
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            emergencyViewModel.onCallPermissionGranted()
        } else {
            emergencyViewModel.onCallPermissionDenied()
        }
    }

    // Initialize contacts from user data once
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            if (!isInitialized) {
                emergencyViewModel.initializeContacts(user.emergencyContacts)
            }
        }
    }

    // Handle success/error states from auth
    LaunchedEffect(authState.emergencyContactsUpdateSuccess) {
        if (authState.emergencyContactsUpdateSuccess == true) {
            snackbarHostState.showSnackbar("Emergency contacts updated successfully!")
            authViewModel.clearEmergencyContactsUpdateState()
            authViewModel.refreshCurrentUser()
        }
    }
    LaunchedEffect(authState.emergencyContactsUpdateError) {
        authState.emergencyContactsUpdateError?.let { error ->
            snackbarHostState.showSnackbar("Error: $error")
            authViewModel.clearEmergencyContactsUpdateState()
        }
    }

    // Handle success/error states from emergency
    LaunchedEffect(emergencyUiState.successMessage) {
        emergencyUiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    LaunchedEffect(emergencyUiState.errorMessage) {
        emergencyUiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar("Error: $error")
        }
    }

    // Show call permission dialog
    if (showCallPermissionDialog && pendingCall != null) {
        CallPermissionDialog(
            phoneNumber = pendingCall!!,
            onRequestPermission = {
                emergencyViewModel.dismissCallPermissionDialog()
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            },
            onUseDial = {
                emergencyViewModel.onCallPermissionDenied()
            },
            onDismiss = {
                emergencyViewModel.dismissCallPermissionDialog()
            }
        )
    }

    val allValid = contact1Validation.isValid &&
            contact2Validation.isValid &&
            contact3Validation.isValid &&
            contact1.isNotEmpty()

    fun handleSaveContacts() {
        if (emergencyViewModel.validateAllContacts() && !authState.isUpdatingEmergencyContacts) {
            val contacts = listOfNotNull(
                contact1.takeIf { it.isNotBlank() }?.trim(),
                contact2.takeIf { it.isNotBlank() }?.trim(),
                contact3.takeIf { it.isNotBlank() }?.trim()
            )
            authViewModel.updateEmergencyContacts(contacts)
        }
    }

    fun handleDeleteContact(index: Int) {
        currentUser?.let { user ->
            val updatedContacts = user.emergencyContacts.toMutableList().apply {
                if (index < size) removeAt(index)
            }
            authViewModel.updateEmergencyContacts(updatedContacts)
        }
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

        InfoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                EmergencyHeader()
                currentUser?.emergencyContacts?.let { contacts ->
                    if (contacts.isNotEmpty()) {
                        EmergencyContactsList(
                            contacts = contacts,
                            onCallClick = { contact -> emergencyViewModel.makeEmergencyCall(contact) },
                            onDeleteClick = { index -> handleDeleteContact(index) }
                        )
                    }
                }
                EmergencyContactFields(
                    contact1 = contact1,
                    contact2 = contact2,
                    contact3 = contact3,
                    contact1Validation = contact1Validation,
                    contact2Validation = contact2Validation,
                    contact3Validation = contact3Validation,
                    onContact1Change = emergencyViewModel::updateContact1,
                    onContact2Change = emergencyViewModel::updateContact2,
                    onContact3Change = emergencyViewModel::updateContact3,
                    hasExistingContacts = currentUser?.emergencyContacts?.isNotEmpty() == true
                )
                EmergencyInfoSection()
                PrimaryButton(
                    text = when {
                        authState.isUpdatingEmergencyContacts -> "Upgrading..."
                        currentUser?.emergencyContacts?.isNotEmpty() == true -> "Update Emergency Contacts"
                        else -> "Save Emergency Contacts"
                    },
                    onClick = { handleSaveContacts() },
                    enabled = allValid && !authState.isUpdatingEmergencyContacts,
                    isLoading = authState.isUpdatingEmergencyContacts,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!allValid && contact1.isEmpty()) {
                    BodyText(
                        text = "Please enter at least one emergency contact",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (!allValid) {
                    BodyText(
                        text = "Please fix the validation errors above",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
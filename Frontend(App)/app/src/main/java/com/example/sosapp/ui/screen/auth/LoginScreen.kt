package com.example.sosapp.ui.screen.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sosapp.R
import com.example.sosapp.data.model.AuthMode
import com.example.sosapp.ui.components.auth.AuthTabSlider
import com.example.sosapp.ui.components.auth.LoginForm
import com.example.sosapp.ui.components.auth.RegisterForm
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.InfoCard
import com.example.sosapp.ui.viewmodel.AuthViewModel

/**
 * Displays the authentication screen, allowing users to log in or register.
 *
 * @param onLoginSuccess Callback invoked when login is successful.
 * @param onNavigateToOTP Callback invoked to navigate to the OTP screen after successful registration.
 * @param viewModel The [AuthViewModel] instance used for authentication logic.
 *
 * The screen shows either a login or registration form based on the current [AuthMode].
 * It handles error display, retry actions, and animated transitions between modes.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToOTP: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Collect state from ViewModel
    val uiState by viewModel.authUiState.collectAsStateWithLifecycle()

    // Handle side effects: navigate on successful login
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    // Navigate to OTP when registration is successful
    val otpState by viewModel.otpUiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isRegistering) {
        if (!uiState.isRegistering && uiState.authMode == AuthMode.REGISTER && uiState.errorMessage == null) {
            // Check if we just finished registering successfully
            if (otpState.email.isNotEmpty()) {
                onNavigateToOTP()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = maxOf(
                    24.dp,
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                ),
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animate card height based on auth mode
        val targetHeight = when (uiState.authMode) {
            AuthMode.LOGIN -> 375.dp // Height for login form
            AuthMode.REGISTER -> 700.dp // Height for registration form
        }
        val animatedHeight by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 400),
            label = "cardHeightAnimation"
        )

        // Background image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Main card containing login or registration form
        InfoCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .padding(
                    start = 24.dp,
                    end = 24.dp
                )
        ) {
            if (uiState.authMode == AuthMode.LOGIN) {
                // Login form UI
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        AuthTabSlider(
                            selectedTab = uiState.authMode,
                            onTabSelected = viewModel::setAuthMode
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Error message with retry for network errors
                    uiState.errorMessage?.let { error ->
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BodyText(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Show retry button for network errors
                                if (error.contains("network", ignoreCase = true) ||
                                    error.contains("timeout", ignoreCase = true) ||
                                    error.contains("connection", ignoreCase = true)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        TextButton(
                                            onClick = {
                                                viewModel.clearAuthError()
                                                viewModel.login()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            BodyText(
                                                text = "Retry",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        LoginForm(
                            email = uiState.loginEmail,
                            onEmailChange = viewModel::updateLoginEmail,
                            password = uiState.loginPassword,
                            onPasswordChange = viewModel::updateLoginPassword,
                            onLoginClick = viewModel::login,
                            isLoading = uiState.isLoading
                        )
                    }
                }
            } else {
                // Registration form UI
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AuthTabSlider(
                            selectedTab = uiState.authMode,
                            onTabSelected = viewModel::setAuthMode
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Error message with retry for network errors
                    uiState.errorMessage?.let { error ->
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BodyText(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Show retry button for network errors
                                if (error.contains("network", ignoreCase = true) ||
                                    error.contains("timeout", ignoreCase = true) ||
                                    error.contains("connection", ignoreCase = true)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        TextButton(
                                            onClick = {
                                                viewModel.clearAuthError()
                                                viewModel.register()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            BodyText(
                                                text = "Retry",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        RegisterForm(
                            fullName = uiState.registerFullName,
                            onFullNameChange = viewModel::updateRegisterFullName,
                            email = uiState.registerEmail,
                            onEmailChange = viewModel::updateRegisterEmail,
                            phone = uiState.registerPhone,
                            onPhoneChange = viewModel::updateRegisterPhone,
                            password = uiState.registerPassword,
                            onPasswordChange = viewModel::updateRegisterPassword,
                            emergencyContact1 = uiState.emergencyContact1,
                            onEmergencyContact1Change = viewModel::updateEmergencyContact1,
                            emergencyContact2 = uiState.emergencyContact2,
                            onEmergencyContact2Change = viewModel::updateEmergencyContact2,
                            emergencyContact3 = uiState.emergencyContact3,
                            onEmergencyContact3Change = viewModel::updateEmergencyContact3,
                            onRegisterClick = viewModel::register,
                            isRegistering = uiState.isRegistering
                        )
                    }
                }
            }
        }
    }
}
package com.example.sosapp.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sosapp.R
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.InfoCard
import com.example.sosapp.ui.components.common.OTPInputField
import com.example.sosapp.ui.components.common.PrimaryButton
import com.example.sosapp.ui.components.common.SecondaryButton
import com.example.sosapp.ui.components.common.SectionTitle
import com.example.sosapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun OTPVerificationScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onOTPVerified: () -> Unit = {},
    onBackToRegister: () -> Unit = {}
) {
    val otpState by viewModel.otpUiState.collectAsStateWithLifecycle()

    // Automatically navigate on successful OTP verification
    LaunchedEffect(otpState.isVerified) {
        if (otpState.isVerified) onOTPVerified()
    }

    // Resend timer countdown
    LaunchedEffect(otpState.resendTimer) {
        if (otpState.resendTimer > 0) {
            delay(1000)
            viewModel.updateResendTimer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = maxOf(24.dp, WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            ),
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionTitle(
                        text = "Verify Your Account",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BodyText(
                        text = "We've sent a 6-digit verification code to",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    BodyText(
                        text = otpState.email,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OTPInputField(
                    otpValues = otpState.otpValues,
                    onOTPChange = viewModel::updateOtpValue,
                    onBackspace = { index ->
                        val newValues = otpState.otpValues.toMutableList()
                        if (newValues[index].isNotEmpty()) {
                            newValues[index] = ""
                        } else if (index > 0) {
                            newValues[index - 1] = ""
                        }
                        newValues.forEachIndexed { i, v -> viewModel.updateOtpValue(i, v) }
                    },
                    hasError = otpState.errorMessage != null
                )

                otpState.errorMessage?.let { error ->
                    BodyText(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = if (otpState.isVerifying) "Verifying..." else "Verify OTP",
                        onClick = { viewModel.verifyOtp() },
                        enabled = !otpState.isVerifying && otpState.otpValues.joinToString("").length == 6,
                        isLoading = otpState.isVerifying,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SecondaryButton(
                        text = if (otpState.canResend) "Resend OTP" else "Resend in ${otpState.resendTimer}s",
                        onClick = { viewModel.resendOtp() },
                        enabled = otpState.canResend,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Back to Registration",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onBackToRegister() }
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )

                BodyText(
                    text = "• Check your spam folder if you don't see the email\n• Make sure the email address is correct\n• The code expires in 10 minutes",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

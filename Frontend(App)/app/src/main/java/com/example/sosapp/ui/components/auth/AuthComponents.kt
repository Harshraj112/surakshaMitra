package com.example.sosapp.ui.components.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sosapp.data.model.AuthMode
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.components.common.CustomTextField
import com.example.sosapp.ui.components.common.PrimaryButton
import com.example.sosapp.ui.components.common.SectionTitle
import com.example.sosapp.ui.components.common.ValidationState

@Composable
fun AuthTabSlider(
    selectedTab: AuthMode,
    onTabSelected: (AuthMode) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(25.dp)
            )
            .padding(4.dp)
    ) {
        val containerWidth = maxWidth
        val sliderWidth = (containerWidth - 4.dp) / 2

        val sliderOffset by animateDpAsState(
            targetValue = if (selectedTab == AuthMode.LOGIN) 0.dp else sliderWidth + 2.dp,
            animationSpec = tween(400),
            label = "slider_offset"
        )

        // Slider Background
        Card(
            modifier = Modifier
                .width(sliderWidth)
                .height(42.dp)
                .offset { IntOffset(sliderOffset.roundToPx(), 0) },
            shape = RoundedCornerShape(21.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {}

        // Tab Content
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable { onTabSelected(AuthMode.LOGIN) },
                contentAlignment = Alignment.Center
            ) {
                SectionTitle(
                    text = "LOGIN",
                    color = if (selectedTab == AuthMode.LOGIN)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable { onTabSelected(AuthMode.REGISTER) },
                contentAlignment = Alignment.Center
            ) {
                SectionTitle(
                    text = "REGISTER",
                    color = if (selectedTab == AuthMode.REGISTER)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Email address",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        CustomTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        PrimaryButton(
            text = if (isLoading) "Logging in..." else "Log In",
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        )
    }
}

@Composable
fun RegisterForm(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    emergencyContact1: String,
    onEmergencyContact1Change: (String) -> Unit,
    emergencyContact2: String,
    onEmergencyContact2Change: (String) -> Unit,
    emergencyContact3: String,
    onEmergencyContact3Change: (String) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRegistering: Boolean = false,
) {
    var emailValidation by remember { mutableStateOf(ValidationState(true)) }
    var passwordValidation by remember { mutableStateOf(ValidationState(true)) }

    fun validateEmail(emailInput: String): ValidationState {
        return when {
            emailInput.isEmpty() -> ValidationState(false, "Email is required")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches() ->
                ValidationState(false, "Please enter a valid email address")
            else -> ValidationState(true)
        }
    }

    fun validatePassword(passwordInput: String): ValidationState {
        return when {
            passwordInput.isEmpty() -> ValidationState(false, "Password is required")
            passwordInput.length < 6 -> ValidationState(false, "Password must be at least 6 characters")
            else -> ValidationState(true)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Personal Information Section
        SectionTitle(
            text = "Personal Information",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        CustomTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = "Full name",
            modifier = Modifier.fillMaxWidth()
        )

        CustomTextField(
            value = phone,
            onValueChange = onPhoneChange,
            placeholder = "Phone number",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CustomTextField(
                value = email,
                onValueChange = {
                    onEmailChange(it)
                    emailValidation = validateEmail(it)
                },
                placeholder = "Email address",
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )

            if (!emailValidation.isValid) {
                BodyText(
                    text = emailValidation.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CustomTextField(
                value = password,
                onValueChange = {
                    onPasswordChange(it)
                    passwordValidation = validatePassword(it)
                },
                placeholder = "Create password",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (!passwordValidation.isValid) {
                BodyText(
                    text = passwordValidation.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Emergency Contacts Section
        SectionTitle(
            text = "Emergency Contacts",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        BodyText(
            text = "Add up to 3 emergency contacts (at least 1 required)",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )

        CustomTextField(
            value = emergencyContact1,
            onValueChange = onEmergencyContact1Change,
            placeholder = "Emergency contact 1 (Required)",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )

        CustomTextField(
            value = emergencyContact2,
            onValueChange = onEmergencyContact2Change,
            placeholder = "Emergency contact 2 (Optional)",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )

        CustomTextField(
            value = emergencyContact3,
            onValueChange = onEmergencyContact3Change,
            placeholder = "Emergency contact 3 (Optional)",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        val isFormValid = fullName.isNotBlank() &&
                emailValidation.isValid &&
                email.isNotBlank() &&
                passwordValidation.isValid &&
                password.isNotBlank() &&
                emergencyContact1.isNotBlank()

        PrimaryButton(
            text = if (isRegistering) "Creating Account..." else "Create Account",
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid && !isRegistering
        )

        if (isRegistering) {
            BodyText(
                text = "Please wait while we create your account and send verification code...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
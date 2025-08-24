package com.example.sosapp.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class ValidationState(
    val isValid: Boolean,
    val errorMessage: String = ""
)

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(56.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = MaterialTheme.typography.bodyMedium.fontFamily
            )
        },
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun OTPInputField(
    otpValues: List<String>,
    onOTPChange: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        otpValues.forEachIndexed { index, value ->
            OTPDigitBox(
                value = value,
                onValueChange = { newValue ->
                    val isPaste = newValue.length == 6 && newValue.all { it.isDigit() }
                    if (isPaste) {
                        newValue.forEachIndexed { i, c -> onOTPChange(i, c.toString()) }
                    } else {
                        onOTPChange(index, newValue)
                        if (newValue.length == 1 && index < 5) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    }
                },
                onBackspace = {
                    onBackspace(index)
                    if (index > 0) {
                        focusRequesters[index - 1].requestFocus()
                    }
                },
                hasError = hasError,
                focusRequester = focusRequesters[index],
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OTPDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    hasError: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var lastValue by remember { mutableStateOf(value) }

    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            val backspacePressed = newValue.length < lastValue.length
            lastValue = newValue

            if (backspacePressed && newValue.isEmpty()) {
                onBackspace()
            } else {
                onValueChange(newValue)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .size(48.dp)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = when {
                            hasError -> MaterialTheme.colorScheme.error
                            value.isNotEmpty() -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}
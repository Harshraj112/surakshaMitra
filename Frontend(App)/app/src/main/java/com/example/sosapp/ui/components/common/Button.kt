package com.example.sosapp.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    fontSize: TextUnit = 16.sp,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = 16.sp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily
        )
    }
}

@Composable
fun EmergencyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: String? = null,
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(2.dp, Color.White),
        shape = RoundedCornerShape(30.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (number != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = MaterialTheme.typography.headlineSmall.fontFamily
                )
                Text(
                    text = number,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                )
            }
        } else {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = MaterialTheme.typography.headlineSmall.fontFamily
            )
        }
    }
}

@Composable
fun PanicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 200.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clickable { onClick() }
                .background(
                    brush = if (isActive) {
                        // Active state: Dramatic red gradient with pulsing effect
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF1744), // Bright red center
                                Color(0xFFD32F2F), // Medium red
                                Color(0xFF8B0000), // Dark red edge
                                Color(0xFF4A0000)  // Very dark red outer
                            ),
                            radius = size.value * 0.8f
                        )
                    } else {
                        // Inactive state: Gray gradient
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF616161), // Light gray center
                                Color(0xFF424242), // Medium gray
                                Color(0xFF212121), // Dark gray
                                Color(0xFF000000)  // Black edge
                            ),
                            radius = size.value * 0.8f
                        )
                    },
                    shape = CircleShape
                )
                .then(
                    if (isActive) {
                        Modifier.border(
                            width = 6.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White,
                                    Color.Yellow,
                                    Color.White,
                                    Color.Red,
                                    Color.White
                                )
                            ),
                            shape = CircleShape
                        )
                    } else {
                        Modifier.border(
                            width = 2.dp,
                            color = Color.Gray,
                            shape = CircleShape
                        )
                    }
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isActive) "ACTIVE" else "PANIC",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isActive) 20.sp else 24.sp,
                    fontFamily = MaterialTheme.typography.displaySmall.fontFamily
                )
                if (isActive) {
                    Text(
                        text = "TAP TO STOP",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
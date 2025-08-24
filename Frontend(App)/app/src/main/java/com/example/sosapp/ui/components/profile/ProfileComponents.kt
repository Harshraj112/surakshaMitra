package com.example.sosapp.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sosapp.ui.components.common.BodyText
import com.example.sosapp.ui.theme.EmergencyColors
import com.example.sosapp.ui.theme.MontserratFontFamily

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BodyText(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium
            )

            BodyText(
                text = value,
                color = valueColor,
                fontSize = 16.sp,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun EmergencyContactRow(
    contactNumber: Int,
    phoneNumber: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = EmergencyColors.SafeGreen,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BodyText(
                text = "Emergency Contact $contactNumber",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Medium
            )

            BodyText(
                text = phoneNumber,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

package com.example.sosapp.ui.components.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.sosapp.ui.theme.EmergencyTextStyles
import com.example.sosapp.ui.theme.MontserratFontFamily

@Composable
fun AppTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    style: TextStyle = EmergencyTextStyles.EmergencyTitle
) {
    Text(
        text = text,
        color = color,
        style = style.copy(fontFamily = MontserratFontFamily),
        modifier = modifier
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = 18.sp,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = MontserratFontFamily
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        textAlign = textAlign,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        modifier = modifier
    )
}

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 14.sp,
    textAlign: TextAlign = TextAlign.Start,
    fontFamily: FontFamily = MontserratFontFamily,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        textAlign = textAlign,
        modifier = modifier
    )
}
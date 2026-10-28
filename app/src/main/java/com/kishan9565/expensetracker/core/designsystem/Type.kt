package com.kishan9565.expensetracker.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using default for now, can be swapped out later with Poppins/Conthrax
val DisplayFont = FontFamily.Default
val Poppins = FontFamily.Default

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 48.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
)

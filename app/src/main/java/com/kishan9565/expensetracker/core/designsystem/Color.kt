package com.kishan9565.expensetracker.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val income: Color, val expense: Color, val warning: Color,
    val accentText: Color,
    val deepest: Color,
)
val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

val DarkColors = darkColorScheme(
    primary = Color(0xFF556EE6), onPrimary = Color.White,
    background = Color(0xFF161A27), onBackground = Color(0xFFE5E5E7),
    surface = Color(0xFF222736), onSurface = Color(0xFFE5E5E7),
    surfaceVariant = Color(0xFF2A3042), onSurfaceVariant = Color(0xFF96A0BE),
    outline = Color(0xFF343B52), error = Color(0xFFF46A6A), onError = Color(0xFF161A27),
    surfaceTint = Color.Transparent,
)
val LightColors = lightColorScheme(
    primary = Color(0xFF4A5FD0), onPrimary = Color.White,
    background = Color(0xFFF4F6FB), onBackground = Color(0xFF161A27),
    surface = Color.White, onSurface = Color(0xFF161A27),
    surfaceVariant = Color(0xFFEBEEF6), onSurfaceVariant = Color(0xFF5B6580),
    outline = Color(0xFFDCE1EE), error = Color(0xFFC73E3E),
    surfaceTint = Color.Transparent,
)
val DarkFinance = FinanceColors(
    income = Color(0xFF28BE87), expense = Color(0xFFF46A6A), warning = Color(0xFFE3A94B),
    accentText = Color(0xFF8396F0), deepest = Color(0xFF090D29),
)
val LightFinance = FinanceColors(
    income = Color(0xFF17805B), expense = Color(0xFFC73E3E), warning = Color(0xFF8F5B00),
    accentText = Color(0xFF4A5FD0), deepest = Color(0xFF161A27),
)

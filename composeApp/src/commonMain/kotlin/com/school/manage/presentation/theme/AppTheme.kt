package com.school.manage.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class SchoolThemeColors(
    val isDark: Boolean,
    val bgApp: Color,
    val bgCard: Color,
    val bgCardHover: Color,
    val borderCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val brandPrimary: Color,
    val brandAccent: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val inputBg: Color,
    val inputBorder: Color
)

val LightPalette = SchoolThemeColors(
    isDark = false,
    bgApp = Color(0xFFF4F7FB),
    bgCard = Color(0xFFFFFFFF),
    bgCardHover = Color(0xFFF8FAFC),
    borderCard = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    brandPrimary = Color(0xFF0D529C),
    brandAccent = Color(0xFF2563EB),
    success = Color(0xFF16A34A),
    error = Color(0xFFDC2626),
    warning = Color(0xFFD97706),
    inputBg = Color(0xFFFFFFFF),
    inputBorder = Color(0xFFCBD5E1)
)

val DarkPalette = SchoolThemeColors(
    isDark = true,
    bgApp = Color(0xFF0B1120),
    bgCard = Color(0xFF1E293B),
    bgCardHover = Color(0xFF334155),
    borderCard = Color(0xFF334155),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    brandPrimary = Color(0xFF38BDF8),
    brandAccent = Color(0xFF60A5FA),
    success = Color(0xFF22C55E),
    error = Color(0xFFEF4444),
    warning = Color(0xFFF59E0B),
    inputBg = Color(0xFF1E293B),
    inputBorder = Color(0xFF475569)
)

val LocalSchoolColors = staticCompositionLocalOf { LightPalette }
val LocalThemeToggle = staticCompositionLocalOf { {} }

@Composable
fun SchoolAppTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    onToggleTheme: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val colors = if (isDark) DarkPalette else LightPalette

    val m3Colors = if (isDark) {
        darkColorScheme(
            primary = colors.brandPrimary,
            secondary = colors.brandAccent,
            background = colors.bgApp,
            surface = colors.bgCard,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = colors.brandPrimary,
            secondary = colors.brandAccent,
            background = colors.bgApp,
            surface = colors.bgCard,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    CompositionLocalProvider(
        LocalSchoolColors provides colors,
        LocalThemeToggle provides onToggleTheme
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )
    }
}

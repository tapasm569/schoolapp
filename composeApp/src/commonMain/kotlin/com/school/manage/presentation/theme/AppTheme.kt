package com.school.manage.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

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
    bgApp = Color(0xFFF5F7FC),
    bgCard = Color(0xFFFFFFFF),
    bgCardHover = Color(0xFFF0F3FA),
    borderCard = Color(0xFFE3E8F2),
    textPrimary = Color(0xFF172033),
    textSecondary = Color(0xFF667085),
    brandPrimary = Color(0xFF4338CA),
    brandAccent = Color(0xFF0D9488),
    success = Color(0xFF15803D),
    error = Color(0xFFB42318),
    warning = Color(0xFFB45309),
    inputBg = Color(0xFFFFFFFF),
    inputBorder = Color(0xFFD0D5DD)
)

val DarkPalette = SchoolThemeColors(
    isDark = true,
    bgApp = Color(0xFF101522),
    bgCard = Color(0xFF191F2E),
    bgCardHover = Color(0xFF252D40),
    borderCard = Color(0xFF343D52),
    textPrimary = Color(0xFFF2F4F8),
    textSecondary = Color(0xFFADB7CA),
    brandPrimary = Color(0xFFA5B4FC),
    brandAccent = Color(0xFF5EEAD4),
    success = Color(0xFF4ADE80),
    error = Color(0xFFFF8A80),
    warning = Color(0xFFFBBF24),
    inputBg = Color(0xFF20283A),
    inputBorder = Color(0xFF48536B)
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
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
                extraLarge = RoundedCornerShape(28.dp)
            ),
            content = content
        )
    }
}

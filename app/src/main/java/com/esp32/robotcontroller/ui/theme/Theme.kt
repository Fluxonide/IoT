package com.esp32.robotcontroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = SurfaceDark,
    secondary = PrimaryLight,
    tertiary = AccentGreen,
    background = SurfaceDark,
    surface = SurfaceVariant,
    surfaceVariant = SurfaceCard,
    error = AccentRed,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF9E9E9E)
)

@Composable
fun ESP32RobotControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}

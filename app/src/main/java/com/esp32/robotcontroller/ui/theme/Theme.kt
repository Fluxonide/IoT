package com.esp32.robotcontroller.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF422B00),
    primaryContainer = AmberDim,
    onPrimaryContainer = Color(0xFFFFDDB8),
    inversePrimary = Color(0xFF825500),
    secondary = Cyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = Green,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = GreenDim,
    onTertiaryContainer = Color(0xFF8CFDB2),
    error = Red,
    onError = Color(0xFF690005),
    errorContainer = RedDim,
    onErrorContainer = Color(0xFFFFDAD6),
    background = BgDark,
    onBackground = TextMain,
    surface = BgDark,
    onSurface = TextMain,
    surfaceVariant = PanelDark,
    onSurfaceVariant = TextDim,
    surfaceContainer = PanelDark,
    surfaceContainerLow = BgRaised,
    surfaceContainerHigh = PanelDark2,
    outline = BorderLine,
    outlineVariant = BorderLineSoft
)

@Composable
fun ESP32RobotControllerTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

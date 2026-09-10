package com.esp32.robotcontroller.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun DirectionalPad(
    onDirectionPress: (String) -> Unit,
    onDirectionRelease: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 64.dp,
    spacing: Dp = 8.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
    ) {
        // Top row: [ ] [▲ Forward] [ ]
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(buttonSize))
            DpadButton(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Forward",
                size = buttonSize,
                onPress = { onDirectionPress("F") },
                onRelease = onDirectionRelease
            )
            Box(modifier = Modifier.size(buttonSize))
        }

        // Middle row: [◄ Left] [STOP] [Right ►]
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DpadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Left",
                size = buttonSize,
                onPress = { onDirectionPress("L") },
                onRelease = onDirectionRelease
            )

            // Center STOP Button (matching HTML .stop-button)
            DpadStopButton(
                size = buttonSize,
                onStop = { onDirectionPress("S") }
            )

            DpadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                size = buttonSize,
                onPress = { onDirectionPress("R") },
                onRelease = onDirectionRelease
            )
        }

        // Bottom row: [ ] [▼ Backward] [ ]
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(buttonSize))
            DpadButton(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Backward",
                size = buttonSize,
                onPress = { onDirectionPress("B") },
                onRelease = onDirectionRelease
            )
            Box(modifier = Modifier.size(buttonSize))
        }
    }
}

@Composable
private fun DpadButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val isPressed = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val normalBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val pressedBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val borderColor = if (isPressed.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(if (isPressed.value) pressedBg else normalBg)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    isPressed.value = true

                    // Haptic feedback
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(VibratorManager::class.java)
                            vibratorManager?.defaultVibrator?.vibrate(
                                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = context.getSystemService(Vibrator::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(
                                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                            }
                        }
                    } catch (_: Exception) { }

                    onPress()

                    // Keep sending command while pressed every 150ms
                    var lastCommandTime = System.currentTimeMillis()
                    while (true) {
                        val up = withTimeoutOrNull(50) {
                            waitForUpOrCancellation()
                        }

                        if (up != null) {
                            up.consume()
                            break
                        } else {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastCommandTime >= 150) {
                                onPress()
                                lastCommandTime = currentTime
                            }
                        }
                    }

                    isPressed.value = false
                    onRelease()
                }
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
private fun DpadStopButton(
    size: Dp,
    onStop: () -> Unit
) {
    val isPressed = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val normalBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
    val pressedBg = MaterialTheme.colorScheme.errorContainer
    val borderColor = if (isPressed.value) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(if (isPressed.value) pressedBg else normalBg)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed.value = true

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(VibratorManager::class.java)
                            vibratorManager?.defaultVibrator?.vibrate(
                                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = context.getSystemService(Vibrator::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(
                                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                            }
                        }
                    } catch (_: Exception) { }

                    onStop()

                    val up = waitForUpOrCancellation()
                    up?.consume()
                    isPressed.value = false
                    onStop()
                }
            }
    ) {
        Text(
            text = "STOP",
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
    }
}


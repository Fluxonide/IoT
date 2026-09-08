package com.esp32.robotcontroller.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.esp32.robotcontroller.ui.theme.DpadButton
import com.esp32.robotcontroller.ui.theme.DpadButtonPressed
import com.esp32.robotcontroller.ui.theme.EmergencyRed
import com.esp32.robotcontroller.ui.theme.SurfaceDark
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun DirectionalPad(
    onDirectionPress: (String) -> Unit,
    onDirectionRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 72.dp
    val spacing = 6.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
    ) {
        // Forward
        DpadButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Forward",
            size = buttonSize,
            onPress = { onDirectionPress("F") },
            onRelease = onDirectionRelease
        )

        // Left - Center Stop - Right
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

            // Center Stop Button (■) matching test2.html
            DpadButton(
                icon = Icons.Filled.Stop,
                contentDescription = "Stop",
                size = buttonSize,
                customColor = Color(0xFF9D3038),
                customPressedColor = EmergencyRed,
                onPress = { onDirectionPress("S") },
                onRelease = onDirectionRelease
            )

            DpadButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                size = buttonSize,
                onPress = { onDirectionPress("R") },
                onRelease = onDirectionRelease
            )
        }

        // Backward
        DpadButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Backward",
            size = buttonSize,
            onPress = { onDirectionPress("B") },
            onRelease = onDirectionRelease
        )
    }
}

@Composable
private fun DpadButton(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    customColor: Color? = null,
    customPressedColor: Color? = null,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val isPressed = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val normalBg = customColor ?: DpadButton
    val pressedBg = customPressedColor ?: DpadButtonPressed

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
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

                    // Keep sending command while pressed every 150ms (same as test2.html)
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
            tint = if (isPressed.value && customPressedColor == null) SurfaceDark else Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

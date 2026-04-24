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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.esp32.robotcontroller.ui.theme.DpadButton
import com.esp32.robotcontroller.ui.theme.DpadButtonPressed
import com.esp32.robotcontroller.ui.theme.SurfaceDark

@Composable
fun DirectionalPad(
    onDirectionPress: (String) -> Unit,
    onDirectionRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 80.dp
    val spacing = 4.dp

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

        // Left - Center - Right
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

            // Center spacer (empty square)
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark.copy(alpha = 0.5f))
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
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val isPressed = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed.value) DpadButtonPressed else DpadButton)
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
                    
                    // Keep sending command while pressed
                    var lastCommandTime = System.currentTimeMillis()
                    while (true) {
                        val up = withTimeoutOrNull(50) {
                            waitForUpOrCancellation()
                        }
                        
                        if (up != null) {
                            up.consume()
                            break
                        } else {
                            // Still pressed, send command again
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastCommandTime >= 100) {
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
            tint = if (isPressed.value) SurfaceDark else androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

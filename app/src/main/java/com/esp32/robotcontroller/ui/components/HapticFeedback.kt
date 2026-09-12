package com.esp32.robotcontroller.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object HapticFeedback {
    fun vibrate(context: Context, durationMs: Long = 30) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Vibrator::class.java)
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) { }
    }
}

class SliderHapticTracker(
    private val context: Context,
    private val presets: List<Int>,
    private val resetDistance: Float = 4f
) {
    private var lastValue: Float? = null
    private var lastTriggeredPreset: Int? = null

    fun onValueChange(newValue: Float) {
        val prev = lastValue
        lastValue = newValue

        // Reset triggered preset if slider has moved sufficiently far from it
        lastTriggeredPreset?.let { triggered ->
            if (kotlin.math.abs(newValue - triggered) >= resetDistance) {
                lastTriggeredPreset = null
            }
        }

        if (prev == null) {
            val initialPreset = presets.firstOrNull { kotlin.math.abs(newValue - it) <= 1f }
            if (initialPreset != null) {
                lastTriggeredPreset = initialPreset
            }
            return
        }

        val minVal = minOf(prev, newValue)
        val maxVal = maxOf(prev, newValue)

        // Find preset reached or crossed during this sliding movement
        val hitPreset = if (newValue >= prev) {
            presets.firstOrNull { it.toFloat() in minVal..maxVal || kotlin.math.abs(newValue - it) <= 0.6f }
        } else {
            presets.lastOrNull { it.toFloat() in minVal..maxVal || kotlin.math.abs(newValue - it) <= 0.6f }
        }

        if (hitPreset != null && hitPreset != lastTriggeredPreset) {
            HapticFeedback.vibrate(context)
            lastTriggeredPreset = hitPreset
        }
    }

    fun triggerManualPreset(preset: Int) {
        lastTriggeredPreset = if (presets.contains(preset)) preset else null
        lastValue = preset.toFloat()
        HapticFeedback.vibrate(context)
    }

    fun triggerStep(newValue: Int) {
        lastTriggeredPreset = if (presets.contains(newValue)) newValue else null
        lastValue = newValue.toFloat()
        HapticFeedback.vibrate(context)
    }
}

@Composable
fun rememberSliderHapticTracker(
    presets: List<Int>,
    resetDistance: Float = 4f
): SliderHapticTracker {
    val context = LocalContext.current
    return remember(context, presets, resetDistance) {
        SliderHapticTracker(context, presets, resetDistance)
    }
}

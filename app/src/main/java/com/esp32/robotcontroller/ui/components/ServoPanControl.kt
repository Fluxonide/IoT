package com.esp32.robotcontroller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServoPanControl(
    servoAngle: Int,
    onAngleChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onServoLeft: (() -> Unit)? = null,
    onServoCenter: (() -> Unit)? = null,
    onServoRight: (() -> Unit)? = null,
    onAngleChangeFinished: ((Int) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            val servoPresets = remember { listOf(20, 90, 160) }
            val servoHapticTracker = rememberSliderHapticTracker(presets = servoPresets, resetDistance = 3f)
            var localServoAngle by remember(servoAngle) { mutableFloatStateOf(servoAngle.toFloat()) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAMERA SERVO",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${localServoAngle.toInt()}°",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quick preset buttons matching HTML: ◄ Left (20), ● Center (90), Right ► (160)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ServoPresetButton(
                    label = "◄ Left",
                    isSelected = servoAngle == 20,
                    onClick = {
                        servoHapticTracker.triggerManualPreset(20)
                        onServoLeft?.invoke() ?: onAngleChange(20)
                    },
                    modifier = Modifier.weight(1f)
                )
                ServoPresetButton(
                    label = "● Center",
                    isSelected = servoAngle == 90,
                    onClick = {
                        servoHapticTracker.triggerManualPreset(90)
                        onServoCenter?.invoke() ?: onAngleChange(90)
                    },
                    modifier = Modifier.weight(1f)
                )
                ServoPresetButton(
                    label = "Right ►",
                    isSelected = servoAngle == 160,
                    onClick = {
                        servoHapticTracker.triggerManualPreset(160)
                        onServoRight?.invoke() ?: onAngleChange(160)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Slider(
                value = localServoAngle,
                onValueChange = {
                    localServoAngle = it
                    servoHapticTracker.onValueChange(it)
                    onAngleChange(it.toInt())
                },
                onValueChangeFinished = {
                    onAngleChangeFinished?.invoke(localServoAngle.toInt())
                },
                valueRange = 20f..160f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
private fun ServoPresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val border = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}


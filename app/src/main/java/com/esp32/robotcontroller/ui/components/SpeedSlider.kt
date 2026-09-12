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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.remember

@Composable
fun SpeedSlider(
    currentSpeed: Int,
    onSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val speedPresets = remember { listOf(100, 180, 255) }
    val hapticTracker = rememberSliderHapticTracker(presets = speedPresets, resetDistance = 4f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MOTOR SPEED",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$currentSpeed / 255",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick gear presets: Slow (100), Med (180), Max (255)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpeedPresetChip(
                    label = "SLOW (100)",
                    isSelected = currentSpeed == 100,
                    onClick = {
                        hapticTracker.triggerManualPreset(100)
                        onSpeedChange(100)
                    },
                    modifier = Modifier.weight(1f)
                )
                SpeedPresetChip(
                    label = "MED (180)",
                    isSelected = currentSpeed == 180,
                    onClick = {
                        hapticTracker.triggerManualPreset(180)
                        onSpeedChange(180)
                    },
                    modifier = Modifier.weight(1f)
                )
                SpeedPresetChip(
                    label = "MAX (255)",
                    isSelected = currentSpeed == 255,
                    onClick = {
                        hapticTracker.triggerManualPreset(255)
                        onSpeedChange(255)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = currentSpeed.toFloat(),
                onValueChange = {
                    hapticTracker.onValueChange(it)
                    onSpeedChange(it.toInt())
                },
                valueRange = 0f..255f,
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
private fun SpeedPresetChip(
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
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}


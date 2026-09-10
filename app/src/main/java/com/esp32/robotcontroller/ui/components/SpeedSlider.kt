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
import com.esp32.robotcontroller.ui.theme.Amber
import com.esp32.robotcontroller.ui.theme.BorderLine
import com.esp32.robotcontroller.ui.theme.BorderLineSoft
import com.esp32.robotcontroller.ui.theme.PanelDark
import com.esp32.robotcontroller.ui.theme.PanelDark2
import com.esp32.robotcontroller.ui.theme.TextDim
import com.esp32.robotcontroller.ui.theme.TextMain

@Composable
fun SpeedSlider(
    currentSpeed: Int,
    onSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, BorderLine, RoundedCornerShape(4.dp))
            .background(PanelDark)
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
                    color = TextDim,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$currentSpeed / 255",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Amber
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
                    onClick = { onSpeedChange(100) },
                    modifier = Modifier.weight(1f)
                )
                SpeedPresetChip(
                    label = "MED (180)",
                    isSelected = currentSpeed == 180,
                    onClick = { onSpeedChange(180) },
                    modifier = Modifier.weight(1f)
                )
                SpeedPresetChip(
                    label = "MAX (255)",
                    isSelected = currentSpeed == 255,
                    onClick = { onSpeedChange(255) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = currentSpeed.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 0f..255f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Amber,
                    activeTrackColor = Amber,
                    inactiveTrackColor = BorderLineSoft
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
    val bg = if (isSelected) Amber.copy(alpha = 0.15f) else PanelDark2
    val border = if (isSelected) Amber else BorderLine
    val textColor = if (isSelected) Amber else TextMain

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


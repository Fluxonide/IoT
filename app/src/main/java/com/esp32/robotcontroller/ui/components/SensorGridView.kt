package com.esp32.robotcontroller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.model.SensorData
import com.esp32.robotcontroller.ui.theme.Amber
import com.esp32.robotcontroller.ui.theme.BorderLine
import com.esp32.robotcontroller.ui.theme.BorderLineSoft
import com.esp32.robotcontroller.ui.theme.PanelDark
import com.esp32.robotcontroller.ui.theme.PanelDark2
import com.esp32.robotcontroller.ui.theme.TextDim
import com.esp32.robotcontroller.ui.theme.TextFaint
import com.esp32.robotcontroller.ui.theme.TextMain
import java.util.Locale

@Composable
fun SensorGridView(
    sensorData: SensorData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, BorderLine, RoundedCornerShape(3.dp))
            .background(PanelDark)
            .padding(14.dp)
    ) {
        // Card Head: 04 Live sensor data
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "04",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = Amber
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = "Live sensor data",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMain
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 1: DISTANCE & TEMPERATURE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorBox(
                name = "DISTANCE",
                value = String.format(Locale.US, "%.1f", sensorData.distance),
                unit = "cm",
                modifier = Modifier.weight(1f)
            )
            SensorBox(
                name = "TEMPERATURE",
                value = String.format(Locale.US, "%.1f", sensorData.temperature),
                unit = "°C",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: HUMIDITY & MQ SENSOR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorBox(
                name = "HUMIDITY",
                value = String.format(Locale.US, "%.1f", sensorData.humidity),
                unit = "%",
                modifier = Modifier.weight(1f)
            )
            SensorBox(
                name = "MQ SENSOR",
                value = String.format(Locale.US, "%.0f", sensorData.mq),
                unit = null,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: WATER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorBox(
                name = "WATER",
                value = String.format(Locale.US, "%.0f", sensorData.water),
                unit = null,
                modifier = Modifier.weight(1f)
            )
            // Empty spacer for 2-column symmetry or secondary info
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, BorderLineSoft, RoundedCornerShape(3.dp))
                    .background(PanelDark2)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LAST UPDATE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextFaint,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(sensorData.timestamp % 100000) / 1000}s",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextDim
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorBox(
    name: String,
    value: String,
    unit: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, BorderLineSoft, RoundedCornerShape(3.dp))
            .background(PanelDark2)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextFaint,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                    Text(
                        text = unit,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextFaint
                    )
                }
            }
        }
    }
}


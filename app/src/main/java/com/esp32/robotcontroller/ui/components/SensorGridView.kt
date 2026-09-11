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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.model.PredictionState
import com.esp32.robotcontroller.model.SensorData
import com.esp32.robotcontroller.model.TrendDirection
import com.esp32.robotcontroller.ui.theme.Amber
import com.esp32.robotcontroller.ui.theme.Cyan
import com.esp32.robotcontroller.ui.theme.Green
import com.esp32.robotcontroller.ui.theme.TextDim
import java.util.Locale

@Composable
fun SensorGridView(
    sensorData: SensorData,
    predictions: Map<String, PredictionState> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp)
    ) {
        // Card Head: 04 Live sensor data & AI Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "04",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "Live sensor data & AI Prediction",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // AI Status Pill
            Text(
                text = "ML LINEAR REGRESSION",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Amber.copy(alpha = 0.8f)
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
                liveValue = String.format(Locale.US, "%.1f", sensorData.distance),
                unit = "cm",
                predictionState = predictions["distance"],
                modifier = Modifier.weight(1f)
            )
            SensorBox(
                name = "TEMPERATURE",
                liveValue = String.format(Locale.US, "%.1f", sensorData.temperature),
                unit = "°C",
                predictionState = predictions["temperature"],
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: HUMIDITY & MQ GAS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorBox(
                name = "HUMIDITY",
                liveValue = String.format(Locale.US, "%.1f", sensorData.humidity),
                unit = "%",
                predictionState = predictions["humidity"],
                modifier = Modifier.weight(1f)
            )
            SensorBox(
                name = "MQ GAS SENSOR",
                liveValue = String.format(Locale.US, "%.0f", sensorData.mq),
                unit = null,
                predictionState = predictions["mq"],
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: WATER SENSOR & TELEMETRY CLOCK
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SensorBox(
                name = "WATER SENSOR",
                liveValue = String.format(Locale.US, "%.0f", sensorData.water),
                unit = null,
                predictionState = predictions["water"],
                modifier = Modifier.weight(1f)
            )
            // Telemetry heartbeat card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LAST UPDATE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(sensorData.timestamp % 100000) / 1000}s",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BUFFER: 30 SAMPLES",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Green.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorBox(
    name: String,
    liveValue: String,
    unit: String?,
    predictionState: PredictionState?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp)
    ) {
        // Sensor Name Header
        Text(
            text = name,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 1. LIVE VALUE SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = liveValue,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 0.5.dp
        )
        Spacer(modifier = Modifier.height(6.dp))

        // 2. AI PREDICTED NEXT VALUE SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI PREDICT",
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Amber.copy(alpha = 0.9f)
            )

            when (predictionState) {
                is PredictionState.Ready -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (unit == null) {
                                String.format(Locale.US, "%.0f", predictionState.nextValue)
                            } else {
                                String.format(Locale.US, "%.1f", predictionState.nextValue)
                            },
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Amber
                        )
                        if (unit != null) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = unit,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Amber.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                is PredictionState.CollectingData -> {
                    Text(
                        text = "Collecting data...",
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                is PredictionState.Error -> {
                    Text(
                        text = "Unavailable",
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
                null -> {
                    Text(
                        text = "--",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. TREND SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TREND",
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            if (predictionState is PredictionState.Ready) {
                val trendColor = when (predictionState.trend) {
                    TrendDirection.INCREASING -> Green
                    TrendDirection.DECREASING -> Cyan
                    TrendDirection.STABLE -> TextDim
                    TrendDirection.UNKNOWN -> TextDim
                }
                Text(
                    text = "${predictionState.trend.symbol} ${predictionState.trend.label}",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = trendColor
                )
            } else {
                Text(
                    text = "--",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

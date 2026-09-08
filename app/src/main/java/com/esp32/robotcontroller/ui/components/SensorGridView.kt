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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.model.SensorData
import java.util.Locale

@Composable
fun SensorGridView(
    sensorData: SensorData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section: Live Sensor Data
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF111A23))
                .border(1.dp, Color(0xFF263442), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "Live Sensor Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of sensor boxes (2 columns on mobile)
            val row1 = listOf(
                "Distance" to String.format(Locale.US, "%.1f cm", sensorData.distance),
                "Temperature" to String.format(Locale.US, "%.1f °C", sensorData.temperature)
            )
            val row2 = listOf(
                "Humidity" to String.format(Locale.US, "%.1f %%", sensorData.humidity),
                "MQ Sensor" to String.format(Locale.US, "%.0f", sensorData.mq)
            )
            val row3 = listOf(
                "Water" to String.format(Locale.US, "%.0f", sensorData.water),
                "Accel Mag" to String.format(Locale.US, "%.2f", sensorData.accelMagnitude)
            )

            SensorRow(row1)
            Spacer(modifier = Modifier.height(8.dp))
            SensorRow(row2)
            Spacer(modifier = Modifier.height(8.dp))
            SensorRow(row3)
            Spacer(modifier = Modifier.height(8.dp))
            SensorRow(listOf(
                "Gyro Mag" to String.format(Locale.US, "%.2f", sensorData.gyroMagnitude),
                "Timestamp" to "${(sensorData.timestamp % 100000) / 1000}s"
            ))
        }

        // Section: MPU6050 Raw Values
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF111A23))
                .border(1.dp, Color(0xFF263442), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "MPU6050 Raw Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Accel X, Y, Z
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorBox(label = "Accel X", value = String.format(Locale.US, "%.2f", sensorData.ax), modifier = Modifier.weight(1f))
                SensorBox(label = "Accel Y", value = String.format(Locale.US, "%.2f", sensorData.ay), modifier = Modifier.weight(1f))
                SensorBox(label = "Accel Z", value = String.format(Locale.US, "%.2f", sensorData.az), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gyro X, Y, Z
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorBox(label = "Gyro X", value = String.format(Locale.US, "%.2f", sensorData.gx), modifier = Modifier.weight(1f))
                SensorBox(label = "Gyro Y", value = String.format(Locale.US, "%.2f", sensorData.gy), modifier = Modifier.weight(1f))
                SensorBox(label = "Gyro Z", value = String.format(Locale.US, "%.2f", sensorData.gz), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SensorRow(items: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((label, value) in items) {
            SensorBox(label = label, value = value, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SensorBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C141C))
            .border(1.dp, Color(0xFF263442), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF8497AA)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

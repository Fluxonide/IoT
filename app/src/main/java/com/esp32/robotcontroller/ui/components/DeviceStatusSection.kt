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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.model.DeviceStatus

@Composable
fun DeviceStatusSection(
    isMotorOnline: Boolean,
    motorStatus: DeviceStatus,
    isSensorOnline: Boolean,
    sensorStatus: DeviceStatus,
    isCameraOnline: Boolean,
    cameraStatus: DeviceStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Device Status",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Motor ESP32 Card
        DeviceCard(
            title = "Motor ESP32",
            isOnline = isMotorOnline,
            lines = listOf(
                "IP" to motorStatus.ip,
                "RSSI" to motorStatus.rssi,
                "Channel" to motorStatus.channel
            )
        )

        // Sensor ESP32 Card
        DeviceCard(
            title = "Sensor ESP32",
            isOnline = isSensorOnline,
            lines = listOf(
                "IP" to sensorStatus.ip,
                "RSSI" to sensorStatus.rssi,
                "Channel" to sensorStatus.channel
            )
        )

        // Camera ESP32 Card
        DeviceCard(
            title = "ESP32-CAM",
            isOnline = isCameraOnline,
            lines = listOf(
                "IP" to cameraStatus.ip,
                "Servo" to cameraStatus.servo,
                "RSSI" to cameraStatus.rssi
            )
        )
    }
}

@Composable
fun DeviceCard(
    title: String,
    isOnline: Boolean,
    lines: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C141C))
            .border(1.dp, Color(0xFF263442), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Title Row with indicator dot
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF55E984) else Color(0xFFFF4D5D))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isOnline) "ONLINE" else "OFFLINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) Color(0xFF55E984) else Color(0xFFFF4D5D)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info lines
        for ((k, v) in lines) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = k,
                    fontSize = 13.sp,
                    color = Color(0xFF92A4B5)
                )
                Text(
                    text = v,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE8EEF5)
                )
            }
        }
    }
}

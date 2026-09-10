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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.model.DeviceStatus
import com.esp32.robotcontroller.ui.theme.Green
import com.esp32.robotcontroller.ui.theme.GreenDim
import com.esp32.robotcontroller.ui.theme.Red
import com.esp32.robotcontroller.ui.theme.RedDim

@Composable
fun DeviceStatusSection(
    isMotorOnline: Boolean,
    motorStatus: DeviceStatus,
    isCameraOnline: Boolean,
    cameraStatus: DeviceStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Motor node card (Card 05)
        NodeCard(
            idx = "05",
            title = "Motor node",
            isOnline = isMotorOnline,
            lines = listOf(
                "Device" to motorStatus.device,
                "IP address" to motorStatus.ip,
                "Gateway" to motorStatus.gateway,
                "Wi-Fi SSID" to motorStatus.ssid,
                "RSSI" to motorStatus.rssi,
                "Channel" to motorStatus.channel,
                "Uptime" to motorStatus.uptime,
                "Motor speed" to motorStatus.motorSpeed
            )
        )

        // Camera node card (Card 06)
        NodeCard(
            idx = "06",
            title = "Camera node",
            isOnline = isCameraOnline,
            lines = listOf(
                "Device" to cameraStatus.device,
                "IP address" to cameraStatus.ip,
                "Gateway" to cameraStatus.gateway,
                "Wi-Fi SSID" to cameraStatus.ssid,
                "RSSI" to cameraStatus.rssi,
                "Servo angle" to cameraStatus.servo
            )
        )
    }
}

// Backwards compatibility overload
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
    DeviceStatusSection(
        isMotorOnline = isMotorOnline,
        motorStatus = motorStatus,
        isCameraOnline = isCameraOnline,
        cameraStatus = cameraStatus,
        modifier = modifier
    )
}

@Composable
private fun NodeCard(
    idx: String,
    title: String,
    isOnline: Boolean,
    lines: List<Pair<String, String>>,
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
        // Card Head with Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = idx,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Online/Offline Pill matching HTML
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        if (isOnline) GreenDim else MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(20.dp)
                    )
                    .background(if (isOnline) Color(0x106FDC8C) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isOnline) "online" else "offline",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOnline) Green else MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info table
        for ((k, v) in lines) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = k,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = v,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


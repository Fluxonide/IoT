package com.esp32.robotcontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.ui.components.ConnectionIndicator
import com.esp32.robotcontroller.ui.components.DirectionalPad
import com.esp32.robotcontroller.ui.components.EmergencyStopButton
import com.esp32.robotcontroller.ui.components.MjpegView
import com.esp32.robotcontroller.ui.components.SpeedSlider
import com.esp32.robotcontroller.ui.theme.Primary
import com.esp32.robotcontroller.ui.theme.SurfaceCard
import com.esp32.robotcontroller.viewmodel.RobotViewModel

@Composable
fun ControlScreen(viewModel: RobotViewModel) {
    val isConnected by viewModel.isConnected.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val cameraFrame by viewModel.cameraFrame.collectAsState()
    val currentDirection by viewModel.currentDirection.collectAsState()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsState()
    val isStreamActive by viewModel.isStreamActive.collectAsState()

    // Start camera stream when screen is shown
    LaunchedEffect(Unit) {
        viewModel.startCameraStream()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // === Top Bar: Connection + Telemetry ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionIndicator(isConnected = isConnected)

            // Telemetry info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryChip(label = "DIR", value = currentDirection)
                TelemetryChip(label = "SPD", value = "$currentSpeed")
            }
        }

        // === Camera Feed ===
        MjpegView(
            frame = cameraFrame,
            isStreamActive = isStreamActive,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // === Speed Slider ===
        SpeedSlider(
            currentSpeed = currentSpeed,
            onSpeedChange = { viewModel.updateSpeed(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // === Controls: Emergency Stop + D-Pad ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emergency Stop on the left
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmergencyStopButton(
                    onEmergencyStop = { viewModel.emergencyStop() }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "E-STOP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Uptime
                Text(
                    text = formatUptime(uptimeSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // D-Pad on the right
            DirectionalPad(
                onDirectionPress = { viewModel.sendDirection(it) },
                onDirectionRelease = { viewModel.onDirectionRelease() }
            )
        }
    }
}

@Composable
private fun TelemetryChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

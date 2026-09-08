package com.esp32.robotcontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.ui.components.AircraftAttitudeCard
import com.esp32.robotcontroller.ui.components.ConnectionDialog
import com.esp32.robotcontroller.ui.components.DeviceStatusSection
import com.esp32.robotcontroller.ui.components.DirectionalPad
import com.esp32.robotcontroller.ui.components.EmergencyStopButton
import com.esp32.robotcontroller.ui.components.GraphDataset
import com.esp32.robotcontroller.ui.components.MjpegView
import com.esp32.robotcontroller.ui.components.SensorGraphCard
import com.esp32.robotcontroller.ui.components.SensorGridView
import com.esp32.robotcontroller.ui.components.ServoPanControl
import com.esp32.robotcontroller.ui.components.SpeedSlider
import com.esp32.robotcontroller.ui.theme.Primary
import com.esp32.robotcontroller.ui.theme.SurfaceCard
import com.esp32.robotcontroller.viewmodel.RobotViewModel

@Composable
fun ControlScreen(viewModel: RobotViewModel) {
    val isMotorOnline by viewModel.isMotorOnline.collectAsState()
    val isSensorOnline by viewModel.isSensorOnline.collectAsState()
    val isCameraOnline by viewModel.isCameraOnline.collectAsState()

    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val servoAngle by viewModel.servoAngle.collectAsState()
    val cameraFrame by viewModel.cameraFrame.collectAsState()
    val currentDirection by viewModel.currentDirection.collectAsState()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val cameraRetryCount by viewModel.cameraRetryCount.collectAsState()
    val cameraError by viewModel.cameraError.collectAsState()

    val motorUrl by viewModel.motorUrl.collectAsState()
    val sensorUrl by viewModel.sensorUrl.collectAsState()
    val cameraUrl by viewModel.cameraUrl.collectAsState()

    val motorStatus by viewModel.motorStatus.collectAsState()
    val sensorStatus by viewModel.sensorStatus.collectAsState()
    val cameraStatus by viewModel.cameraStatus.collectAsState()

    val sensorData by viewModel.sensorData.collectAsState()

    val distanceHistory by viewModel.distanceHistory.collectAsState()
    val temperatureHistory by viewModel.temperatureHistory.collectAsState()
    val humidityHistory by viewModel.humidityHistory.collectAsState()
    val mqHistory by viewModel.mqHistory.collectAsState()
    val waterHistory by viewModel.waterHistory.collectAsState()
    val axHistory by viewModel.axHistory.collectAsState()
    val ayHistory by viewModel.ayHistory.collectAsState()
    val azHistory by viewModel.azHistory.collectAsState()
    val gxHistory by viewModel.gxHistory.collectAsState()
    val gyHistory by viewModel.gyHistory.collectAsState()
    val gzHistory by viewModel.gzHistory.collectAsState()

    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()
    val yaw by viewModel.yaw.collectAsState()
    val isHudVisible by viewModel.isHudVisible.collectAsState()
    val isDemoMode by viewModel.isGyroDemoMode.collectAsState()

    var showConnectionDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        TabItem("Controls", Icons.Default.SportsEsports),
        TabItem("Sensors", Icons.Default.BarChart),
        TabItem("Attitude", Icons.Default.AirplanemodeActive),
        TabItem("Devices", Icons.Default.Router)
    )

    LaunchedEffect(Unit) {
        viewModel.startCameraStream()
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            currentMotorUrl = motorUrl,
            currentSensorUrl = sensorUrl,
            currentCameraUrl = cameraUrl,
            onDismiss = { showConnectionDialog = false },
            onSave = { newMotor, newSensor, newCam ->
                viewModel.updateConnectionUrls(newMotor, newSensor, newCam)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1117))
    ) {
        // === Top Header Bar ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111A23))
                .border(width = 0.5.dp, color = Color(0xFF263442))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ESP32 Robot Dashboard",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Motor + Sensors + ESP32-CAM",
                        fontSize = 11.sp,
                        color = Color(0xFF8EA0B2)
                    )
                }

                IconButton(
                    onClick = { showConnectionDialog = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Connection Settings",
                        tint = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header Status Pills (Motor, Sensors, Camera)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeaderStatusPill(label = "Motor", isOnline = isMotorOnline, modifier = Modifier.weight(1f))
                HeaderStatusPill(label = "Sensors", isOnline = isSensorOnline, modifier = Modifier.weight(1f))
                HeaderStatusPill(label = "Camera", isOnline = isCameraOnline, modifier = Modifier.weight(1f))
            }
        }

        // === Navigation Tabs ===
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF111A23),
            contentColor = Primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Primary
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(20.dp)) },
                    text = {
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // === Tab Content Area ===
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (selectedTabIndex) {
                0 -> ControlsTab(
                    cameraFrame = cameraFrame,
                    cameraState = cameraState,
                    pitch = pitch,
                    roll = roll,
                    yaw = yaw,
                    isHudVisible = isHudVisible,
                    isDemoMode = isDemoMode,
                    cameraRetryCount = cameraRetryCount,
                    cameraError = cameraError,
                    currentSpeed = currentSpeed,
                    servoAngle = servoAngle,
                    currentDirection = currentDirection,
                    uptimeSeconds = uptimeSeconds,
                    onZeroGyro = { viewModel.zeroGyro() },
                    onToggleDemo = { viewModel.toggleDemoMode() },
                    onToggleHud = { viewModel.toggleHud() },
                    onRetryCamera = { viewModel.retryCameraStream() },
                    onDirectionPress = { viewModel.sendDirection(it) },
                    onDirectionRelease = { viewModel.onDirectionRelease() },
                    onSpeedChange = { viewModel.updateSpeed(it) },
                    onServoChange = { viewModel.setServoAngle(it) },
                    onEmergencyStop = { viewModel.emergencyStop() }
                )

                1 -> SensorsAndGraphsTab(
                    sensorData = sensorData,
                    distanceHistory = distanceHistory,
                    temperatureHistory = temperatureHistory,
                    humidityHistory = humidityHistory,
                    mqHistory = mqHistory,
                    waterHistory = waterHistory,
                    axHistory = axHistory,
                    ayHistory = ayHistory,
                    azHistory = azHistory,
                    gxHistory = gxHistory,
                    gyHistory = gyHistory,
                    gzHistory = gzHistory
                )

                2 -> AttitudeTab(
                    roll = roll,
                    pitch = pitch
                )

                3 -> DevicesTab(
                    isMotorOnline = isMotorOnline,
                    motorStatus = motorStatus,
                    isSensorOnline = isSensorOnline,
                    sensorStatus = sensorStatus,
                    isCameraOnline = isCameraOnline,
                    cameraStatus = cameraStatus,
                    motorUrl = motorUrl,
                    sensorUrl = sensorUrl,
                    cameraUrl = cameraUrl,
                    onOpenSettings = { showConnectionDialog = true }
                )
            }
        }
    }
}

private data class TabItem(val title: String, val icon: ImageVector)

@Composable
private fun HeaderStatusPill(
    label: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF222C36))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF62E68B) else Color(0xFFFF6875))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                color = Color(0xFFAEBECD)
            )
            Text(
                text = if (isOnline) "ONLINE" else "OFFLINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) Color(0xFF62E68B) else Color(0xFFFF6875)
            )
        }
    }
}

/* ================= Tab 1: Controls ================= */

@Composable
private fun ControlsTab(
    cameraFrame: android.graphics.Bitmap?,
    cameraState: com.esp32.robotcontroller.viewmodel.CameraState,
    pitch: Float,
    roll: Float,
    yaw: Float,
    isHudVisible: Boolean,
    isDemoMode: Boolean,
    cameraRetryCount: Int,
    cameraError: String?,
    currentSpeed: Int,
    servoAngle: Int,
    currentDirection: String,
    uptimeSeconds: Long,
    onZeroGyro: () -> Unit,
    onToggleDemo: () -> Unit,
    onToggleHud: () -> Unit,
    onRetryCamera: () -> Unit,
    onDirectionPress: (String) -> Unit,
    onDirectionRelease: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onServoChange: (Int) -> Unit,
    onEmergencyStop: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Camera View with HUD
        MjpegView(
            frame = cameraFrame,
            cameraState = cameraState,
            pitch = pitch,
            roll = roll,
            yaw = yaw,
            isHudVisible = isHudVisible,
            isDemoMode = isDemoMode,
            onZeroGyro = onZeroGyro,
            onToggleDemo = onToggleDemo,
            onToggleHud = onToggleHud,
            retryCount = cameraRetryCount,
            errorMessage = cameraError,
            onRetry = onRetryCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        )

        // Servo Pan Control
        ServoPanControl(
            servoAngle = servoAngle,
            onAngleChange = onServoChange
        )

        // Motor Speed Slider
        SpeedSlider(
            currentSpeed = currentSpeed,
            onSpeedChange = onSpeedChange
        )

        // Controls Section: Direction D-Pad + Emergency Stop & Telemetry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111A23))
                .border(1.dp, Color(0xFF263442), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Direction & Speed & E-Stop
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmergencyStopButton(onEmergencyStop = onEmergencyStop)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "E-STOP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6875)
                )

                Spacer(modifier = Modifier.height(14.dp))

                TelemetryChip(label = "DIR", value = currentDirection)
                Spacer(modifier = Modifier.height(6.dp))
                TelemetryChip(label = "UP", value = formatUptime(uptimeSeconds))
            }

            // Right side: Directional Pad with center Stop
            DirectionalPad(
                onDirectionPress = onDirectionPress,
                onDirectionRelease = onDirectionRelease
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/* ================= Tab 2: Sensors & Graphs ================= */

@Composable
private fun SensorsAndGraphsTab(
    sensorData: com.esp32.robotcontroller.model.SensorData,
    distanceHistory: List<Float>,
    temperatureHistory: List<Float>,
    humidityHistory: List<Float>,
    mqHistory: List<Float>,
    waterHistory: List<Float>,
    axHistory: List<Float>,
    ayHistory: List<Float>,
    azHistory: List<Float>,
    gxHistory: List<Float>,
    gyHistory: List<Float>,
    gzHistory: List<Float>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sensor Metrics Grid
        SensorGridView(sensorData = sensorData)

        // Distance Graph
        SensorGraphCard(
            title = "Distance",
            datasets = listOf(GraphDataset("Distance", distanceHistory, Color(0xFF45B7FF))),
            minVal = 0f
        )

        // Temperature Graph
        SensorGraphCard(
            title = "Temperature",
            datasets = listOf(GraphDataset("Temperature", temperatureHistory, Color(0xFFFF725C)))
        )

        // Humidity Graph
        SensorGraphCard(
            title = "Humidity",
            datasets = listOf(GraphDataset("Humidity", humidityHistory, Color(0xFF56D6A1))),
            minVal = 0f,
            maxVal = 100f
        )

        // MQ Gas Sensor Graph
        SensorGraphCard(
            title = "MQ Sensor",
            datasets = listOf(GraphDataset("MQ", mqHistory, Color(0xFFD89CFF))),
            minVal = 0f,
            maxVal = 4095f
        )

        // Water Sensor Graph
        SensorGraphCard(
            title = "Water Sensor",
            datasets = listOf(GraphDataset("Water", waterHistory, Color(0xFF55D9E8))),
            minVal = 0f,
            maxVal = 4095f
        )

        // Accelerometer X/Y/Z Graph
        SensorGraphCard(
            title = "Accelerometer X/Y/Z",
            datasets = listOf(
                GraphDataset("X", axHistory, Color(0xFFFF6B6B)),
                GraphDataset("Y", ayHistory, Color(0xFF6BE58D)),
                GraphDataset("Z", azHistory, Color(0xFF65A9FF))
            )
        )

        // Gyroscope X/Y/Z Graph
        SensorGraphCard(
            title = "Gyroscope X/Y/Z",
            datasets = listOf(
                GraphDataset("X", gxHistory, Color(0xFFFF6B6B)),
                GraphDataset("Y", gyHistory, Color(0xFF6BE58D)),
                GraphDataset("Z", gzHistory, Color(0xFF65A9FF))
            )
        )

        Spacer(modifier = Modifier.height(14.dp))
    }
}

/* ================= Tab 3: Attitude ================= */

@Composable
private fun AttitudeTab(
    roll: Float,
    pitch: Float
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AircraftAttitudeCard(
            roll = roll,
            pitch = pitch
        )

        Spacer(modifier = Modifier.height(14.dp))
    }
}

/* ================= Tab 4: Devices ================= */

@Composable
private fun DevicesTab(
    isMotorOnline: Boolean,
    motorStatus: com.esp32.robotcontroller.model.DeviceStatus,
    isSensorOnline: Boolean,
    sensorStatus: com.esp32.robotcontroller.model.DeviceStatus,
    isCameraOnline: Boolean,
    cameraStatus: com.esp32.robotcontroller.model.DeviceStatus,
    motorUrl: String,
    sensorUrl: String,
    cameraUrl: String,
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Detailed Device Cards
        DeviceStatusSection(
            isMotorOnline = isMotorOnline,
            motorStatus = motorStatus,
            isSensorOnline = isSensorOnline,
            sensorStatus = sensorStatus,
            isCameraOnline = isCameraOnline,
            cameraStatus = cameraStatus
        )

        // Active URL summary Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0C141C))
                .border(1.dp, Color(0xFF263442), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured Endpoints",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "EDIT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.clickable { onOpenSettings() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            UrlInfoLine(label = "Motor", url = motorUrl)
            UrlInfoLine(label = "Sensor", url = sensorUrl)
            UrlInfoLine(label = "Camera", url = cameraUrl)
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun UrlInfoLine(label: String, url: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF8497AA))
        Text(text = url, fontSize = 12.sp, color = Color(0xFFE8EEF5), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TelemetryChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C141C))
            .border(1.dp, Color(0xFF263442), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label ",
                fontSize = 10.sp,
                color = Color(0xFF8497AA)
            )
            Text(
                text = value,
                fontSize = 11.sp,
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

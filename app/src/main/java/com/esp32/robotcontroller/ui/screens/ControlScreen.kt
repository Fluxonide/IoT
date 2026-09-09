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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.ui.components.ConnectionDialog
import com.esp32.robotcontroller.ui.components.DeviceStatusSection
import com.esp32.robotcontroller.ui.components.DirectionalPad
import com.esp32.robotcontroller.ui.components.GraphDataset
import com.esp32.robotcontroller.ui.components.MjpegView
import com.esp32.robotcontroller.ui.components.SensorGraphCard
import com.esp32.robotcontroller.ui.components.SensorGridView
import com.esp32.robotcontroller.ui.theme.Amber
import com.esp32.robotcontroller.ui.theme.BgDark
import com.esp32.robotcontroller.ui.theme.BgRaised
import com.esp32.robotcontroller.ui.theme.BorderLine
import com.esp32.robotcontroller.ui.theme.BorderLineSoft
import com.esp32.robotcontroller.ui.theme.Cyan
import com.esp32.robotcontroller.ui.theme.Green
import com.esp32.robotcontroller.ui.theme.GreenDim
import com.esp32.robotcontroller.ui.theme.PanelDark
import com.esp32.robotcontroller.ui.theme.PanelDark2
import com.esp32.robotcontroller.ui.theme.Purple
import com.esp32.robotcontroller.ui.theme.Red
import com.esp32.robotcontroller.ui.theme.RedDim
import com.esp32.robotcontroller.ui.theme.TextDim
import com.esp32.robotcontroller.ui.theme.TextFaint
import com.esp32.robotcontroller.ui.theme.TextMain
import com.esp32.robotcontroller.viewmodel.RobotViewModel

@Composable
fun ControlScreen(viewModel: RobotViewModel) {
    val isMotorOnline by viewModel.isMotorOnline.collectAsState()
    val isCameraOnline by viewModel.isCameraOnline.collectAsState()

    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val servoAngle by viewModel.servoAngle.collectAsState()
    val cameraFrame by viewModel.cameraFrame.collectAsState()
    val currentDirection by viewModel.currentDirection.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val cameraRetryCount by viewModel.cameraRetryCount.collectAsState()
    val cameraError by viewModel.cameraError.collectAsState()

    val motorUrl by viewModel.motorUrl.collectAsState()
    val sensorUrl by viewModel.sensorUrl.collectAsState()
    val cameraUrl by viewModel.cameraUrl.collectAsState()

    val motorStatus by viewModel.motorStatus.collectAsState()
    val cameraStatus by viewModel.cameraStatus.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()

    val distanceHistory by viewModel.distanceHistory.collectAsState()
    val temperatureHistory by viewModel.temperatureHistory.collectAsState()
    val humidityHistory by viewModel.humidityHistory.collectAsState()
    val mqHistory by viewModel.mqHistory.collectAsState()
    val waterHistory by viewModel.waterHistory.collectAsState()

    var showConnectionDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val isConnected = isMotorOnline || isCameraOnline

    val tabs = listOf(
        TabItem("Controls", Icons.Default.SportsEsports),
        TabItem("Telemetry", Icons.Default.BarChart),
        TabItem("Nodes", Icons.Default.Router)
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
            .background(BgDark)
    ) {
        // ============================================================
        // HEADER — RVR-01 FIELD TELEMETRY CONSOLE
        // ============================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgRaised)
                .border(width = 0.5.dp, color = BorderLine)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RVR",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Text(
                        text = "-01",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Amber
                    )
                }
                Text(
                    text = "FIELD TELEMETRY CONSOLE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                    color = TextFaint
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Connection badge matching HTML header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, BorderLine, RoundedCornerShape(20.dp))
                        .background(PanelDark)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Green else Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) "Connected" else "Offline",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = TextDim
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { showConnectionDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Amber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ============================================================
        // NAVIGATION TABS
        // ============================================================
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = BgRaised,
            contentColor = Amber,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Amber,
                    height = 2.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) },
                    text = {
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) Amber else TextDim
                        )
                    }
                )
            }
        }

        // ============================================================
        // TAB CONTENT AREA
        // ============================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            when (selectedTabIndex) {
                0 -> OptimizedCockpitTab(
                    cameraFrame = cameraFrame,
                    cameraState = cameraState,
                    isCameraOnline = isCameraOnline,
                    cameraRetryCount = cameraRetryCount,
                    cameraError = cameraError,
                    currentSpeed = currentSpeed,
                    servoAngle = servoAngle,
                    currentDirection = currentDirection,
                    onRetryCamera = { viewModel.retryCameraStream() },
                    onDirectionPress = { viewModel.sendDirection(it) },
                    onDirectionRelease = { viewModel.onDirectionRelease() },
                    onSpeedChange = { viewModel.updateSpeed(it) },
                    onServoChange = { viewModel.setServoAngle(it) },
                    onServoLeft = { viewModel.servoLeft() },
                    onServoCenter = { viewModel.servoCenter() },
                    onServoRight = { viewModel.servoRight() }
                )

                1 -> TelemetryTab(
                    sensorData = sensorData,
                    distanceHistory = distanceHistory,
                    temperatureHistory = temperatureHistory,
                    humidityHistory = humidityHistory,
                    mqHistory = mqHistory,
                    waterHistory = waterHistory
                )

                2 -> NodesTab(
                    isMotorOnline = isMotorOnline,
                    motorStatus = motorStatus,
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

/* ============================================================
   TAB 1: OPTIMIZED COCKPIT (Zero-Scroll Camera + Control Wheel)
   Fixes UX: Control wheel staying on screen, Cam staying visible
   Removes: Redundant X-cross emergency stop button
   ============================================================ */

@Composable
private fun OptimizedCockpitTab(
    cameraFrame: android.graphics.Bitmap?,
    cameraState: com.esp32.robotcontroller.viewmodel.CameraState,
    isCameraOnline: Boolean,
    cameraRetryCount: Int,
    cameraError: String?,
    currentSpeed: Int,
    servoAngle: Int,
    currentDirection: String,
    onRetryCamera: () -> Unit,
    onDirectionPress: (String) -> Unit,
    onDirectionRelease: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onServoChange: (Int) -> Unit,
    onServoLeft: () -> Unit,
    onServoCenter: () -> Unit,
    onServoRight: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- 01 Optical Feed Card ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .border(1.dp, BorderLine, RoundedCornerShape(3.dp))
                .background(PanelDark)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "01",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = Amber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Optical feed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain
                    )
                }

                // Camera pill status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, if (isCameraOnline) GreenDim else RedDim, RoundedCornerShape(20.dp))
                        .background(if (isCameraOnline) Color(0x106FDC8C) else Color(0x10FF6B6B))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isCameraOnline) "online" else "checking",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCameraOnline) Green else Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Camera stream frame with compact height (~175dp) so controls fit below
            MjpegView(
                frame = cameraFrame,
                cameraState = cameraState,
                retryCount = cameraRetryCount,
                errorMessage = cameraError,
                onRetry = onRetryCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            )
        }

        // --- Side-by-Side Cockpit: Controls & Sliders in one view without scrolling ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Deck: Motor Speed & Servo Pan Controls (weight ~1.15)
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, BorderLine, RoundedCornerShape(3.dp))
                    .background(PanelDark)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section: Motor Speed
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MOTOR SPEED",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDim,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$currentSpeed / 255",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Amber
                        )
                    }

                    Slider(
                        value = currentSpeed.toFloat(),
                        onValueChange = { onSpeedChange(it.toInt()) },
                        valueRange = 0f..255f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Amber,
                            activeTrackColor = Amber,
                            inactiveTrackColor = BorderLineSoft
                        )
                    )
                }

                // Section: Camera Servo
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CAM SERVO",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDim,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$servoAngle°",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Amber
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Preset buttons: ◄ Left (20), ● Center (90), Right ► (160)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QuickServoButton(
                            label = "◄",
                            isSelected = servoAngle == 20,
                            onClick = onServoLeft,
                            modifier = Modifier.weight(1f)
                        )
                        QuickServoButton(
                            label = "●",
                            isSelected = servoAngle == 90,
                            onClick = onServoCenter,
                            modifier = Modifier.weight(1f)
                        )
                        QuickServoButton(
                            label = "►",
                            isSelected = servoAngle == 160,
                            onClick = onServoRight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Slider(
                        value = servoAngle.toFloat(),
                        onValueChange = { onServoChange(it.toInt()) },
                        valueRange = 20f..160f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Amber,
                            activeTrackColor = Amber,
                            inactiveTrackColor = BorderLineSoft
                        )
                    )
                }
            }

            // Right Deck: Movement Controls D-Pad with Center STOP (weight ~1.0)
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, BorderLine, RoundedCornerShape(3.dp))
                    .background(PanelDark)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: 03 Movement & current state badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "03",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Amber
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Move",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMain
                        )
                    }

                    Text(
                        text = currentDirection,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (currentDirection == "STOP") Red else Green
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Responsive D-Pad: Compact 48dp buttons with center STOP button
                DirectionalPad(
                    onDirectionPress = onDirectionPress,
                    onDirectionRelease = onDirectionRelease,
                    buttonSize = 46.dp,
                    spacing = 5.dp
                )
            }
        }
    }
}

@Composable
private fun QuickServoButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Amber.copy(alpha = 0.15f) else PanelDark2
    val border = if (isSelected) Amber else BorderLine
    val textC = if (isSelected) Amber else TextMain

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = textC
        )
    }
}

/* ============================================================
   TAB 2: TELEMETRY (Sensors + 5 History Charts)
   ============================================================ */

@Composable
private fun TelemetryTab(
    sensorData: com.esp32.robotcontroller.model.SensorData,
    distanceHistory: List<Float>,
    temperatureHistory: List<Float>,
    humidityHistory: List<Float>,
    mqHistory: List<Float>,
    waterHistory: List<Float>
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 04: Live sensor data (Distance, Temperature, Humidity, MQ, Water)
        SensorGridView(sensorData = sensorData)

        // Telemetry History Label matching HTML
        Text(
            text = "TELEMETRY HISTORY — LAST 60 SAMPLES",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = TextFaint,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        // 1. Distance Graph
        SensorGraphCard(
            title = "Ultrasonic distance",
            datasets = listOf(GraphDataset("Distance (cm)", distanceHistory, Amber)),
            minVal = 0f
        )

        // 2. Temperature Graph
        SensorGraphCard(
            title = "Temperature",
            datasets = listOf(GraphDataset("Temperature (°C)", temperatureHistory, Red))
        )

        // 3. Humidity Graph
        SensorGraphCard(
            title = "Humidity",
            datasets = listOf(GraphDataset("Humidity (%)", humidityHistory, Cyan)),
            minVal = 0f,
            maxVal = 100f
        )

        // 4. MQ Sensor Graph
        SensorGraphCard(
            title = "MQ sensor",
            datasets = listOf(GraphDataset("MQ Value", mqHistory, Green)),
            minVal = 0f
        )

        // 5. Water Sensor Graph
        SensorGraphCard(
            title = "Water sensor",
            datasets = listOf(GraphDataset("Water Value", waterHistory, Purple)),
            minVal = 0f
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

/* ============================================================
   TAB 3: NODES (Motor Node + Camera Node Status)
   ============================================================ */

@Composable
private fun NodesTab(
    isMotorOnline: Boolean,
    motorStatus: com.esp32.robotcontroller.model.DeviceStatus,
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Node 05 & 06 Cards
        DeviceStatusSection(
            isMotorOnline = isMotorOnline,
            motorStatus = motorStatus,
            isCameraOnline = isCameraOnline,
            cameraStatus = cameraStatus
        )

        // Configured Endpoints Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .border(1.dp, BorderLine, RoundedCornerShape(3.dp))
                .background(PanelDark)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured Endpoints",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )

                Text(
                    text = "EDIT",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Amber,
                    modifier = Modifier.clickable { onOpenSettings() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            UrlInfoLine(label = "Motor ESP32", url = motorUrl)
            UrlInfoLine(label = "Sensor ESP32", url = sensorUrl)
            UrlInfoLine(label = "Camera ESP32", url = cameraUrl)
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun UrlInfoLine(label: String, url: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = BorderLineSoft.copy(alpha = 0.5f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextFaint)
        Text(
            text = url,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMain,
            fontWeight = FontWeight.Medium
        )
    }
}


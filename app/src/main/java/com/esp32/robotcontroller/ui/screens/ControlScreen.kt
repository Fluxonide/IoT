package com.esp32.robotcontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import android.content.res.Configuration
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
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
import java.util.Locale

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ============================================================
        // HEADER — RVR-01 FIELD TELEMETRY CONSOLE
        // ============================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        ) {
            // Status bar spacer so the top bar color extends seamlessly behind Android status bar
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 7.dp),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "-01",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "FIELD TELEMETRY CONSOLE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Connection badge matching HTML header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

        // ============================================================
        // NAVIGATION TABS
        // ============================================================
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MaterialTheme.colorScheme.primary,
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
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
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
                    sensorData = sensorData,
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
   TAB 1: OPTIMIZED CONTROLS COCKPIT
   - 01 Optical Feed with integrated Pan Servo Control
   - 04 Live Sensor Telemetry Strip (Glanceable HUD)
   - 03 Movement & Speed Cockpit (Side-by-side D-Pad & Throttle)
   Engineered for 392 dpi (and all standard DPIs) so controls
   never get pushed down or cut off off-screen!
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
    sensorData: com.esp32.robotcontroller.model.SensorData,
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
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isCompact = screenHeightDp < 700.dp
    val isMedium = screenHeightDp in 700.dp..840.dp // covers standard 392 dpi phones

    // Adaptive sizing based on screen density / DPI / display scaling
    val cameraStreamHeight = when {
        isLandscape -> 120.dp
        isCompact -> 115.dp
        isMedium -> 130.dp
        else -> 155.dp
    }
    val dpadButtonSize = when {
        isLandscape -> 44.dp
        isCompact -> 42.dp
        isMedium -> 48.dp
        else -> 52.dp
    }
    val dpadSpacing = when {
        isLandscape -> 4.dp
        isCompact -> 3.dp
        isMedium -> 4.dp
        else -> 5.dp
    }
    val cardPadding = if (isCompact || isMedium) 8.dp else 10.dp
    val cardSpacing = if (isCompact || isMedium) 5.dp else 7.dp

    if (isLandscape) {
        // Landscape 2-Column Split Cockpit
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1.05f),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                OpticalFeedCard(
                    cameraFrame = cameraFrame,
                    cameraState = cameraState,
                    isCameraOnline = isCameraOnline,
                    cameraRetryCount = cameraRetryCount,
                    cameraError = cameraError,
                    servoAngle = servoAngle,
                    cameraStreamHeight = cameraStreamHeight,
                    cardPadding = cardPadding,
                    onRetryCamera = onRetryCamera,
                    onServoChange = onServoChange,
                    onServoLeft = onServoLeft,
                    onServoCenter = onServoCenter,
                    onServoRight = onServoRight
                )

                LiveSensorsStrip(
                    sensorData = sensorData,
                    cardPadding = cardPadding
                )
            }

            Column(
                modifier = Modifier.weight(0.95f),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                MovementAndSpeedCard(
                    currentDirection = currentDirection,
                    currentSpeed = currentSpeed,
                    dpadButtonSize = dpadButtonSize,
                    dpadSpacing = dpadSpacing,
                    cardPadding = cardPadding,
                    onDirectionPress = onDirectionPress,
                    onDirectionRelease = onDirectionRelease,
                    onSpeedChange = onSpeedChange
                )
            }
        }
    } else {
        // Portrait Cockpit Layout:
        // Compact height ensures all 3 sections remain fully on screen on 392 dpi
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(cardSpacing)
        ) {
            // CARD 1: 01 OPTICAL FEED & INTEGRATED SERVO PAN
            OpticalFeedCard(
                cameraFrame = cameraFrame,
                cameraState = cameraState,
                isCameraOnline = isCameraOnline,
                cameraRetryCount = cameraRetryCount,
                cameraError = cameraError,
                servoAngle = servoAngle,
                cameraStreamHeight = cameraStreamHeight,
                cardPadding = cardPadding,
                onRetryCamera = onRetryCamera,
                onServoChange = onServoChange,
                onServoLeft = onServoLeft,
                onServoCenter = onServoCenter,
                onServoRight = onServoRight
            )

            // CARD 2: 04 LIVE SENSORS TELEMETRY STRIP (HIGH-VISIBILITY HUD)
            LiveSensorsStrip(
                sensorData = sensorData,
                cardPadding = cardPadding
            )

            // CARD 3: 03 MOVEMENT & SPEED COCKPIT (SIDE-BY-SIDE ERGONOMIC CONTROL)
            MovementAndSpeedCard(
                currentDirection = currentDirection,
                currentSpeed = currentSpeed,
                dpadButtonSize = dpadButtonSize,
                dpadSpacing = dpadSpacing,
                cardPadding = cardPadding,
                onDirectionPress = onDirectionPress,
                onDirectionRelease = onDirectionRelease,
                onSpeedChange = onSpeedChange
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun OpticalFeedCard(
    cameraFrame: android.graphics.Bitmap?,
    cameraState: com.esp32.robotcontroller.viewmodel.CameraState,
    isCameraOnline: Boolean,
    cameraRetryCount: Int,
    cameraError: String?,
    servoAngle: Int,
    cameraStreamHeight: androidx.compose.ui.unit.Dp,
    cardPadding: androidx.compose.ui.unit.Dp,
    onRetryCamera: () -> Unit,
    onServoChange: (Int) -> Unit,
    onServoLeft: () -> Unit,
    onServoCenter: () -> Unit,
    onServoRight: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(cardPadding)
    ) {
        // Header: 01 Optical feed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "01",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Optical feed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Camera status pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, if (isCameraOnline) GreenDim else RedDim, RoundedCornerShape(20.dp))
                    .background(if (isCameraOnline) Color(0x106FDC8C) else Color(0x10FF6B6B))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isCameraOnline) "online" else "checking",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCameraOnline) Green else Red
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Adaptive Camera stream viewport
        MjpegView(
            frame = cameraFrame,
            cameraState = cameraState,
            retryCount = cameraRetryCount,
            errorMessage = cameraError,
            onRetry = onRetryCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(cameraStreamHeight)
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Integrated Sleek Camera Pan Servo Control Row (replaces previous multi-row stack)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "PAN",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            QuickActionChip(
                label = "◄ 20°",
                isSelected = servoAngle == 20,
                onClick = onServoLeft,
                modifier = Modifier.weight(0.9f)
            )
            QuickActionChip(
                label = "● 90°",
                isSelected = servoAngle == 90,
                onClick = onServoCenter,
                modifier = Modifier.weight(0.9f)
            )
            QuickActionChip(
                label = "160° ►",
                isSelected = servoAngle == 160,
                onClick = onServoRight,
                modifier = Modifier.weight(0.9f)
            )

            var localServoAngle by remember(servoAngle) { mutableFloatStateOf(servoAngle.toFloat()) }
            Slider(
                value = localServoAngle,
                onValueChange = {
                    localServoAngle = it
                    onServoChange(it.toInt())
                },
                valueRange = 20f..160f,
                modifier = Modifier
                    .weight(1.6f)
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Text(
                text = "$servoAngle°",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LiveSensorsStrip(
    sensorData: com.esp32.robotcontroller.model.SensorData,
    cardPadding: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = cardPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val distVal = sensorData.distance
        val isObstacleClose = distVal in 0.1f..15f
        val isObstacleCaution = distVal in 15f..30f
        val distColor = when {
            isObstacleClose -> MaterialTheme.colorScheme.error
            isObstacleCaution -> Amber
            else -> MaterialTheme.colorScheme.onSurface
        }

        MiniSensorBox(
            label = "DIST",
            value = String.format(Locale.US, "%.1f", distVal),
            unit = "cm",
            valueColor = distColor,
            isAlert = isObstacleClose,
            modifier = Modifier.weight(1.15f)
        )

        MiniSensorBox(
            label = "TEMP",
            value = String.format(Locale.US, "%.1f", sensorData.temperature),
            unit = "°C",
            modifier = Modifier.weight(1f)
        )

        MiniSensorBox(
            label = "HUM",
            value = String.format(Locale.US, "%.0f", sensorData.humidity),
            unit = "%",
            modifier = Modifier.weight(1f)
        )

        MiniSensorBox(
            label = "MQ",
            value = String.format(Locale.US, "%.0f", sensorData.mq),
            unit = null,
            modifier = Modifier.weight(0.9f)
        )

        MiniSensorBox(
            label = "WATER",
            value = String.format(Locale.US, "%.0f", sensorData.water),
            unit = null,
            modifier = Modifier.weight(0.95f)
        )
    }
}

@Composable
private fun MovementAndSpeedCard(
    currentDirection: String,
    currentSpeed: Int,
    dpadButtonSize: androidx.compose.ui.unit.Dp,
    dpadSpacing: androidx.compose.ui.unit.Dp,
    cardPadding: androidx.compose.ui.unit.Dp,
    onDirectionPress: (String) -> Unit,
    onDirectionRelease: () -> Unit,
    onSpeedChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(cardPadding)
    ) {
        // Header: 03 Movement & Speed + Live driving status pill
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
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Movement & Speed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val isBraked = currentDirection == "STOP" || currentDirection.isEmpty()
            val statusText = when (currentDirection) {
                "F" -> "DRIVING (FWD ▲)"
                "B" -> "DRIVING (REV ▼)"
                "L" -> "TURNING (LEFT ◄)"
                "R" -> "TURNING (RIGHT ►)"
                else -> "BRAKED"
            }
            val statusColor = if (isBraked) Red else Green
            val statusBg = if (isBraked) RedDim else GreenDim

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusBg)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Side-by-Side Ergonomic Cockpit: D-Pad on Left, Speed Throttle on Right!
        // This cuts vertical height in half, ensuring movement & speed never get cut off on 392 dpi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DIRECTIONAL PAD
            DirectionalPad(
                onDirectionPress = onDirectionPress,
                onDirectionRelease = onDirectionRelease,
                buttonSize = dpadButtonSize,
                spacing = dpadSpacing
            )

            // MOTOR SPEED THROTTLE COLUMN
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header: THROTTLE label + Speed readout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THROTTLE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$currentSpeed",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / 255",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Speed Slider
                var localSpeed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed.toFloat()) }
                Slider(
                    value = localSpeed,
                    onValueChange = {
                        localSpeed = it
                        onSpeedChange(it.toInt())
                    },
                    valueRange = 0f..255f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Gear Presets: 100, 180, 255
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    QuickActionChip(
                        label = "100",
                        isSelected = currentSpeed == 100,
                        onClick = { onSpeedChange(100) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionChip(
                        label = "180",
                        isSelected = currentSpeed == 180,
                        onClick = { onSpeedChange(180) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionChip(
                        label = "255",
                        isSelected = currentSpeed == 255,
                        onClick = { onSpeedChange(255) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Precision Steppers: -10, 0 (IDLE), +10
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    QuickStepButton(
                        label = "-10",
                        onClick = { onSpeedChange((currentSpeed - 10).coerceAtLeast(0)) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickStepButton(
                        label = "0 IDLE",
                        onClick = { onSpeedChange(0) },
                        modifier = Modifier.weight(1.2f)
                    )
                    QuickStepButton(
                        label = "+10",
                        onClick = { onSpeedChange((currentSpeed + 10).coerceAtMost(255)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Hold arrow to drive · Center STOP to brake",
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun QuickStepButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val border = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val textC = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

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
            color = textC
        )
    }
}

@Composable
private fun MiniSensorBox(
    label: String,
    value: String,
    unit: String?,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isAlert: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = if (isAlert) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val border = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(0.5.dp, border, RoundedCornerShape(3.dp))
            .background(bg)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 0.2.sp
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(1.dp))
                    Text(
                        text = unit,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        // 1. Distance Graph
        SensorGraphCard(
            title = "Ultrasonic distance",
            datasets = listOf(GraphDataset("Distance (cm)", distanceHistory, MaterialTheme.colorScheme.primary)),
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
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "EDIT",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(
            text = url,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}


package com.esp32.robotcontroller.ui.screens

import androidx.compose.foundation.background
import com.esp32.robotcontroller.ui.components.rememberSliderHapticTracker
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    val streamUrl by viewModel.streamUrl.collectAsState()
    val wsUrl by viewModel.wsUrl.collectAsState()

    val motorStatus by viewModel.motorStatus.collectAsState()
    val cameraStatus by viewModel.cameraStatus.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()

    val distanceHistory by viewModel.distanceHistory.collectAsState()
    val temperatureHistory by viewModel.temperatureHistory.collectAsState()
    val humidityHistory by viewModel.humidityHistory.collectAsState()
    val mqHistory by viewModel.mqHistory.collectAsState()
    val waterHistory by viewModel.waterHistory.collectAsState()
    val predictions by viewModel.predictions.collectAsState()

    var showConnectionDialog by remember { mutableStateOf(false) }

    val tabs = remember {
        listOf(
            TabItem("Controls", Icons.Default.SportsEsports),
            TabItem("Telemetry", Icons.Default.BarChart),
            TabItem("Nodes", Icons.Default.Router)
        )
    }

    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    val isConnected = isMotorOnline || isCameraOnline

    LaunchedEffect(Unit) {
        viewModel.startCameraStream()
    }

    if (showConnectionDialog) {
        ConnectionDialog(
            currentMotorUrl = motorUrl,
            currentSensorUrl = sensorUrl,
            currentCameraUrl = cameraUrl,
            currentStreamUrl = streamUrl,
            currentWsUrl = wsUrl,
            onDismiss = { showConnectionDialog = false },
            onSave = { newMotor, newSensor, newCam, newStream, newWs ->
                viewModel.updateConnectionUrls(newMotor, newSensor, newCam, newStream, newWs)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ============================================================
        // HEADER — VIKRAM-01 FIELD TELEMETRY CONSOLE
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
                        text = "VIKRAM",
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
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 2.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp)) },
                    text = {
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // ============================================================
        // TAB CONTENT AREA — swipeable via HorizontalPager
        // ============================================================
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) { page ->
            when (page) {
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
                    onServoFinished = { viewModel.setServoAngle(it, immediate = true) },
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
                    waterHistory = waterHistory,
                    predictions = predictions
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
   TAB 1: CONTROLS COCKPIT
   Uses weight-based layout so the camera feed fills the top
   and controls fill the bottom — nothing gets cut off and
   everything is properly sized on any DPI (392, 440, etc.)
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
    onServoFinished: (Int) -> Unit = {},
    onServoLeft: () -> Unit,
    onServoCenter: () -> Unit,
    onServoRight: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // LANDSCAPE: Camera left, Controls right
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CameraFeedSection(
                    cameraFrame = cameraFrame,
                    cameraState = cameraState,
                    isCameraOnline = isCameraOnline,
                    cameraRetryCount = cameraRetryCount,
                    cameraError = cameraError,
                    onRetryCamera = onRetryCamera,
                    modifier = Modifier.weight(1f)
                )
                ServoPanRow(
                    servoAngle = servoAngle,
                    onServoChange = onServoChange,
                    onServoFinished = onServoFinished,
                    onServoLeft = onServoLeft,
                    onServoCenter = onServoCenter,
                    onServoRight = onServoRight
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ControlsSection(
                    currentDirection = currentDirection,
                    currentSpeed = currentSpeed,
                    sensorData = sensorData,
                    onDirectionPress = onDirectionPress,
                    onDirectionRelease = onDirectionRelease,
                    onSpeedChange = onSpeedChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        // PORTRAIT: Camera top, Controls bottom — weight-based, no scrolling needed
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // TOP: Camera Feed + Servo Pan (~40%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CameraFeedSection(
                    cameraFrame = cameraFrame,
                    cameraState = cameraState,
                    isCameraOnline = isCameraOnline,
                    cameraRetryCount = cameraRetryCount,
                    cameraError = cameraError,
                    onRetryCamera = onRetryCamera,
                    modifier = Modifier.weight(1f)
                )
                ServoPanRow(
                    servoAngle = servoAngle,
                    onServoChange = onServoChange,
                    onServoFinished = onServoFinished,
                    onServoLeft = onServoLeft,
                    onServoCenter = onServoCenter,
                    onServoRight = onServoRight
                )
            }

            // BOTTOM: Controls (~58%)
            ControlsSection(
                currentDirection = currentDirection,
                currentSpeed = currentSpeed,
                sensorData = sensorData,
                onDirectionPress = onDirectionPress,
                onDirectionRelease = onDirectionRelease,
                onSpeedChange = onSpeedChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.58f)
            )
        }
    }
}

@Composable
private fun CameraFeedSection(
    cameraFrame: android.graphics.Bitmap?,
    cameraState: com.esp32.robotcontroller.viewmodel.CameraState,
    isCameraOnline: Boolean,
    cameraRetryCount: Int,
    cameraError: String?,
    onRetryCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
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

        // Camera stream fills all remaining vertical space
        MjpegView(
            frame = cameraFrame,
            cameraState = cameraState,
            retryCount = cameraRetryCount,
            errorMessage = cameraError,
            onRetry = onRetryCamera,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun ServoPanRow(
    servoAngle: Int,
    onServoChange: (Int) -> Unit,
    onServoFinished: (Int) -> Unit = {},
    onServoLeft: () -> Unit,
    onServoCenter: () -> Unit,
    onServoRight: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "PAN",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val servoPresets = remember { listOf(20, 90, 160) }
        val servoHapticTracker = rememberSliderHapticTracker(presets = servoPresets, resetDistance = 3f)

        QuickActionChip(
            label = "◄ 20°",
            isSelected = servoAngle == 20,
            onClick = {
                servoHapticTracker.triggerManualPreset(20)
                onServoLeft()
            },
            modifier = Modifier.weight(0.8f)
        )
        QuickActionChip(
            label = "● 90°",
            isSelected = servoAngle == 90,
            onClick = {
                servoHapticTracker.triggerManualPreset(90)
                onServoCenter()
            },
            modifier = Modifier.weight(0.8f)
        )
        QuickActionChip(
            label = "160° ►",
            isSelected = servoAngle == 160,
            onClick = {
                servoHapticTracker.triggerManualPreset(160)
                onServoRight()
            },
            modifier = Modifier.weight(0.8f)
        )

        var localServoAngle by remember(servoAngle) { mutableFloatStateOf(servoAngle.toFloat()) }
        Slider(
            value = localServoAngle,
            onValueChange = {
                localServoAngle = it
                servoHapticTracker.onValueChange(it)
                onServoChange(it.toInt())
            },
            onValueChangeFinished = {
                onServoFinished(localServoAngle.toInt())
            },
            valueRange = 20f..160f,
            modifier = Modifier
                .weight(1.5f)
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Text(
            text = "${localServoAngle.toInt()}°",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ControlsSection(
    currentDirection: String,
    currentSpeed: Int,
    sensorData: com.esp32.robotcontroller.model.SensorData,
    onDirectionPress: (String) -> Unit,
    onDirectionRelease: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isBraked) RedDim else GreenDim)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (currentDirection) {
                        "F" -> "FWD ▲"
                        "B" -> "REV ▼"
                        "L" -> "LEFT ◄"
                        "R" -> "RIGHT ►"
                        else -> "BRAKED"
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isBraked) Red else Green
                )
            }
        }

        // D-Pad — centered, fills available vertical space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            DirectionalPad(
                onDirectionPress = onDirectionPress,
                onDirectionRelease = onDirectionRelease,
                buttonSize = 56.dp,
                spacing = 6.dp
            )
        }

        // Speed Throttle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currentSpeed",
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " / 255",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            val speedPresets = remember { listOf(100, 180, 255) }
            val hapticTracker = rememberSliderHapticTracker(presets = speedPresets, resetDistance = 4f)

            var localSpeed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed.toFloat()) }
            Slider(
                value = localSpeed,
                onValueChange = {
                    localSpeed = it
                    hapticTracker.onValueChange(it)
                    onSpeedChange(it.toInt())
                },
                valueRange = 0f..255f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickStepButton(
                    label = "−10",
                    onClick = {
                        val newSpeed = (currentSpeed - 10).coerceAtLeast(0)
                        hapticTracker.triggerStep(newSpeed)
                        onSpeedChange(newSpeed)
                    },
                    modifier = Modifier.weight(0.8f)
                )
                QuickActionChip(
                    label = "100",
                    isSelected = currentSpeed == 100,
                    onClick = {
                        hapticTracker.triggerManualPreset(100)
                        onSpeedChange(100)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "180",
                    isSelected = currentSpeed == 180,
                    onClick = {
                        hapticTracker.triggerManualPreset(180)
                        onSpeedChange(180)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "255",
                    isSelected = currentSpeed == 255,
                    onClick = {
                        hapticTracker.triggerManualPreset(255)
                        onSpeedChange(255)
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickStepButton(
                    label = "+10",
                    onClick = {
                        val newSpeed = (currentSpeed + 10).coerceAtMost(255)
                        hapticTracker.triggerStep(newSpeed)
                        onSpeedChange(newSpeed)
                    },
                    modifier = Modifier.weight(0.8f)
                )
            }
        }

        // Sensor HUD strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val distVal = sensorData.distance
            val isClose = distVal in 0.1f..15f
            val distColor = when {
                isClose -> MaterialTheme.colorScheme.error
                distVal in 15f..30f -> Amber
                else -> MaterialTheme.colorScheme.onSurface
            }

            MiniSensorBox("DIST", String.format(Locale.US, "%.1f", distVal), "cm", distColor, isClose, Modifier.weight(1f))
            MiniSensorBox("TEMP", String.format(Locale.US, "%.1f", sensorData.temperature), "°C", modifier = Modifier.weight(1f))
            MiniSensorBox("HUM", String.format(Locale.US, "%.0f", sensorData.humidity), "%", modifier = Modifier.weight(1f))
            MiniSensorBox("MQ", String.format(Locale.US, "%.0f", sensorData.mq), null, modifier = Modifier.weight(0.85f))
            MiniSensorBox("WATER", String.format(Locale.US, "%.0f", sensorData.water), null, modifier = Modifier.weight(0.85f))
        }
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
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
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
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
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
            .clip(RoundedCornerShape(4.dp))
            .border(0.5.dp, border, RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 12.sp,
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
    waterHistory: List<Float>,
    predictions: Map<String, com.esp32.robotcontroller.model.PredictionState> = emptyMap()
) {
    val scrollState = rememberScrollState()

    val distPred = (predictions["distance"] as? com.esp32.robotcontroller.model.PredictionState.Ready)?.futurePoints ?: emptyList()
    val tempPred = (predictions["temperature"] as? com.esp32.robotcontroller.model.PredictionState.Ready)?.futurePoints ?: emptyList()
    val humPred = (predictions["humidity"] as? com.esp32.robotcontroller.model.PredictionState.Ready)?.futurePoints ?: emptyList()
    val mqPred = (predictions["mq"] as? com.esp32.robotcontroller.model.PredictionState.Ready)?.futurePoints ?: emptyList()
    val waterPred = (predictions["water"] as? com.esp32.robotcontroller.model.PredictionState.Ready)?.futurePoints ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 04: Live sensor data & AI Prediction
        SensorGridView(sensorData = sensorData, predictions = predictions)

        // Telemetry History Label matching HTML
        Text(
            text = "TELEMETRY HISTORY & AI FORECAST — PAST (SOLID) / FUTURE (DASHED)",
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
            datasets = listOf(GraphDataset("Distance (cm)", distanceHistory, MaterialTheme.colorScheme.primary, predictionData = distPred)),
            minVal = 0f
        )

        // 2. Temperature Graph
        SensorGraphCard(
            title = "Temperature",
            datasets = listOf(GraphDataset("Temperature (°C)", temperatureHistory, Red, predictionData = tempPred))
        )

        // 3. Humidity Graph
        SensorGraphCard(
            title = "Humidity",
            datasets = listOf(GraphDataset("Humidity (%)", humidityHistory, Cyan, predictionData = humPred)),
            minVal = 0f,
            maxVal = 100f
        )

        // 4. MQ Sensor Graph
        SensorGraphCard(
            title = "MQ sensor",
            datasets = listOf(GraphDataset("MQ Value", mqHistory, Green, predictionData = mqPred)),
            minVal = 0f
        )

        // 5. Water Sensor Graph
        SensorGraphCard(
            title = "Water sensor",
            datasets = listOf(GraphDataset("Water Value", waterHistory, Purple, predictionData = waterPred)),
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


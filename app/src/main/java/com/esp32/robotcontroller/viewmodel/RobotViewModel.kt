package com.esp32.robotcontroller.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esp32.robotcontroller.network.RobotApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin

enum class CameraState {
    DISCONNECTED,   // Not started or stopped
    CONNECTING,     // Attempting to connect to camera
    STREAMING,      // Receiving frames
    ERROR           // Connection failed, will retry
}

class RobotViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("robot_controller_prefs", Context.MODE_PRIVATE)

    private val initialRobotUrl = RobotApiService.sanitizeControlUrl(
        prefs.getString("robot_url", "http://192.168.137.50") ?: "http://192.168.137.50"
    )
    private val initialCameraUrl = RobotApiService.sanitizeCameraWsUrl(
        prefs.getString("camera_url", "ws://192.168.137.60/ws") ?: "ws://192.168.137.60/ws"
    )

    private val apiService = RobotApiService(
        controlBaseUrl = initialRobotUrl,
        cameraWsUrl = initialCameraUrl
    )

    // Configured Custom URLs
    private val _robotUrl = MutableStateFlow(initialRobotUrl)
    val robotUrl: StateFlow<String> = _robotUrl.asStateFlow()

    private val _cameraUrl = MutableStateFlow(initialCameraUrl)
    val cameraUrl: StateFlow<String> = _cameraUrl.asStateFlow()

    // Gyroscope Telemetry State (Pitch, Roll, Yaw)
    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _roll = MutableStateFlow(0f)
    val roll: StateFlow<Float> = _roll.asStateFlow()

    private val _yaw = MutableStateFlow(0f)
    val yaw: StateFlow<Float> = _yaw.asStateFlow()

    private val _isHudVisible = MutableStateFlow(true)
    val isHudVisible: StateFlow<Boolean> = _isHudVisible.asStateFlow()

    private val _isGyroDemoMode = MutableStateFlow(false)
    val isGyroDemoMode: StateFlow<Boolean> = _isGyroDemoMode.asStateFlow()

    private var rawPitch = 0f
    private var rawRoll = 0f
    private var rawYaw = 0f
    private var pitchOffset = 0f
    private var rollOffset = 0f
    private var demoJob: Job? = null

    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Speed
    private val _currentSpeed = MutableStateFlow(128)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    // Camera frame
    private val _cameraFrame = MutableStateFlow<Bitmap?>(null)
    val cameraFrame: StateFlow<Bitmap?> = _cameraFrame.asStateFlow()

    // Current direction label
    private val _currentDirection = MutableStateFlow("STOP")
    val currentDirection: StateFlow<String> = _currentDirection.asStateFlow()

    // Uptime counter
    private val _uptimeSeconds = MutableStateFlow(0L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    // Camera state
    private val _cameraState = MutableStateFlow(CameraState.DISCONNECTED)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    // Retry counter for camera connection
    private val _cameraRetryCount = MutableStateFlow(0)
    val cameraRetryCount: StateFlow<Int> = _cameraRetryCount.asStateFlow()

    // Camera error message
    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    // Track last sent command timestamp to prevent excessive duplicate commands
    private var lastCommandTime = 0L
    private val commandThrottleMs = 100L // Allow command every 100ms

    private var cameraWebSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var connectionCheckJob: Job? = null
    private var uptimeJob: Job? = null
    private val isDecoding = AtomicBoolean(false)  // prevent frame queue buildup

    // Reusable decode options to reduce GC pressure
    private val decodeOptions = BitmapFactory.Options().apply {
        inMutable = true
        inPreferredConfig = Bitmap.Config.RGB_565  // half the memory of ARGB_8888
    }

    // Speed debounce flow
    private val speedFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    init {
        startConnectionMonitor()
        startUptimeCounter()
        collectSpeedChanges()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun collectSpeedChanges() {
        viewModelScope.launch {
            speedFlow
                .debounce(150)
                .collectLatest { speed ->
                    apiService.setSpeed(speed)
                }
        }
    }

    private fun startConnectionMonitor() {
        connectionCheckJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val connected = apiService.checkConnection()
                _isConnected.value = connected
                delay(3000)
            }
        }
    }

    private fun startUptimeCounter() {
        uptimeJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uptimeSeconds.value++
            }
        }
    }

    fun sendDirection(direction: String) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle commands but allow them through periodically for continuous movement
        if (currentTime - lastCommandTime < commandThrottleMs) {
            return
        }
        
        lastCommandTime = currentTime
        
        _currentDirection.value = when (direction) {
            "F" -> "FORWARD"
            "B" -> "BACKWARD"
            "L" -> "LEFT"
            "R" -> "RIGHT"
            else -> "STOP"
        }

        viewModelScope.launch(Dispatchers.IO) {
            when (direction) {
                "F" -> apiService.moveForward()
                "B" -> apiService.moveBackward()
                "L" -> apiService.turnLeft()
                "R" -> apiService.turnRight()
                "S" -> apiService.stop()
            }
        }
    }

    fun onDirectionRelease() {
        sendDirection("S")
    }

    fun updateSpeed(speed: Int) {
        _currentSpeed.value = speed
        speedFlow.tryEmit(speed)
    }

    fun startCameraStream() {
        if (cameraWebSocket != null) return

        _cameraRetryCount.value = 0
        _cameraError.value = null
        _cameraState.value = CameraState.CONNECTING

        connectWebSocket()
    }

    private fun connectWebSocket() {
        _cameraState.value = CameraState.CONNECTING
        _cameraError.value = null

        cameraWebSocket = apiService.connectCameraWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _cameraState.value = CameraState.STREAMING
                _cameraRetryCount.value = 0
                _cameraError.value = null
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Drop frame if previous decode is still in-flight
                // This prevents frame queue buildup which causes latency
                if (!isDecoding.compareAndSet(false, true)) return

                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        val jpegData = bytes.toByteArray()
                        val bitmap = BitmapFactory.decodeByteArray(
                            jpegData, 0, jpegData.size, decodeOptions
                        )
                        if (bitmap != null) {
                            _cameraFrame.value = bitmap
                        }
                    } finally {
                        isDecoding.set(false)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseTelemetryMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _cameraRetryCount.value++
                _cameraError.value = t.message ?: "Connection failed"
                _cameraState.value = CameraState.ERROR
                cameraWebSocket = null

                // Auto-reconnect with backoff
                scheduleReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                cameraWebSocket = null
                if (_cameraState.value == CameraState.STREAMING) {
                    // Unexpected close — reconnect
                    _cameraState.value = CameraState.ERROR
                    _cameraError.value = "Connection closed (code $code)"
                    _cameraRetryCount.value++
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            // Give the ESP32-CAM time to clean up the old connection
            // before we reconnect. Too fast = disconnect loop.
            val backoff = 1000L + (_cameraRetryCount.value.coerceAtMost(4)) * 1000L
            delay(backoff)
            if (_cameraState.value != CameraState.DISCONNECTED) {
                connectWebSocket()
            }
        }
    }

    fun stopCameraStream() {
        reconnectJob?.cancel()
        reconnectJob = null
        cameraWebSocket?.close(1000, "User stopped")
        cameraWebSocket = null
        _cameraState.value = CameraState.DISCONNECTED
        _cameraFrame.value = null
        _cameraRetryCount.value = 0
        _cameraError.value = null
    }

    fun retryCameraStream() {
        stopCameraStream()
        startCameraStream()
    }

    fun updateConnectionUrls(newRobotUrl: String, newCameraUrl: String) {
        val sanitizedRobot = RobotApiService.sanitizeControlUrl(newRobotUrl)
        val sanitizedCamera = RobotApiService.sanitizeCameraWsUrl(newCameraUrl)

        _robotUrl.value = sanitizedRobot
        _cameraUrl.value = sanitizedCamera

        prefs.edit()
            .putString("robot_url", sanitizedRobot)
            .putString("camera_url", sanitizedCamera)
            .apply()

        apiService.updateUrls(sanitizedRobot, sanitizedCamera)

        // Restart camera stream with new WebSocket URL
        retryCameraStream()

        // Trigger immediate connection check
        viewModelScope.launch(Dispatchers.IO) {
            val connected = apiService.checkConnection()
            _isConnected.value = connected
        }
    }

    fun emergencyStop() {
        lastCommandTime = 0L
        _currentDirection.value = "STOP"
        viewModelScope.launch(Dispatchers.IO) {
            repeat(3) {
                apiService.stop()
                delay(50)
            }
        }
    }

    fun toggleHud() {
        _isHudVisible.value = !_isHudVisible.value
    }

    fun setHudVisible(visible: Boolean) {
        _isHudVisible.value = visible
    }

    fun toggleDemoMode() {
        val next = !_isGyroDemoMode.value
        _isGyroDemoMode.value = next
        if (next) startGyroDemo() else stopGyroDemo()
    }

    fun zeroGyro() {
        pitchOffset = rawPitch
        rollOffset = rawRoll
        updateCalibratedValues()
    }

    fun updateGyroData(p: Float, r: Float, y: Float) {
        if (_isGyroDemoMode.value) return
        rawPitch = p
        rawRoll = r
        rawYaw = y
        updateCalibratedValues()
    }

    private fun updateCalibratedValues() {
        _pitch.value = rawPitch - pitchOffset
        _roll.value = rawRoll - rollOffset
        _yaw.value = (rawYaw % 360f + 360f) % 360f
    }

    private fun startGyroDemo() {
        demoJob?.cancel()
        demoJob = viewModelScope.launch {
            var step = 0f
            while (isActive && _isGyroDemoMode.value) {
                step += 0.05f
                val demoPitch = (sin(step * 0.7f) * 15f).toFloat()
                val demoRoll = (sin(step.toDouble()) * 25.0).toFloat()
                val demoYaw = ((step * 10f) % 360f)
                _pitch.value = demoPitch
                _roll.value = demoRoll
                _yaw.value = demoYaw
                delay(33)
            }
        }
    }

    private fun stopGyroDemo() {
        demoJob?.cancel()
        demoJob = null
        updateCalibratedValues()
    }

    private fun parseTelemetryMessage(text: String) {
        try {
            val trimmed = text.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val json = JSONObject(trimmed)
                val p = when {
                    json.has("pitch") -> json.getDouble("pitch").toFloat()
                    json.has("p") -> json.getDouble("p").toFloat()
                    json.has("x") -> json.getDouble("x").toFloat()
                    else -> null
                }
                val r = when {
                    json.has("roll") -> json.getDouble("roll").toFloat()
                    json.has("r") -> json.getDouble("r").toFloat()
                    json.has("y") -> json.getDouble("y").toFloat()
                    else -> null
                }
                val y = when {
                    json.has("yaw") -> json.getDouble("yaw").toFloat()
                    json.has("heading") -> json.getDouble("heading").toFloat()
                    json.has("z") -> json.getDouble("z").toFloat()
                    else -> null
                }
                if (p != null || r != null) {
                    updateGyroData(p ?: rawPitch, r ?: rawRoll, y ?: rawYaw)
                }
            } else if (trimmed.contains(",") || trimmed.startsWith("GYRO:", ignoreCase = true)) {
                val clean = trimmed.removePrefix("GYRO:").removePrefix("gyro:").trim()
                val parts = clean.split(",").mapNotNull { it.trim().toFloatOrNull() }
                if (parts.size >= 2) {
                    val p = parts[0]
                    val r = parts[1]
                    val y = if (parts.size >= 3) parts[2] else rawYaw
                    updateGyroData(p, r, y)
                }
            }
        } catch (_: Exception) {
            // Ignore non-telemetry messages
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraWebSocket?.close(1000, "ViewModel cleared")
        reconnectJob?.cancel()
        connectionCheckJob?.cancel()
        uptimeJob?.cancel()
        demoJob?.cancel()
    }
}


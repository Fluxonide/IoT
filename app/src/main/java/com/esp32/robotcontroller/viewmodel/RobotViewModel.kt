package com.esp32.robotcontroller.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esp32.robotcontroller.model.DeviceStatus
import com.esp32.robotcontroller.model.SensorData
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class CameraState {
    DISCONNECTED,   // Not started or stopped
    CONNECTING,     // Attempting to connect to camera
    STREAMING,      // Receiving frames
    ERROR           // Connection failed, will retry
}

class RobotViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("robot_controller_prefs", Context.MODE_PRIVATE)

    private val initialMotorUrl = RobotApiService.sanitizeHttpUrl(
        prefs.getString("motor_url", "http://10.78.24.50") ?: "http://10.78.24.50",
        "http://10.78.24.50"
    )
    private val initialSensorUrl = RobotApiService.sanitizeHttpUrl(
        prefs.getString("sensor_url", "http://10.78.24.51") ?: "http://10.78.24.51",
        "http://10.78.24.51"
    )
    private val initialCameraUrl = RobotApiService.sanitizeHttpUrl(
        prefs.getString("camera_url", "http://10.78.24.60") ?: "http://10.78.24.60",
        "http://10.78.24.60"
    )

    private val apiService = RobotApiService(
        motorBaseUrl = initialMotorUrl,
        sensorBaseUrl = initialSensorUrl,
        cameraBaseUrl = initialCameraUrl
    )

    // Configured Custom URLs
    private val _motorUrl = MutableStateFlow(initialMotorUrl)
    val motorUrl: StateFlow<String> = _motorUrl.asStateFlow()
    // For backwards compatibility
    val robotUrl: StateFlow<String> = _motorUrl

    private val _sensorUrl = MutableStateFlow(initialSensorUrl)
    val sensorUrl: StateFlow<String> = _sensorUrl.asStateFlow()

    private val _cameraUrl = MutableStateFlow(initialCameraUrl)
    val cameraUrl: StateFlow<String> = _cameraUrl.asStateFlow()

    // Device Online / Offline Status
    private val _isMotorOnline = MutableStateFlow(false)
    val isMotorOnline: StateFlow<Boolean> = _isMotorOnline.asStateFlow()
    val isConnected: StateFlow<Boolean> = _isMotorOnline // alias

    private val _isSensorOnline = MutableStateFlow(false)
    val isSensorOnline: StateFlow<Boolean> = _isSensorOnline.asStateFlow()

    private val _isCameraOnline = MutableStateFlow(false)
    val isCameraOnline: StateFlow<Boolean> = _isCameraOnline.asStateFlow()

    // Device Details
    private val _motorStatus = MutableStateFlow(DeviceStatus(ip = "10.78.24.50"))
    val motorStatus: StateFlow<DeviceStatus> = _motorStatus.asStateFlow()

    private val _sensorStatus = MutableStateFlow(DeviceStatus(ip = "10.78.24.51"))
    val sensorStatus: StateFlow<DeviceStatus> = _sensorStatus.asStateFlow()

    private val _cameraStatus = MutableStateFlow(DeviceStatus(ip = "10.78.24.60"))
    val cameraStatus: StateFlow<DeviceStatus> = _cameraStatus.asStateFlow()

    // Live Sensor Telemetry
    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    // Historical Sensor Graphs (capped at 60 points, matching esp32-robot-dashboard.html)
    private val maxPoints = 60
    private val _distanceHistory = MutableStateFlow<List<Float>>(emptyList())
    val distanceHistory: StateFlow<List<Float>> = _distanceHistory.asStateFlow()

    private val _temperatureHistory = MutableStateFlow<List<Float>>(emptyList())
    val temperatureHistory: StateFlow<List<Float>> = _temperatureHistory.asStateFlow()

    private val _humidityHistory = MutableStateFlow<List<Float>>(emptyList())
    val humidityHistory: StateFlow<List<Float>> = _humidityHistory.asStateFlow()

    private val _mqHistory = MutableStateFlow<List<Float>>(emptyList())
    val mqHistory: StateFlow<List<Float>> = _mqHistory.asStateFlow()

    private val _waterHistory = MutableStateFlow<List<Float>>(emptyList())
    val waterHistory: StateFlow<List<Float>> = _waterHistory.asStateFlow()

    // Motor Speed (default 180 as in HTML)
    private val _currentSpeed = MutableStateFlow(180)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    // Servo Pan Angle (20° to 160°, default 90°)
    private val _servoAngle = MutableStateFlow(90)
    val servoAngle: StateFlow<Int> = _servoAngle.asStateFlow()
    val isUserAdjustingServo = AtomicBoolean(false)

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

    private val _cameraRetryCount = MutableStateFlow(0)
    val cameraRetryCount: StateFlow<Int> = _cameraRetryCount.asStateFlow()

    private val _cameraError = MutableStateFlow<String?>(null)
    val cameraError: StateFlow<String?> = _cameraError.asStateFlow()

    // Commands & Debounce Flows
    private var lastCommandTime = 0L
    private val commandThrottleMs = 100L
    private val isDecoding = AtomicBoolean(false)
    private val decodeOptions = BitmapFactory.Options().apply {
        inMutable = true
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    private val speedFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val servoFlow = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private var cameraWebSocket: WebSocket? = null
    private var httpMjpegJob: Job? = null
    private var reconnectJob: Job? = null
    private var sensorPollJob: Job? = null
    private var statusPollJob: Job? = null
    private var uptimeJob: Job? = null

    init {
        startUptimeCounter()
        startSensorPolling()
        startDeviceStatusPolling()
        collectSpeedChanges()
        collectServoChanges()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun collectSpeedChanges() {
        viewModelScope.launch {
            speedFlow.debounce(150).collectLatest { speed ->
                apiService.setSpeed(speed)
            }
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun collectServoChanges() {
        viewModelScope.launch {
            servoFlow.debounce(50).collectLatest { angle ->
                apiService.setServo(angle)
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

    /* ================= Sensor Polling (1000ms, matching HTML) ================= */

    private fun startSensorPolling() {
        sensorPollJob?.cancel()
        sensorPollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val result = apiService.getSensorData()
                when (result) {
                    is RobotApiService.Result.Success -> {
                        val data = result.data
                        _isSensorOnline.value = true
                        _sensorData.value = data

                        // Update historical arrays
                        _distanceHistory.value = pushHistory(_distanceHistory.value, data.distance)
                        _temperatureHistory.value = pushHistory(_temperatureHistory.value, data.temperature)
                        _humidityHistory.value = pushHistory(_humidityHistory.value, data.humidity)
                        _mqHistory.value = pushHistory(_mqHistory.value, data.mq)
                        _waterHistory.value = pushHistory(_waterHistory.value, data.water)
                    }
                    is RobotApiService.Result.Error -> {
                        _isSensorOnline.value = false
                    }
                }
                delay(1000) // 1s interval as in esp32-robot-dashboard.html
            }
        }
    }

    private fun pushHistory(list: List<Float>, value: Float): List<Float> {
        val next = ArrayList(list)
        next.add(value)
        if (next.size > maxPoints) {
            next.removeAt(0)
        }
        return next
    }

    /* ================= 2000ms Status Polling ================= */

    private fun startDeviceStatusPolling() {
        statusPollJob?.cancel()
        statusPollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                // Motor status
                when (val motorRes = apiService.getMotorStatus()) {
                    is RobotApiService.Result.Success -> {
                        _isMotorOnline.value = true
                        _motorStatus.value = motorRes.data
                    }
                    is RobotApiService.Result.Error -> {
                        _isMotorOnline.value = false
                    }
                }

                // Camera status
                when (val camRes = apiService.getCameraStatus()) {
                    is RobotApiService.Result.Success -> {
                        _isCameraOnline.value = true
                        _cameraStatus.value = camRes.data
                        val s = camRes.data.servo.removeSuffix("°").toIntOrNull()
                        if (s != null && !isUserAdjustingServo.get()) {
                            _servoAngle.value = s
                        }
                    }
                    is RobotApiService.Result.Error -> {
                        _isCameraOnline.value = false
                    }
                }

                delay(2000)
            }
        }
    }

    /* ================= Motor & Direction Controls ================= */

    fun sendDirection(direction: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCommandTime < commandThrottleMs) return
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

    /* ================= Servo Pan Controls ================= */

    fun setServoAngle(angle: Int) {
        val clamped = angle.coerceIn(20, 160)
        _servoAngle.value = clamped
        servoFlow.tryEmit(clamped)
    }

    fun servoLeft() {
        _servoAngle.value = 20
        viewModelScope.launch(Dispatchers.IO) {
            apiService.servoLeft()
        }
    }

    fun servoCenter() {
        _servoAngle.value = 90
        viewModelScope.launch(Dispatchers.IO) {
            apiService.servoCenter()
        }
    }

    fun servoRight() {
        _servoAngle.value = 160
        viewModelScope.launch(Dispatchers.IO) {
            apiService.servoRight()
        }
    }

    /* ================= Camera Stream ================= */

    fun startCameraStream() {
        if (cameraWebSocket != null || httpMjpegJob != null) return

        _cameraRetryCount.value = 0
        _cameraError.value = null
        _cameraState.value = CameraState.CONNECTING

        val camUrl = _cameraUrl.value
        if (camUrl.startsWith("ws://", ignoreCase = true) || camUrl.startsWith("wss://", ignoreCase = true)) {
            connectWebSocket()
        } else {
            connectHttpMjpeg()
        }
    }

    private fun connectHttpMjpeg() {
        httpMjpegJob?.cancel()
        httpMjpegJob = viewModelScope.launch(Dispatchers.IO) {
            _cameraState.value = CameraState.CONNECTING
            val streamUrl = apiService.getCameraStreamUrl()
            val mjpegClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            try {
                val request = Request.Builder().url(streamUrl).build()
                val response = mjpegClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }

                _cameraState.value = CameraState.STREAMING
                _isCameraOnline.value = true
                _cameraError.value = null

                val inputStream = BufferedInputStream(response.body!!.byteStream())
                val buffer = ByteArray(4096)
                val frameBuffer = ByteArrayOutputStream()
                var inFrame = false
                var prevByte = 0

                while (isActive) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    for (i in 0 until bytesRead) {
                        val currentByte = buffer[i].toInt() and 0xFF
                        if (!inFrame) {
                            if (prevByte == 0xFF && currentByte == 0xD8) {
                                inFrame = true
                                frameBuffer.reset()
                                frameBuffer.write(0xFF)
                                frameBuffer.write(0xD8)
                            }
                        } else {
                            frameBuffer.write(currentByte)
                            if (prevByte == 0xFF && currentByte == 0xD9) {
                                inFrame = false
                                val jpeg = frameBuffer.toByteArray()
                                if (isDecoding.compareAndSet(false, true)) {
                                    val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, decodeOptions)
                                    if (bitmap != null) {
                                        _cameraFrame.value = bitmap
                                    }
                                    isDecoding.set(false)
                                }
                            }
                        }
                        prevByte = currentByte
                    }
                }
            } catch (e: Exception) {
                _cameraRetryCount.value++
                _cameraError.value = e.message ?: "Stream failed"
                _cameraState.value = CameraState.ERROR
                scheduleReconnect()
            }
        }
    }

    private fun connectWebSocket() {
        _cameraState.value = CameraState.CONNECTING
        _cameraError.value = null

        cameraWebSocket = apiService.connectCameraWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _cameraState.value = CameraState.STREAMING
                _isCameraOnline.value = true
                _cameraRetryCount.value = 0
                _cameraError.value = null
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
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
                // Text telemetry via WebSocket is unused
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _cameraRetryCount.value++
                _cameraError.value = t.message ?: "Connection failed"
                _cameraState.value = CameraState.ERROR
                cameraWebSocket = null
                scheduleReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                cameraWebSocket = null
                if (_cameraState.value == CameraState.STREAMING) {
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
            val backoff = 1000L + (_cameraRetryCount.value.coerceAtMost(4)) * 1000L
            delay(backoff)
            if (_cameraState.value != CameraState.DISCONNECTED) {
                startCameraStream()
            }
        }
    }

    fun stopCameraStream() {
        reconnectJob?.cancel()
        reconnectJob = null
        httpMjpegJob?.cancel()
        httpMjpegJob = null
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

    fun updateConnectionUrls(newMotorUrl: String, newSensorUrl: String, newCameraUrl: String) {
        val sanitizedMotor = RobotApiService.sanitizeHttpUrl(newMotorUrl, "http://10.78.24.50")
        val sanitizedSensor = RobotApiService.sanitizeHttpUrl(newSensorUrl, "http://10.78.24.51")
        val sanitizedCamera = RobotApiService.sanitizeHttpUrl(newCameraUrl, "http://10.78.24.60")

        _motorUrl.value = sanitizedMotor
        _sensorUrl.value = sanitizedSensor
        _cameraUrl.value = sanitizedCamera

        prefs.edit()
            .putString("motor_url", sanitizedMotor)
            .putString("sensor_url", sanitizedSensor)
            .putString("camera_url", sanitizedCamera)
            // Backwards compatibility
            .putString("robot_url", sanitizedMotor)
            .apply()

        apiService.updateUrls(sanitizedMotor, sanitizedSensor, sanitizedCamera)
        retryCameraStream()
        startSensorPolling()
        startDeviceStatusPolling()
    }

    // Backwards compatibility overload
    fun updateConnectionUrls(newRobotUrl: String, newCameraUrl: String) {
        updateConnectionUrls(newRobotUrl, _sensorUrl.value, newCameraUrl)
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

    override fun onCleared() {
        super.onCleared()
        stopCameraStream()
        sensorPollJob?.cancel()
        statusPollJob?.cancel()
        uptimeJob?.cancel()
    }
}


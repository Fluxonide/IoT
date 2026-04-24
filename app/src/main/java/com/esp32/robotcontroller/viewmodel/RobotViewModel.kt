package com.esp32.robotcontroller.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
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
import java.util.concurrent.atomic.AtomicBoolean

enum class CameraState {
    DISCONNECTED,   // Not started or stopped
    CONNECTING,     // Attempting to connect to camera
    STREAMING,      // Receiving frames
    ERROR           // Connection failed, will retry
}

class RobotViewModel : ViewModel() {
    private val apiService = RobotApiService()

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
            // Fast backoff: 500ms, 1s, 1.5s... capped at 2.5s
            val backoff = (_cameraRetryCount.value.coerceAtMost(5)) * 500L
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
        cameraWebSocket?.close(1000, "ViewModel cleared")
        reconnectJob?.cancel()
        connectionCheckJob?.cancel()
        uptimeJob?.cancel()
    }
}


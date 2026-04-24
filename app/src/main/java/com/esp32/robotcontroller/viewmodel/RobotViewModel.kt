package com.esp32.robotcontroller.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esp32.robotcontroller.network.RobotApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

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

    // Stream active
    private val _isStreamActive = MutableStateFlow(false)
    val isStreamActive: StateFlow<Boolean> = _isStreamActive.asStateFlow()

    // Anti-flood: track last sent command
    private var lastSentCommand: String? = null

    private var streamJob: Job? = null
    private var connectionCheckJob: Job? = null
    private var uptimeJob: Job? = null

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
        if (direction == lastSentCommand) return
        lastSentCommand = direction
        _currentDirection.value = when (direction) {
            "F" -> "FORWARD"
            "B" -> "BACKWARD"
            "L" -> "LEFT"
            "R" -> "RIGHT"
            else -> "STOP"
        }

        viewModelScope.launch {
            when (direction) {
                "F" -> apiService.moveForward()
                "B" -> apiService.moveBackward()
                "L" -> apiService.turnLeft()
                "R" -> apiService.turnRight()
                "S" -> {
                    apiService.stop()
                    lastSentCommand = null
                }
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
        if (streamJob?.isActive == true) return

        streamJob = viewModelScope.launch(Dispatchers.IO) {
            _isStreamActive.value = true
            while (isActive) {
                when (val result = apiService.openMjpegStream()) {
                    is RobotApiService.Result.Success -> {
                        try {
                            decodeMjpegStream(result.data)
                        } catch (_: Exception) {
                            // Stream interrupted, retry
                        }
                    }
                    is RobotApiService.Result.Error -> {
                        delay(2000)
                    }
                }
            }
            _isStreamActive.value = false
        }
    }

    fun stopCameraStream() {
        streamJob?.cancel()
        streamJob = null
        _isStreamActive.value = false
        _cameraFrame.value = null
    }

    private suspend fun decodeMjpegStream(inputStream: InputStream) {
        val buffer = ByteArray(8192)
        val jpegBuffer = ByteArrayOutputStream()
        var inJpeg = false
        var prev = 0

        while (currentCoroutineContext().isActive) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break

            for (i in 0 until bytesRead) {
                val current = buffer[i].toInt() and 0xFF
                if (!inJpeg) {
                    if (prev == 0xFF && current == 0xD8) {
                        inJpeg = true
                        jpegBuffer.reset()
                        jpegBuffer.write(0xFF)
                        jpegBuffer.write(0xD8)
                    }
                } else {
                    jpegBuffer.write(current)
                    if (prev == 0xFF && current == 0xD9) {
                        inJpeg = false
                        val jpegData = jpegBuffer.toByteArray()
                        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
                        if (bitmap != null) {
                            _cameraFrame.value = bitmap
                        }
                    }
                }
                prev = current
            }
        }
        inputStream.close()
    }

    fun emergencyStop() {
        lastSentCommand = null
        _currentDirection.value = "STOP"
        viewModelScope.launch {
            repeat(3) {
                apiService.stop()
                delay(50)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        connectionCheckJob?.cancel()
        uptimeJob?.cancel()
    }
}

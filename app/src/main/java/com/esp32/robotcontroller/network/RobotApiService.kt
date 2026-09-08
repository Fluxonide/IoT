package com.esp32.robotcontroller.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class RobotApiService(
    private var controlBaseUrl: String = "http://192.168.137.50",
    private var cameraWsUrl: String = "ws://192.168.137.60/ws"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(5, TimeUnit.SECONDS)  // faster disconnect detection
        .build()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    fun updateUrls(newControlUrl: String, newCameraWsUrl: String) {
        this.controlBaseUrl = sanitizeControlUrl(newControlUrl)
        this.cameraWsUrl = sanitizeCameraWsUrl(newCameraWsUrl)
    }

    fun getControlUrl(): String = controlBaseUrl
    fun getCameraWsUrl(): String = cameraWsUrl

    suspend fun sendCommand(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
            val request = Request.Builder()
                .url("${controlBaseUrl.trimEnd('/')}$cleanEndpoint")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.Success(response.body?.string() ?: "OK")
            } else {
                Result.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun moveForward() = sendCommand("/F")
    suspend fun moveBackward() = sendCommand("/B")
    suspend fun turnLeft() = sendCommand("/L")
    suspend fun turnRight() = sendCommand("/R")
    suspend fun stop() = sendCommand("/S")
    suspend fun setSpeed(value: Int) = sendCommand("/speed?v=$value")

    /**
     * Connect to the ESP32-CAM WebSocket that pushes JPEG frames as binary messages.
     * Returns the WebSocket instance so the caller can close it when done.
     */
    fun connectCameraWebSocket(listener: WebSocketListener): WebSocket {
        val request = Request.Builder()
            .url(cameraWsUrl)
            .build()
        return wsClient.newWebSocket(request, listener)
    }

    fun checkConnection(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${controlBaseUrl.trimEnd('/')}/S")
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        fun sanitizeControlUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return "http://192.168.137.50"
            val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                "http://$trimmed"
            } else {
                trimmed
            }
            return withScheme.trimEnd('/')
        }

        fun sanitizeCameraWsUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return "ws://192.168.137.60/ws"
            var result = trimmed
            if (result.startsWith("http://", ignoreCase = true)) {
                result = "ws://" + result.substring(7)
            } else if (result.startsWith("https://", ignoreCase = true)) {
                result = "wss://" + result.substring(8)
            } else if (!result.startsWith("ws://", ignoreCase = true) &&
                !result.startsWith("wss://", ignoreCase = true)
            ) {
                result = "ws://$result"
            }
            val withoutScheme = result.substringAfter("://")
            if (!withoutScheme.contains("/")) {
                result = "$result/ws"
            }
            return result
        }
    }
}


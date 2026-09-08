package com.esp32.robotcontroller.network

import com.esp32.robotcontroller.model.DeviceStatus
import com.esp32.robotcontroller.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

class RobotApiService(
    private var motorBaseUrl: String = "http://10.78.24.50",
    private var sensorBaseUrl: String = "http://10.78.24.51",
    private var cameraBaseUrl: String = "http://10.78.24.60"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .build()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    fun updateUrls(newMotorUrl: String, newSensorUrl: String, newCameraUrl: String) {
        this.motorBaseUrl = sanitizeHttpUrl(newMotorUrl, "http://10.78.24.50")
        this.sensorBaseUrl = sanitizeHttpUrl(newSensorUrl, "http://10.78.24.51")
        this.cameraBaseUrl = sanitizeHttpUrl(newCameraUrl, "http://10.78.24.60")
    }

    fun getMotorUrl(): String = motorBaseUrl
    fun getSensorUrl(): String = sensorBaseUrl
    fun getCameraUrl(): String = cameraBaseUrl

    /* ================= Motor Commands ================= */

    suspend fun sendMotorCommand(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
            val request = Request.Builder()
                .url("${motorBaseUrl.trimEnd('/')}$cleanEndpoint")
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

    suspend fun moveForward() = sendMotorCommand("/F")
    suspend fun moveBackward() = sendMotorCommand("/B")
    suspend fun turnLeft() = sendMotorCommand("/L")
    suspend fun turnRight() = sendMotorCommand("/R")
    suspend fun stop() = sendMotorCommand("/S")
    suspend fun setSpeed(value: Int) = sendMotorCommand("/speed?v=$value")

    suspend fun getMotorStatus(): Result<DeviceStatus> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${motorBaseUrl.trimEnd('/')}/status")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string().orEmpty()
                val json = JSONObject(jsonStr)
                val ip = json.optString("ip", motorBaseUrl.removePrefix("http://").removePrefix("https://").substringBefore("/"))
                val rssi = if (json.has("rssi")) "${json.opt("rssi")} dBm" else "--"
                val channel = if (json.has("channel")) json.optString("channel") else "--"
                Result.Success(DeviceStatus(isOnline = true, ip = ip, rssi = rssi, channel = channel))
            } else {
                Result.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    /* ================= Sensor Commands ================= */

    suspend fun getSensorData(): Result<SensorData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${sensorBaseUrl.trimEnd('/')}/data")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string().orEmpty()
                val json = JSONObject(jsonStr)

                val accelObj = json.optJSONObject("accel")
                val gyroObj = json.optJSONObject("gyro")

                val sensorData = SensorData(
                    distance = json.optDouble("distance", 0.0).toFloat(),
                    temperature = json.optDouble("temperature", 0.0).toFloat(),
                    humidity = json.optDouble("humidity", 0.0).toFloat(),
                    mq = json.optDouble("mq", 0.0).toFloat(),
                    water = json.optDouble("water", 0.0).toFloat(),
                    accelMagnitude = json.optDouble("accelMagnitude", 0.0).toFloat(),
                    gyroMagnitude = json.optDouble("gyroMagnitude", 0.0).toFloat(),
                    ax = accelObj?.optDouble("x", 0.0)?.toFloat() ?: 0f,
                    ay = accelObj?.optDouble("y", 0.0)?.toFloat() ?: 0f,
                    az = accelObj?.optDouble("z", 0.0)?.toFloat() ?: 0f,
                    gx = gyroObj?.optDouble("x", 0.0)?.toFloat() ?: 0f,
                    gy = gyroObj?.optDouble("y", 0.0)?.toFloat() ?: 0f,
                    gz = gyroObj?.optDouble("z", 0.0)?.toFloat() ?: 0f
                )
                Result.Success(sensorData)
            } else {
                Result.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Sensor error")
        }
    }

    /* ================= Camera & Servo Commands ================= */

    suspend fun setServo(angle: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val clamped = angle.coerceIn(20, 160)
            val request = Request.Builder()
                .url("${cameraBaseUrl.trimEnd('/')}/servo?angle=$clamped")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.Success(response.body?.string() ?: "OK")
            } else {
                Result.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Servo error")
        }
    }

    suspend fun getCameraStatus(): Result<DeviceStatus> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${cameraBaseUrl.trimEnd('/')}/status")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string().orEmpty()
                val json = JSONObject(jsonStr)
                val ip = json.optString("ip", cameraBaseUrl.removePrefix("http://").removePrefix("https://").substringBefore("/"))
                val rssi = if (json.has("rssi")) "${json.opt("rssi")} dBm" else "--"
                val servo = if (json.has("servo")) "${json.opt("servo")}°" else "--"
                Result.Success(DeviceStatus(isOnline = true, ip = ip, rssi = rssi, servo = servo))
            } else {
                Result.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Camera error")
        }
    }

    fun connectCameraWebSocket(listener: WebSocketListener): WebSocket {
        val wsUrl = sanitizeCameraWsUrl(cameraBaseUrl)
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        return wsClient.newWebSocket(request, listener)
    }

    fun getCameraStreamUrl(): String {
        val base = cameraBaseUrl.trimEnd('/')
        // Standard ESP32-CAM stream is either base:81/stream or base/stream
        return if (base.contains(":81")) {
            if (base.endsWith("/stream")) base else "$base/stream"
        } else {
            // Check if user already provided path
            if (base.endsWith("/stream") || base.endsWith(".mjpg")) base else "$base:81/stream"
        }
    }

    companion object {
        fun sanitizeHttpUrl(url: String, default: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return default
            val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                "http://$trimmed"
            } else {
                trimmed
            }
            return withScheme.trimEnd('/')
        }

        fun sanitizeControlUrl(url: String): String = sanitizeHttpUrl(url, "http://10.78.24.50")

        fun sanitizeCameraWsUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return "ws://10.78.24.60/ws"
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

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
    private var motorBaseUrl: String = "http://172.17.40.60",
    private var sensorBaseUrl: String = "http://172.17.40.60",
    private var cameraBaseUrl: String = "http://172.17.40.60",
    private var cameraStreamUrl: String = "http://172.17.40.60:82/stream",
    private var wsUrl: String = "ws://172.17.40.60:81"
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

    fun updateUrls(
        newMotorUrl: String,
        newSensorUrl: String,
        newCameraUrl: String,
        newStreamUrl: String = "",
        newWsUrl: String = ""
    ) {
        this.motorBaseUrl = sanitizeHttpUrl(newMotorUrl, "http://172.17.40.60")
        this.sensorBaseUrl = sanitizeHttpUrl(newSensorUrl, "http://172.17.40.60")
        this.cameraBaseUrl = sanitizeHttpUrl(newCameraUrl, "http://172.17.40.60")
        if (newStreamUrl.isNotBlank()) {
            this.cameraStreamUrl = newStreamUrl.trim()
        }
        if (newWsUrl.isNotBlank()) {
            this.wsUrl = sanitizeCameraWsUrl(newWsUrl)
        }
    }

    fun getMotorUrl(): String = motorBaseUrl
    fun getSensorUrl(): String = sensorBaseUrl
    fun getCameraUrl(): String = cameraBaseUrl
    fun getStreamUrl(): String = cameraStreamUrl
    fun getWsUrl(): String = wsUrl

    /* ================= Motor Commands ================= */

    suspend fun sendMotorCommand(endpoint: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
            val request = Request.Builder()
                .url("${motorBaseUrl.trimEnd('/')}$cleanEndpoint")
                .header("Connection", "close")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.Success(response.body?.string().orEmpty().ifEmpty { "OK" })
                } else {
                    Result.Error("HTTP ${response.code}")
                }
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
                .header("Connection", "close")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string().orEmpty()
                    val json = JSONObject(jsonStr)
                    val device = json.optString("device", "ESP32 Motor Node")
                    val ip = json.optString("ip", motorBaseUrl.removePrefix("http://").removePrefix("https://").substringBefore("/"))
                    val gateway = json.optString("gateway", "--")
                    val ssid = json.optString("ssid", "--")
                    val rssi = if (json.has("rssi")) "${json.opt("rssi")} dBm" else "--"
                    val channel = json.optString("channel", "--")
                    val uptime = if (json.has("uptime")) "${json.opt("uptime")} s" else "--"
                    val motorSpeed = json.optString("motorSpeed", "--")
                    Result.Success(
                        DeviceStatus(
                            isOnline = true,
                            device = device,
                            ip = ip,
                            gateway = gateway,
                            ssid = ssid,
                            rssi = rssi,
                            channel = channel,
                            uptime = uptime,
                            motorSpeed = motorSpeed
                        )
                    )
                } else {
                    Result.Error("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    /* ================= Sensor Commands ================= */

    suspend fun getSensorData(): Result<SensorData> = withContext(Dispatchers.IO) {
        try {
            val primaryUrl = "${sensorBaseUrl.trimEnd('/')}/data"
            var jsonStr: String? = null
            try {
                val request = Request.Builder().url(primaryUrl).header("Connection", "close").get().build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        jsonStr = resp.body?.string()
                    }
                }
            } catch (_: Exception) {}

            if (jsonStr == null && sensorBaseUrl != motorBaseUrl) {
                val fallbackUrl = "${motorBaseUrl.trimEnd('/')}/data"
                try {
                    val request = Request.Builder().url(fallbackUrl).header("Connection", "close").get().build()
                    client.newCall(request).execute().use { resp ->
                        if (resp.isSuccessful) {
                            jsonStr = resp.body?.string()
                        }
                    }
                } catch (_: Exception) {}
            }

            if (jsonStr != null) {
                val json = JSONObject(jsonStr!!)
                val sensorData = SensorData(
                    distance = json.optDouble("distance", 0.0).toFloat(),
                    temperature = json.optDouble("temperature", 0.0).toFloat(),
                    humidity = json.optDouble("humidity", 0.0).toFloat(),
                    mq = json.optDouble("mq", 0.0).toFloat(),
                    water = json.optDouble("water", 0.0).toFloat()
                )
                Result.Success(sensorData)
            } else {
                Result.Error("Sensor fetch failed")
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
                .header("Connection", "close")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.Success(response.body?.string().orEmpty().ifEmpty { "OK" })
                } else {
                    Result.Error("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Servo error")
        }
    }

    suspend fun servoLeft(): Result<String> = withContext(Dispatchers.IO) {
        sendCameraCommandWithFallback("/left", 20)
    }

    suspend fun servoCenter(): Result<String> = withContext(Dispatchers.IO) {
        sendCameraCommandWithFallback("/center", 90)
    }

    suspend fun servoRight(): Result<String> = withContext(Dispatchers.IO) {
        sendCameraCommandWithFallback("/right", 160)
    }

    private suspend fun sendCameraCommandWithFallback(endpoint: String, fallbackAngle: Int): Result<String> {
        val cleanEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        val primaryResult = try {
            val request = Request.Builder()
                .url("${cameraBaseUrl.trimEnd('/')}$cleanEndpoint")
                .header("Connection", "close")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.Success(response.body?.string().orEmpty().ifEmpty { "OK" })
                else Result.Error("HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Servo error")
        }

        return if (primaryResult is Result.Success) {
            primaryResult
        } else {
            setServo(fallbackAngle)
        }
    }

    suspend fun getCameraStatus(): Result<DeviceStatus> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${cameraBaseUrl.trimEnd('/')}/status")
                .header("Connection", "close")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string().orEmpty()
                    val json = JSONObject(jsonStr)
                    val device = json.optString("device", "ESP32 Camera Node")
                    val ip = json.optString("ip", cameraBaseUrl.removePrefix("http://").removePrefix("https://").substringBefore("/"))
                    val gateway = json.optString("gateway", "--")
                    val ssid = json.optString("ssid", "--")
                    val rssi = if (json.has("rssi")) "${json.opt("rssi")} dBm" else "--"
                    val servo = if (json.has("servoAngle")) "${json.opt("servoAngle")}°"
                        else if (json.has("servo")) "${json.opt("servo")}°" else "--"
                    Result.Success(
                        DeviceStatus(
                            isOnline = true,
                            device = device,
                            ip = ip,
                            gateway = gateway,
                            ssid = ssid,
                            rssi = rssi,
                            servo = servo
                        )
                    )
                } else {
                    Result.Error("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Camera error")
        }
    }

    fun connectCameraWebSocket(listener: WebSocketListener): WebSocket {
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        return wsClient.newWebSocket(request, listener)
    }

    fun getCameraStreamUrl(): String = cameraStreamUrl

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

        fun sanitizeControlUrl(url: String): String = sanitizeHttpUrl(url, "http://172.17.40.60")

        fun sanitizeCameraWsUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return "ws://172.17.40.60:81"
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
            return result
        }
    }
}

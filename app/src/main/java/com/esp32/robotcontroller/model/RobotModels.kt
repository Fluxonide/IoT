package com.esp32.robotcontroller.model

data class SensorData(
    val distance: Float = 0f,
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val mq: Float = 0f,
    val water: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceStatus(
    val isOnline: Boolean = false,
    val device: String = "--",
    val ip: String = "--",
    val gateway: String = "--",
    val ssid: String = "--",
    val rssi: String = "--",
    val channel: String = "--",
    val uptime: String = "--",
    val motorSpeed: String = "--",
    val servo: String = "--"
)

enum class TrendDirection(val symbol: String, val label: String) {
    INCREASING("↑", "Increasing"),
    DECREASING("↓", "Decreasing"),
    STABLE("→", "Stable"),
    UNKNOWN("--", "Unknown")
}

sealed class PredictionState {
    data class CollectingData(val currentSamples: Int = 0, val requiredSamples: Int = 5) : PredictionState()

    data class Ready(
        val nextValue: Float,
        val future30s: Float,
        val future60s: Float,
        val trend: TrendDirection,
        val slope: Float,
        val futurePoints: List<Float> = emptyList()
    ) : PredictionState()

    data class Error(val message: String = "Unavailable") : PredictionState()
}



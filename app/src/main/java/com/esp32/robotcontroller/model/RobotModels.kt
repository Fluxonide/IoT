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


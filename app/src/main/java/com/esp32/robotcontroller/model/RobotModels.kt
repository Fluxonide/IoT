package com.esp32.robotcontroller.model

data class SensorData(
    val distance: Float = 0f,
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val mq: Float = 0f,
    val water: Float = 0f,
    val accelMagnitude: Float = 0f,
    val gyroMagnitude: Float = 0f,
    val ax: Float = 0f,
    val ay: Float = 0f,
    val az: Float = 0f,
    val gx: Float = 0f,
    val gy: Float = 0f,
    val gz: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceStatus(
    val isOnline: Boolean = false,
    val ip: String = "--",
    val rssi: String = "--",
    val channel: String = "--",
    val servo: String = "--"
)

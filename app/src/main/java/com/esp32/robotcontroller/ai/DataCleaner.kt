package com.esp32.robotcontroller.ai

import kotlin.math.abs

/**
 * Validates and cleans raw sensor data before feeding into the ML prediction buffer.
 *
 * Logical pipeline:
 * RAW DATA -> Validation -> Cleaning -> Recent valid readings -> AI prediction
 *
 * Rules:
 * 1. Checks numeric / finite (not NaN, not infinite).
 * 2. Checks physical sensor range bounds (DHT11, MQ, HW-038, HC-SR04).
 * 3. Enforces valid positive, monotonically non-decreasing timestamps.
 * 4. Filters gross outliers/spikes while strictly preserving real sensor shifts.
 * 5. Does NOT mutate raw SensorData.
 */
object DataCleaner {

    // Valid physical sensor limits
    private val TEMP_RANGE = -10.0f..85.0f      // DHT11 rated 0-50°C, tolerate edge room variations
    private val HUMIDITY_RANGE = 0.0f..100.0f   // Relative humidity %
    private val DISTANCE_RANGE = 2.0f..450.0f   // HC-SR04 ultrasonic range in cm
    private val MQ_RANGE = 0.0f..4095.0f        // Gas sensor ADC range (ESP32 12-bit ADC)
    private val WATER_RANGE = 0.0f..4095.0f     // Water level sensor ADC range

    // Maximum sudden step jump permitted per 1s interval (to discard hardware glitches/floating ADC pin noise)
    private const val MAX_TEMP_STEP = 15.0f     // 15°C jump in 1 second is physically unrealistic
    private const val MAX_HUMIDITY_STEP = 40.0f // 40% jump in 1 second is unrealistic

    private var lastValidTimestamp: Long = 0L

    /**
     * Checks if a numeric sensor reading is valid and within physical bounds.
     */
    fun isNumericValid(value: Float, range: ClosedFloatingPointRange<Float>): Boolean {
        return !value.isNaN() && !value.isInfinite() && value in range
    }

    /**
     * Checks if timestamp is positive and >= last observed timestamp.
     */
    @Synchronized
    fun isTimestampValid(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false
        // Allow slight tolerance (up to 5s backwards due to NTP sync)
        if (lastValidTimestamp > 0L && timestamp < lastValidTimestamp - 5000L) {
            return false
        }
        if (timestamp > lastValidTimestamp) {
            lastValidTimestamp = timestamp
        }
        return true
    }

    /**
     * Validates and cleans a sensor value against recent readings buffer.
     * Returns the cleaned Float if valid, or null if the reading should be rejected.
     */
    fun validateAndClean(
        sensorKey: String,
        rawValue: Float,
        recentBuffer: List<Float>
    ): Float? {
        // Step 1: Numeric check
        if (rawValue.isNaN() || rawValue.isInfinite()) {
            return null
        }

        // Step 2: Physical sensor range validation
        val range = when (sensorKey) {
            "temperature" -> TEMP_RANGE
            "humidity" -> HUMIDITY_RANGE
            "distance" -> DISTANCE_RANGE
            "mq" -> MQ_RANGE
            "water" -> WATER_RANGE
            else -> 0f..Float.MAX_VALUE
        }

        if (rawValue !in range) {
            return null
        }

        // Step 3: Outlier check against recent buffer
        // Note: Do NOT aggressively filter real sensor changes (e.g. smoke detection spike).
        // Only discard physically impossible transitions.
        if (recentBuffer.isNotEmpty()) {
            val lastValue = recentBuffer.last()
            when (sensorKey) {
                "temperature" -> {
                    if (abs(rawValue - lastValue) > MAX_TEMP_STEP) return null
                }
                "humidity" -> {
                    if (abs(rawValue - lastValue) > MAX_HUMIDITY_STEP) return null
                }
                "distance" -> {
                    // Distance sensor HC-SR04 can read 0 or >450 on timeout echo
                    if (rawValue < 2f || rawValue > 450f) return null
                }
                // MQ and water sensors can have rapid legitimate changes when detecting gas/smoke or water,
                // so we only enforce range boundaries.
            }
        }

        return rawValue
    }
}

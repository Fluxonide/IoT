package com.esp32.robotcontroller.ai

import com.esp32.robotcontroller.model.PredictionState
import com.esp32.robotcontroller.model.TrendDirection
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Lightweight Linear Regression Predictor.
 *
 * Maintains a small, bounded rolling window (20–50 readings) per sensor to keep
 * memory usage strictly constant.
 *
 * Fits Ordinary Least Squares (OLS) linear regression:
 *   y = m * x + c
 *
 * Predicts:
 *   - Next reading (t + 1)
 *   - Next 30 seconds (t + 30)
 *   - Next 1 minute (t + 60)
 *   - Trend direction (Increasing / Decreasing / Stable)
 *   - Future points list for rendering dashed prediction on the sensor graph.
 */
class LinearRegressionPredictor(
    private val windowCapacity: Int = 30,
    private val minRequiredSamples: Int = 5
) {
    // Thread-safe rolling buffers for each sensor (bounded to windowCapacity)
    private val sensorBuffers = ConcurrentHashMap<String, ArrayDeque<Float>>()

    /**
     * Pushes a new cleaned sensor reading to the rolling buffer.
     * Enforces strict maximum window capacity.
     */
    @Synchronized
    fun addSample(sensorKey: String, value: Float) {
        val buffer = sensorBuffers.getOrPut(sensorKey) { ArrayDeque(windowCapacity) }
        buffer.addLast(value)
        while (buffer.size > windowCapacity) {
            buffer.removeFirst()
        }
    }

    /**
     * Gets an immutable snapshot of recent valid readings.
     */
    @Synchronized
    fun getRecentSamples(sensorKey: String): List<Float> {
        return sensorBuffers[sensorKey]?.toList() ?: emptyList()
    }

    /**
     * Computes the linear regression prediction for a given sensor.
     * Runs in O(N) where N <= windowCapacity (at most 30 iterations).
     * Extremely lightweight and suitable for running on background thread without UI jitter.
     */
    fun predict(sensorKey: String): PredictionState {
        val samples = getRecentSamples(sensorKey)
        val n = samples.size

        // Fail safely if insufficient data exists
        if (n < minRequiredSamples) {
            return PredictionState.CollectingData(
                currentSamples = n,
                requiredSamples = minRequiredSamples
            )
        }

        try {
            // OLS Linear Regression
            // x_i = 0, 1, 2, ..., n-1
            // y_i = samples[i]
            var sumX = 0.0
            var sumY = 0.0
            var sumXY = 0.0
            var sumXX = 0.0

            for (i in 0 until n) {
                val x = i.toDouble()
                val y = samples[i].toDouble()
                sumX += x
                sumY += y
                sumXY += x * y
                sumXX += x * x
            }

            val meanX = sumX / n
            val meanY = sumY / n

            val denominator = sumXX - (sumX * sumX / n)
            if (abs(denominator) < 1e-9) {
                // Flat line or zero variance in X
                val flatVal = samples.last()
                return PredictionState.Ready(
                    nextValue = flatVal,
                    future30s = flatVal,
                    future60s = flatVal,
                    trend = TrendDirection.STABLE,
                    slope = 0f,
                    futurePoints = List(10) { flatVal }
                )
            }

            val slope = (sumXY - (sumX * sumY / n)) / denominator
            val intercept = meanY - (slope * meanX)

            // Step index for the latest observed sample is (n - 1)
            val nextStep = n.toDouble()
            val future30sStep = (n - 1 + 30).toDouble()
            val future60sStep = (n - 1 + 60).toDouble()

            val rawNext = (slope * nextStep + intercept).toFloat()
            val raw30s = (slope * future30sStep + intercept).toFloat()
            val raw60s = (slope * future60sStep + intercept).toFloat()

            // Clamp predictions within reasonable bounds per sensor
            val nextValue = clampPrediction(sensorKey, rawNext)
            val future30s = clampPrediction(sensorKey, raw30s)
            val future60s = clampPrediction(sensorKey, raw60s)

            // Trend determination
            val trendThreshold = when (sensorKey) {
                "temperature" -> 0.05f
                "humidity" -> 0.2f
                "distance" -> 0.5f
                "mq" -> 0.5f
                "water" -> 0.5f
                else -> 0.1f
            }

            val trend = when {
                slope > trendThreshold -> TrendDirection.INCREASING
                slope < -trendThreshold -> TrendDirection.DECREASING
                else -> TrendDirection.STABLE
            }

            // Generate future trajectory points (e.g. 10 projection steps into future)
            // Starts at t = n - 1 (current NOW) up to t = n - 1 + 30
            val futurePoints = ArrayList<Float>(10)
            for (step in 1..10) {
                val stepIdx = (n - 1 + (step * 3)).toDouble() // up to +30s
                val predictedPoint = clampPrediction(sensorKey, (slope * stepIdx + intercept).toFloat())
                futurePoints.add(predictedPoint)
            }

            return PredictionState.Ready(
                nextValue = nextValue,
                future30s = future30s,
                future60s = future60s,
                trend = trend,
                slope = slope.toFloat(),
                futurePoints = futurePoints
            )
        } catch (e: Exception) {
            return PredictionState.Error(e.message ?: "Unavailable")
        }
    }

    /**
     * Clamps predicted values to physical reality so calculations never output
     * unphysical figures (e.g. negative water sensor, humidity > 100%).
     */
    private fun clampPrediction(sensorKey: String, value: Float): Float {
        if (value.isNaN() || value.isInfinite()) return 0f
        return when (sensorKey) {
            "temperature" -> value.coerceIn(-10f, 70f)
            "humidity" -> value.coerceIn(0f, 100f)
            "distance" -> value.coerceIn(2f, 450f)
            "mq" -> value.coerceAtLeast(0f)
            "water" -> value.coerceAtLeast(0f)
            else -> value
        }
    }

    /**
     * Clears all rolling buffers.
     */
    @Synchronized
    fun clear() {
        sensorBuffers.clear()
    }
}

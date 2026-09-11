package com.esp32.robotcontroller.ai

import android.content.Context
import com.esp32.robotcontroller.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Historical Data Storage.
 *
 * Appends raw sensor readings to an on-disk CSV file asynchronously.
 * Strictly separate from the live in-memory prediction buffer.
 * Does NOT load the file into RAM, guaranteeing constant minimal memory usage.
 */
class HistoricalDataStorage(private val context: Context) {

    private val fileName = "sensor_history.csv"
    private val csvFile: File by lazy {
        File(context.filesDir, fileName).apply {
            if (!exists()) {
                createNewFile()
                writeHeader()
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    private fun writeHeader() {
        try {
            csvFile.writeText("timestamp,temperature,humidity,distance,mq,water\n")
        } catch (_: Exception) {}
    }

    /**
     * Appends a single raw sensor reading row to CSV on background IO.
     */
    suspend fun appendReading(data: SensorData) = withContext(Dispatchers.IO) {
        try {
            val dateStr = dateFormat.format(Date(data.timestamp))
            val row = String.format(
                Locale.US,
                "%s,%.1f,%.1f,%.1f,%.0f,%.0f\n",
                dateStr,
                data.temperature,
                data.humidity,
                data.distance,
                data.mq,
                data.water
            )
            FileWriter(csvFile, true).use { writer ->
                writer.append(row)
            }
        } catch (_: Exception) {
            // Fail safely: Never crash robot controller on IO error
        }
    }

    fun getStorageFile(): File = csvFile

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            if (csvFile.exists()) {
                csvFile.delete()
            }
            csvFile.createNewFile()
            writeHeader()
        } catch (_: Exception) {}
    }

    suspend fun getFileSize(): Long = withContext(Dispatchers.IO) {
        if (csvFile.exists()) csvFile.length() else 0L
    }
}

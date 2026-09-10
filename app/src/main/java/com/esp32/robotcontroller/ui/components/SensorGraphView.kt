package com.esp32.robotcontroller.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class GraphDataset(
    val name: String,
    val data: List<Float>,
    val color: Color
)

@Composable
fun SensorGraphCard(
    title: String,
    datasets: List<GraphDataset>,
    minVal: Float? = null,
    maxVal: Float? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (ds in datasets) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(ds.color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ds.name,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(6.dp)
        ) {
            SensorGraphCanvas(
                datasets = datasets,
                forcedMin = minVal,
                forcedMax = maxVal,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun SensorGraphCanvas(
    datasets: List<GraphDataset>,
    forcedMin: Float?,
    forcedMax: Float?,
    modifier: Modifier = Modifier
) {
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val textPaint = remember(axisTextColor) {
        Paint().apply {
            color = axisTextColor
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
    }
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val baseLineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftMargin = 70f
        val rightMargin = 16f
        val topMargin = 20f
        val bottomMargin = 32f

        val graphW = w - leftMargin - rightMargin
        val graphH = h - topMargin - bottomMargin

        if (graphW <= 0 || graphH <= 0) return@Canvas

        // Determine min and max
        val allValues = datasets.flatMap { it.data }.filter { it.isFinite() }
        var min = forcedMin ?: (allValues.minOrNull() ?: 0f)
        var max = forcedMax ?: (allValues.maxOrNull() ?: 100f)

        if (min == max) {
            min -= 1f
            max += 1f
        }

        val padding = (max - min) * 0.12f
        if (forcedMin == null) min -= padding
        if (forcedMax == null) max += padding

        val range = (max - min).coerceAtLeast(0.001f)

        // Draw 4 grid lines with values
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topMargin + (graphH * i / gridLines)

            // Horizontal line
            drawLine(
                color = gridLineColor,
                start = Offset(leftMargin, y),
                end = Offset(leftMargin + graphW, y),
                strokeWidth = 1f
            )

            val value = max - ((max - min) * i / gridLines)
            val label = String.format(Locale.US, "%.1f", value)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                leftMargin - 8f,
                y + 7f,
                textPaint
            )
        }

        // X-axis base line
        drawLine(
            color = baseLineColor,
            start = Offset(leftMargin, topMargin + graphH),
            end = Offset(leftMargin + graphW, topMargin + graphH),
            strokeWidth = 1.5f
        )

        // Draw dataset lines (maxPoints = 60, matching HTML MAX_POINTS)
        val maxPoints = 60
        for (ds in datasets) {
            val data = ds.data
            if (data.size < 2) continue

            val path = Path()
            for (idx in data.indices) {
                val value = data[idx]
                val x = leftMargin + (idx.toFloat() / (maxPoints - 1).coerceAtLeast(1)) * graphW
                val normalizedY = ((value - min) / range).coerceIn(0f, 1f)
                val y = topMargin + graphH - (normalizedY * graphH)

                if (idx == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = ds.color,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }
    }
}


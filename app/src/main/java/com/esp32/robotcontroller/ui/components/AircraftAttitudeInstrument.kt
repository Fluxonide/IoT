package com.esp32.robotcontroller.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AircraftAttitudeCard(
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    val smoothRoll by animateFloatAsState(
        targetValue = roll.coerceIn(-90f, 90f),
        animationSpec = spring(stiffness = 600f),
        label = "attRoll"
    )
    val smoothPitch by animateFloatAsState(
        targetValue = pitch.coerceIn(-90f, 90f),
        animationSpec = spring(stiffness = 600f),
        label = "attPitch"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111A23))
            .border(1.dp, Color(0xFF263442), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Aircraft Attitude",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Circular Attitude Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AttitudeCanvas(
                roll = smoothRoll,
                pitch = smoothPitch,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Roll and Pitch telemetry values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AttitudeValueBox(
                label = "ROLL",
                value = String.format(Locale.US, "%.1f°", roll),
                modifier = Modifier.weight(1f)
            )
            AttitudeValueBox(
                label = "PITCH",
                value = String.format(Locale.US, "%.1f°", pitch),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AttitudeCanvas(
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) * 0.44f

        // Clip to circular instrument
        val circlePath = Path().apply {
            addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
        }

        clipPath(circlePath) {
            // Dark base background
            drawRect(color = Color(0xFF0B1117))

            // Horizon (Sky & Ground) rotated by -roll
            rotate(degrees = -roll, pivot = Offset(cx, cy)) {
                val pitchPixels = pitch * 5.5f

                // Sky (#1976A8)
                drawRect(
                    color = Color(0xFF1976A8),
                    topLeft = Offset(-w, -h * 2 + cy + pitchPixels),
                    size = Size(w * 3, h * 2)
                )

                // Ground (#8B5A32)
                drawRect(
                    color = Color(0xFF8B5A32),
                    topLeft = Offset(-w, cy + pitchPixels),
                    size = Size(w * 3, h * 2)
                )

                // White Horizon line
                drawLine(
                    color = Color.White,
                    start = Offset(-w, cy + pitchPixels),
                    end = Offset(w * 2, cy + pitchPixels),
                    strokeWidth = 5f
                )

                // Pitch ladder (-30° to +30° in 10° steps)
                for (deg in -30..30 step 10) {
                    if (deg == 0) continue

                    val rungY = cy + pitchPixels - (deg * 5.5f)
                    val rungWidth = if (deg % 20 == 0) 80f else 45f

                    drawLine(
                        color = Color.White,
                        start = Offset(cx - rungWidth, rungY),
                        end = Offset(cx + rungWidth, rungY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Draw angle number
                    drawContext.canvas.nativeCanvas.drawText(
                        abs(deg).toString(),
                        cx,
                        rungY - 6f,
                        textPaint
                    )
                }
            }
        }

        // --- Fixed Aircraft Center Symbol (Waterline reference) ---
        // Gold / Yellow #FFD84D
        val symbolColor = Color(0xFFFFD84D)
        val wingSpan = radius * 0.48f
        val wingGap = radius * 0.16f

        // Left wing
        drawLine(
            color = symbolColor,
            start = Offset(cx - wingSpan, cy),
            end = Offset(cx - wingGap, cy),
            strokeWidth = 6.5f,
            cap = StrokeCap.Round
        )
        // Right wing
        drawLine(
            color = symbolColor,
            start = Offset(cx + wingGap, cy),
            end = Offset(cx + wingSpan, cy),
            strokeWidth = 6.5f,
            cap = StrokeCap.Round
        )
        // Center vertical tick
        drawLine(
            color = symbolColor,
            start = Offset(cx, cy - 20f),
            end = Offset(cx, cy + 20f),
            strokeWidth = 6.5f,
            cap = StrokeCap.Round
        )
        // Center dot
        drawCircle(
            color = symbolColor,
            radius = 6.5f,
            center = Offset(cx, cy)
        )

        // --- Outer Instrument Ring & Bank Scale ---
        drawCircle(
            color = Color(0xFFD8E2EB),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 5f)
        )

        // Bank scale ticks (-60 to +60 in 10 deg intervals)
        for (angle in -60..60 step 10) {
            val rad = Math.toRadians(angle.toDouble())
            val r1 = radius - 4f
            val r2 = if (angle % 30 == 0) radius - 26f else radius - 15f
            val tickWidth = if (angle % 30 == 0) 4f else 2.5f

            val sinA = sin(rad).toFloat()
            val cosA = -cos(rad).toFloat()

            drawLine(
                color = Color.White,
                start = Offset(cx + sinA * r1, cy + cosA * r1),
                end = Offset(cx + sinA * r2, cy + cosA * r2),
                strokeWidth = tickWidth,
                cap = StrokeCap.Round
            )
        }

        // Roll pointer marker triangle (at -roll position around the rim)
        val markerRad = Math.toRadians(-roll.toDouble())
        val sinM = sin(markerRad).toFloat()
        val cosM = -cos(markerRad).toFloat()
        val markerR = radius + 2f

        val p1 = Offset(cx + sinM * markerR, cy + cosM * markerR)
        val perpSin = cos(markerRad).toFloat()
        val perpCos = sin(markerRad).toFloat()
        val innerR = markerR - 18f
        val p2 = Offset(cx + sinM * innerR - perpSin * 8f, cy + cosM * innerR - perpCos * 8f)
        val p3 = Offset(cx + sinM * innerR + perpSin * 8f, cy + cosM * innerR + perpCos * 8f)

        val markerPath = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawPath(markerPath, color = symbolColor)
    }
}

@Composable
private fun AttitudeValueBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C141C))
            .border(1.dp, Color(0xFF263442), RoundedCornerShape(10.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF8497AA),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

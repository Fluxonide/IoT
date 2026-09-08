package com.esp32.robotcontroller.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GyroHudOverlay(
    pitch: Float,
    roll: Float,
    yaw: Float,
    isDemoMode: Boolean = false,
    onZeroGyro: () -> Unit = {},
    onToggleDemo: () -> Unit = {},
    onToggleHud: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Smooth physics-based interpolation
    val smoothPitch by animateFloatAsState(
        targetValue = pitch.coerceIn(-85f, 85f),
        animationSpec = spring(stiffness = 600f),
        label = "smoothPitch"
    )
    val smoothRoll by animateFloatAsState(
        targetValue = roll.coerceIn(-180f, 180f),
        animationSpec = spring(stiffness = 600f),
        label = "smoothRoll"
    )
    val smoothYaw by animateFloatAsState(
        targetValue = yaw,
        animationSpec = spring(stiffness = 600f),
        label = "smoothYaw"
    )

    val hudColor = Color(0xFF00E5FF)       // Cyan avionics HUD color
    val hudColorDim = Color(0x9900E5FF)
    val hudYellow = Color(0xFFFFD600)      // Center waterline reference

    Box(modifier = modifier.fillMaxSize()) {
        // Main Canvas drawing Artificial Horizon & HUD
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val pitchPxPerDegree = (size.height / 60f).coerceAtLeast(3.5f)

            // --- 1. Rotating Artificial Horizon & Pitch Ladder ---
            rotate(degrees = -smoothRoll, pivot = Offset(cx, cy)) {
                val pitchOffset = smoothPitch * pitchPxPerDegree
                translate(top = pitchOffset) {
                    // Center horizon line (0 degrees)
                    val horizonGap = 35f
                    val horizonLength = size.width * 0.38f

                    // Left horizon bar
                    drawLine(
                        color = hudColor,
                        start = Offset(cx - horizonLength, cy),
                        end = Offset(cx - horizonGap, cy),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                    // Right horizon bar
                    drawLine(
                        color = hudColor,
                        start = Offset(cx + horizonGap, cy),
                        end = Offset(cx + horizonLength, cy),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )

                    // Pitch Ladder Rungs
                    val pitchSteps = listOf(-30, -20, -10, 10, 20, 30)
                    for (deg in pitchSteps) {
                        val rungY = cy - (deg * pitchPxPerDegree)
                        val rungWidth = if (deg % 20 == 0) 55f else 35f
                        val isClimb = deg > 0
                        val tickH = if (isClimb) 7f else -7f

                        if (isClimb) {
                            // Climb (solid line with downward edge ticks)
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx - horizonGap - rungWidth, rungY),
                                end = Offset(cx - horizonGap, rungY),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx - horizonGap - rungWidth, rungY),
                                end = Offset(cx - horizonGap - rungWidth, rungY + tickH),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx + horizonGap, rungY),
                                end = Offset(cx + horizonGap + rungWidth, rungY),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx + horizonGap + rungWidth, rungY),
                                end = Offset(cx + horizonGap + rungWidth, rungY + tickH),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        } else {
                            // Dive (dashed line with upward edge ticks)
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx - horizonGap - rungWidth, rungY),
                                end = Offset(cx - horizonGap, rungY),
                                strokeWidth = 2f,
                                pathEffect = dashEffect,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx - horizonGap - rungWidth, rungY),
                                end = Offset(cx - horizonGap - rungWidth, rungY + tickH),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx + horizonGap, rungY),
                                end = Offset(cx + horizonGap + rungWidth, rungY),
                                strokeWidth = 2f,
                                pathEffect = dashEffect,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = hudColorDim,
                                start = Offset(cx + horizonGap + rungWidth, rungY),
                                end = Offset(cx + horizonGap + rungWidth, rungY + tickH),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // --- 2. Bank / Roll Arc & Pointer (Top Center) ---
            val arcRadius = (size.minDimension * 0.40f).coerceAtLeast(100f)
            val bankAngles = listOf(-60, -45, -30, -20, -10, 0, 10, 20, 30, 45, 60)
            for (angle in bankAngles) {
                val rad = Math.toRadians((angle - 90).toDouble())
                val tickLen = if (angle == 0 || angle % 30 == 0) 10f else 6f
                val x1 = cx + (arcRadius * cos(rad)).toFloat()
                val y1 = cy + (arcRadius * sin(rad)).toFloat()
                val x2 = cx + ((arcRadius + tickLen) * cos(rad)).toFloat()
                val y2 = cy + ((arcRadius + tickLen) * sin(rad)).toFloat()

                drawLine(
                    color = if (angle == 0) hudYellow else hudColorDim,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (angle == 0) 2.5f else 1.5f,
                    cap = StrokeCap.Round
                )
            }

            // Roll indicator pointer (moves along the arc with roll angle)
            val rollRad = Math.toRadians((-smoothRoll - 90).toDouble())
            val ptrX = cx + ((arcRadius - 3f) * cos(rollRad)).toFloat()
            val ptrY = cy + ((arcRadius - 3f) * sin(rollRad)).toFloat()
            val ptrBackX = cx + ((arcRadius - 13f) * cos(rollRad)).toFloat()
            val ptrBackY = cy + ((arcRadius - 13f) * sin(rollRad)).toFloat()

            drawLine(
                color = hudYellow,
                start = Offset(ptrBackX, ptrBackY),
                end = Offset(ptrX, ptrY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // --- 3. Aircraft Boresight / Waterline Symbol (Fixed at Center) ---
            val wingSpan = 38f
            val wingGap = 12f
            val wingDrop = 7f

            // Left wing
            drawLine(
                color = hudYellow,
                start = Offset(cx - wingSpan, cy),
                end = Offset(cx - wingGap, cy),
                strokeWidth = 3f,
                cap = StrokeCap.Square
            )
            drawLine(
                color = hudYellow,
                start = Offset(cx - wingGap, cy),
                end = Offset(cx - wingGap, cy + wingDrop),
                strokeWidth = 3f,
                cap = StrokeCap.Square
            )

            // Right wing
            drawLine(
                color = hudYellow,
                start = Offset(cx + wingGap, cy),
                end = Offset(cx + wingSpan, cy),
                strokeWidth = 3f,
                cap = StrokeCap.Square
            )
            drawLine(
                color = hudYellow,
                start = Offset(cx + wingGap, cy),
                end = Offset(cx + wingGap, cy + wingDrop),
                strokeWidth = 3f,
                cap = StrokeCap.Square
            )

            // Center pip
            drawCircle(
                color = hudYellow,
                radius = 2.5f,
                center = Offset(cx, cy)
            )
        }

        // --- 4. Top Compass Heading Tape ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x77000000))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            val hdg = ((smoothYaw % 360f + 360f) % 360f).toInt()
            val cardinal = when (hdg) {
                in 338..360, in 0..22 -> "N"
                in 23..67 -> "NE"
                in 68..112 -> "E"
                in 113..157 -> "SE"
                in 158..202 -> "S"
                in 203..247 -> "SW"
                in 248..292 -> "W"
                in 293..337 -> "NW"
                else -> "N"
            }
            Text(
                text = "HDG ${String.format("%03d° %s", hdg, cardinal)}",
                color = hudColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // --- 5. Left Pitch & Roll HUD Readouts ---
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x66000000))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = "P: ${if (smoothPitch >= 0) "+" else ""}${String.format("%.1f°", smoothPitch)}",
                color = hudColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "R: ${if (smoothRoll >= 0) "+" else ""}${String.format("%.1f°", smoothRoll)}",
                color = hudColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        // --- 6. Top Right Quick Controls (Zero / Demo / Hide) ---
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zero / Level calibration button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x88000000))
                    .clickable { onZeroGyro() }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LEVEL",
                    color = hudYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Demo simulation button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDemoMode) Color(0xAA00E676) else Color(0x88000000))
                    .clickable { onToggleDemo() }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DEMO",
                    color = if (isDemoMode) Color.Black else hudColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Toggle HUD on/off
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x88000000))
                    .clickable { onToggleHud() }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "HUD",
                    color = hudColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

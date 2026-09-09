package com.esp32.robotcontroller.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.robotcontroller.ui.theme.Amber
import com.esp32.robotcontroller.ui.theme.BorderLineSoft
import com.esp32.robotcontroller.ui.theme.Red
import com.esp32.robotcontroller.viewmodel.CameraState

@Composable
fun MjpegView(
    frame: Bitmap?,
    cameraState: CameraState,
    retryCount: Int = 0,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, BorderLineSoft, RoundedCornerShape(4.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (frame != null && cameraState == CameraState.STREAMING) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = "Live Camera Feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (cameraState) {
                    CameraState.CONNECTING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Amber,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Connecting to camera…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9A9D97)
                            )
                        }
                    }

                    CameraState.STREAMING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Amber,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Receiving stream…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9A9D97)
                            )
                        }
                    }

                    CameraState.ERROR -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(16.dp)
                                .then(
                                    if (onRetry != null) Modifier.clickable { onRetry() }
                                    else Modifier
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Red,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Camera connection failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE7E4D9)
                            )
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9A9D97),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Retry #$retryCount — tap to reconnect",
                                style = MaterialTheme.typography.bodySmall,
                                color = Amber
                            )
                        }
                    }

                    CameraState.DISCONNECTED -> {
                        Text(
                            text = "Camera Offline",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF9A9D97)
                        )
                    }
                }
            }
        }

        // Camera Frame Tag (LIVE · CAM-ESP badge matching HTML)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (cameraState == CameraState.STREAMING) "LIVE · CAM-ESP" else "OFFLINE · CAM-ESP",
                color = if (cameraState == CameraState.STREAMING) Amber else Color(0xFF9A9D97),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}


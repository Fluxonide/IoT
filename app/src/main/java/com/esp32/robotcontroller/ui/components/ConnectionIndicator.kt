package com.esp32.robotcontroller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.esp32.robotcontroller.ui.theme.StatusConnected
import com.esp32.robotcontroller.ui.theme.StatusDisconnected
import com.esp32.robotcontroller.ui.theme.SurfaceCard

@Composable
fun ConnectionIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = if (isConnected) StatusConnected else StatusDisconnected,
        animationSpec = tween(durationMillis = 500),
        label = "statusColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard.copy(alpha = 0.85f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = if (isConnected) "Connected" else "Disconnected",
                style = MaterialTheme.typography.labelLarge,
                color = statusColor
            )
        }
    }
}

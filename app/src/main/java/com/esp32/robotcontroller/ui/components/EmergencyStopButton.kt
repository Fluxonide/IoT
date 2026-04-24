package com.esp32.robotcontroller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.esp32.robotcontroller.ui.theme.EmergencyRed

@Composable
fun EmergencyStopButton(
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPressed = remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .shadow(
                elevation = if (isPressed.value) 2.dp else 8.dp,
                shape = CircleShape,
                ambientColor = EmergencyRed,
                spotColor = EmergencyRed
            )
            .clip(CircleShape)
            .background(
                if (isPressed.value) EmergencyRed.copy(alpha = 0.7f) else EmergencyRed
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed.value = true
                        onEmergencyStop()
                        tryAwaitRelease()
                        isPressed.value = false
                    }
                )
            }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Emergency Stop",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

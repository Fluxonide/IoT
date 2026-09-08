package com.esp32.robotcontroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionDialog(
    currentRobotUrl: String,
    currentCameraUrl: String,
    onDismiss: () -> Unit,
    onSave: (robotUrl: String, cameraUrl: String) -> Unit
) {
    var robotUrlInput by remember { mutableStateOf(currentRobotUrl) }
    var cameraUrlInput by remember { mutableStateOf(currentCameraUrl) }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Connection Settings",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste or enter your custom URLs below:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Robot Custom URL Box
                OutlinedTextField(
                    value = robotUrlInput,
                    onValueChange = { robotUrlInput = it },
                    label = { Text("Connection Custom URL (Robot)") },
                    placeholder = { Text("http://192.168.137.50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clipText ->
                                    robotUrlInput = clipText.trim()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste Robot URL"
                            )
                        }
                    }
                )

                // Camera Custom URL Box
                OutlinedTextField(
                    value = cameraUrlInput,
                    onValueChange = { cameraUrlInput = it },
                    label = { Text("Camera Stream URL (WebSocket)") },
                    placeholder = { Text("ws://192.168.137.60/ws") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clipText ->
                                    cameraUrlInput = clipText.trim()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste Camera URL"
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            robotUrlInput = "http://192.168.137.50"
                            cameraUrlInput = "ws://192.168.137.60/ws"
                        }
                    ) {
                        Text("Reset to Defaults")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(robotUrlInput, cameraUrlInput)
                    onDismiss()
                }
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

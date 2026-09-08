package com.esp32.robotcontroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionDialog(
    currentMotorUrl: String,
    currentSensorUrl: String,
    currentCameraUrl: String,
    onDismiss: () -> Unit,
    onSave: (motorUrl: String, sensorUrl: String, cameraUrl: String) -> Unit
) {
    var motorUrlInput by remember { mutableStateOf(currentMotorUrl) }
    var sensorUrlInput by remember { mutableStateOf(currentSensorUrl) }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure IP / URLs for all 3 ESP32 boards:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Motor URL
                OutlinedTextField(
                    value = motorUrlInput,
                    onValueChange = { motorUrlInput = it },
                    label = { Text("Motor ESP32 URL") },
                    placeholder = { Text("http://10.78.24.50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clip ->
                                    motorUrlInput = clip.trim()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )

                // Sensor URL
                OutlinedTextField(
                    value = sensorUrlInput,
                    onValueChange = { sensorUrlInput = it },
                    label = { Text("Sensor ESP32 URL") },
                    placeholder = { Text("http://10.78.24.51") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clip ->
                                    sensorUrlInput = clip.trim()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )

                // Camera URL
                OutlinedTextField(
                    value = cameraUrlInput,
                    onValueChange = { cameraUrlInput = it },
                    label = { Text("ESP32-CAM Stream / WS URL") },
                    placeholder = { Text("http://10.78.24.60") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clip ->
                                    cameraUrlInput = clip.trim()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            motorUrlInput = "http://10.78.24.50"
                            sensorUrlInput = "http://10.78.24.51"
                            cameraUrlInput = "http://10.78.24.60"
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
                    onSave(motorUrlInput, sensorUrlInput, cameraUrlInput)
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

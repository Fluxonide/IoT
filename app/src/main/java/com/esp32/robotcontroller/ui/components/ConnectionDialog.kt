package com.esp32.robotcontroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/**
 * Extracts the host (with optional port) from a URL string.
 * e.g. "http://172.17.40.60"     → "172.17.40.60"
 *      "http://172.17.40.60:82"  → "172.17.40.60"
 *      "172.17.40.60"            → "172.17.40.60"
 */
private fun extractHost(url: String): String {
    val noScheme = url.trim()
        .removePrefix("http://").removePrefix("https://")
        .removePrefix("ws://").removePrefix("wss://")
    return noScheme.substringBefore("/").substringBefore(":")
}

/** Derive camera stream URL from a base camera URL */
private fun deriveStreamUrl(cameraBase: String): String {
    val host = extractHost(cameraBase)
    return if (host.isNotEmpty()) "http://$host:82/stream" else cameraBase
}

/** Derive WebSocket URL from a base camera URL */
private fun deriveWsUrl(cameraBase: String): String {
    val host = extractHost(cameraBase)
    return if (host.isNotEmpty()) "ws://$host:81" else cameraBase
}

/** Derive status URL from a base URL */
private fun deriveStatusUrl(baseUrl: String): String {
    val trimmed = baseUrl.trim().trimEnd('/')
    val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
        !trimmed.startsWith("https://", ignoreCase = true)
    ) "http://$trimmed" else trimmed
    return "$withScheme/status"
}

@Composable
fun ConnectionDialog(
    currentMotorUrl: String,
    currentSensorUrl: String,
    currentCameraUrl: String,
    currentStreamUrl: String,
    currentWsUrl: String,
    onDismiss: () -> Unit,
    onSave: (motorUrl: String, sensorUrl: String, cameraUrl: String, streamUrl: String, wsUrl: String) -> Unit
) {
    var motorUrlInput by remember { mutableStateOf(currentMotorUrl) }
    var sensorUrlInput by remember { mutableStateOf(currentSensorUrl) }
    var cameraUrlInput by remember { mutableStateOf(currentCameraUrl) }
    var streamUrlInput by remember { mutableStateOf(currentStreamUrl) }
    var wsUrlInput by remember { mutableStateOf(currentWsUrl) }

    // Track whether the user has manually edited stream/ws fields.
    // If not, they auto-derive from the camera base URL.
    var streamManuallyEdited by remember { mutableStateOf(false) }
    var wsManuallyEdited by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure ESP32 endpoints. Stream & WebSocket auto-fill from the Camera URL.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Motor URL
                OutlinedTextField(
                    value = motorUrlInput,
                    onValueChange = { motorUrlInput = it },
                    label = { Text("Motor ESP32 URL") },
                    placeholder = { Text("http://172.17.40.50") },
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
                    placeholder = { Text("http://172.17.40.60") },
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

                // Camera Base URL — drives auto-fill of stream + ws
                OutlinedTextField(
                    value = cameraUrlInput,
                    onValueChange = { newValue ->
                        cameraUrlInput = newValue
                        // Auto-fill stream & ws unless user has manually edited them
                        if (!streamManuallyEdited) {
                            streamUrlInput = deriveStreamUrl(newValue)
                        }
                        if (!wsManuallyEdited) {
                            wsUrlInput = deriveWsUrl(newValue)
                        }
                    },
                    label = { Text("Camera Base URL") },
                    placeholder = { Text("http://172.17.40.60") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clip ->
                                    val pasted = clip.trim()
                                    cameraUrlInput = pasted
                                    if (!streamManuallyEdited) {
                                        streamUrlInput = deriveStreamUrl(pasted)
                                    }
                                    if (!wsManuallyEdited) {
                                        wsUrlInput = deriveWsUrl(pasted)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )

                // Camera Stream URL (auto-filled, manually overridable)
                OutlinedTextField(
                    value = streamUrlInput,
                    onValueChange = {
                        streamUrlInput = it
                        streamManuallyEdited = true
                    },
                    label = { Text("Camera Stream URL") },
                    placeholder = { Text("http://172.17.40.60:82/stream") },
                    supportingText = if (!streamManuallyEdited) {
                        { Text("Auto-filled from Camera URL") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clip ->
                                    streamUrlInput = clip.trim()
                                    streamManuallyEdited = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )

                // WebSocket URL (auto-filled, manually overridable)
                OutlinedTextField(
                    value = wsUrlInput,
                    onValueChange = {
                        wsUrlInput = it
                        wsManuallyEdited = true
                    },
                    label = { Text("WebSocket URL") },
                    placeholder = { Text("ws://172.17.40.60:81") },
                    supportingText = if (!wsManuallyEdited) {
                        { Text("Auto-filled from Camera URL") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clip ->
                                    wsUrlInput = clip.trim()
                                    wsManuallyEdited = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )

                // Derived read-only preview
                Text(
                    text = "Status: ${deriveStatusUrl(cameraUrlInput)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            motorUrlInput = "http://172.17.40.50"
                            sensorUrlInput = "http://172.17.40.60"
                            cameraUrlInput = "http://172.17.40.60"
                            streamUrlInput = "http://172.17.40.60:82/stream"
                            wsUrlInput = "ws://172.17.40.60:81"
                            streamManuallyEdited = false
                            wsManuallyEdited = false
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
                    onSave(motorUrlInput, sensorUrlInput, cameraUrlInput, streamUrlInput, wsUrlInput)
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

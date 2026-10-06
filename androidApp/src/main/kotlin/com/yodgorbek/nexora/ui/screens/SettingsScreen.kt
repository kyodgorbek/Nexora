package com.yodgorbek.nexora.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yodgorbek.nexora.ui.theme.NexoraCardBorder
import com.yodgorbek.nexora.ui.theme.NexoraTeal

@Composable
fun SettingsScreen(
    currentApiKey: String?,
    currentModel: String,
    onSaveApiKey: (String?) -> Unit,
    onSaveModel: (String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey ?: "") }
    var modelInput by remember { mutableStateOf(currentModel) }
    var isSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Configuration & Privacy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage Groq AI keys, telemetry parameters, and security policies",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Groq API Key Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NexoraCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Groq AI Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Groq is strictly optional. Core BLE, local charts, simulator, and rule-based anomaly detection function 100% offline without an API key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            isSaved = false
                        },
                        label = { Text("Groq API Key (gsk_...)") },
                        placeholder = { Text("Leave blank to run in offline local mode") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraTeal,
                            unfocusedBorderColor = NexoraCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {
                            modelInput = it
                            isSaved = false
                        },
                        label = { Text("Groq Model ID") },
                        placeholder = { Text("llama-3.3-70b-versatile") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoraTeal,
                            unfocusedBorderColor = NexoraCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onSaveApiKey(apiKeyInput.trim().ifEmpty { null })
                            onSaveModel(modelInput.trim().ifEmpty { "llama-3.3-70b-versatile" })
                            isSaved = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraTeal, contentColor = Color.Black)
                    ) {
                        Text(if (isSaved) "Saved Successfully" else "Save AI Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // About Nexora & Open Source Licensing
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NexoraCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About Nexora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Version: 1.0.0 (Production Architecture Build)", style = MaterialTheme.typography.bodyMedium)
                    Text("Author: Yodgorbek Komilov", style = MaterialTheme.typography.bodyMedium)
                    Text("License: Apache License 2.0", style = MaterialTheme.typography.bodyMedium)
                    Text("Tagline: Connect. Monitor. Understand.", style = MaterialTheme.typography.bodyMedium, color = NexoraTeal)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Built with Kotlin Multiplatform (KMP), Jetpack Compose Material 3, SwiftUI, Android BLE, CoreBluetooth, and Safe Groq AI.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

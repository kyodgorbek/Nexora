package com.yodgorbek.nexora.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.yodgorbek.nexora.ai.StructuredDeviceCommand
import com.yodgorbek.nexora.ai.TelemetryAnalysisResult
import com.yodgorbek.nexora.domain.model.Anomaly
import com.yodgorbek.nexora.ui.components.AnomalyCard
import com.yodgorbek.nexora.ui.components.CommandConfirmationDialog
import com.yodgorbek.nexora.ui.theme.NexoraCardBorder
import com.yodgorbek.nexora.ui.theme.NexoraGreen
import com.yodgorbek.nexora.ui.theme.NexoraTeal

@Composable
fun InsightsScreen(
    anomalies: List<Anomaly>,
    isAnalyzing: Boolean,
    analysisResult: TelemetryAnalysisResult?,
    isTranslatingCommand: Boolean,
    pendingCommand: StructuredDeviceCommand?,
    onRequestAiAnalysis: () -> Unit,
    onSubmitPrompt: (String) -> Unit,
    onConfirmCommand: (StructuredDeviceCommand) -> Unit,
    onDismissCommand: () -> Unit
) {
    var promptInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Banner
        item {
            Column {
                Text(
                    text = "AI Insights & Safe Commands",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Local anomaly heuristics + optional Groq LLM diagnostic synthesis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Natural Language Command Terminal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NexoraTeal.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NexoraTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Natural Language BLE Command", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "e.g. 'Set sampling interval to 10 seconds' or 'Restart sensor node'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("Enter command in plain English...", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexoraTeal,
                                unfocusedBorderColor = NexoraCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (promptInput.isNotBlank()) {
                                    onSubmitPrompt(promptInput)
                                    promptInput = ""
                                }
                            },
                            enabled = promptInput.isNotBlank() && !isTranslatingCommand,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraTeal, contentColor = Color.Black)
                        ) {
                            if (isTranslatingCommand) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Pending Command Confirmation Modal Card
        if (pendingCommand != null) {
            item {
                CommandConfirmationDialog(
                    command = pendingCommand,
                    onConfirm = { onConfirmCommand(pendingCommand) },
                    onDismiss = onDismissCommand
                )
            }
        }

        // Groq AI Diagnostic Results Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NexoraCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Groq AI Deep Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onRequestAiAnalysis,
                            enabled = !isAnalyzing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraTeal, contentColor = Color.Black)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("Analyze Now", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (analysisResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = analysisResult.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)

                        if (analysisResult.possibleCauses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Probable Causes:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            analysisResult.possibleCauses.forEach { cause ->
                                Text("• $cause", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (analysisResult.recommendations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Recommendations:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            analysisResult.recommendations.forEach { rec ->
                                Text("• $rec", style = MaterialTheme.typography.bodyMedium, color = NexoraGreen)
                            }
                        }
                    }
                }
            }
        }

        // Section: Local Detected Anomalies
        item {
            Text(
                text = "Detected Anomalies (Offline Rule Engine: ${anomalies.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (anomalies.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, NexoraCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NexoraGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("No Active Anomalies", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("All sensor streams and link qualities are within standard baseline boundaries.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            items(anomalies, key = { it.id }) { anomaly ->
                AnomalyCard(
                    anomaly = anomaly,
                    onExplainWithAi = onRequestAiAnalysis
                )
            }
        }
    }
}

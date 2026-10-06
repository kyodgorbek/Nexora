package com.yodgorbek.nexora.ui.screens

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.model.TimeRange
import com.yodgorbek.nexora.ui.components.TelemetrySparkline
import com.yodgorbek.nexora.ui.theme.NexoraAmber
import com.yodgorbek.nexora.ui.theme.NexoraGreen
import com.yodgorbek.nexora.ui.theme.NexoraTeal

@Composable
fun TelemetryScreen(
    deviceId: String,
    history: List<DeviceTelemetry>,
    latest: DeviceTelemetry?,
    selectedRange: TimeRange,
    onSelectTimeRange: (TimeRange) -> Unit
) {
    val tempPoints = history.mapNotNull { it.temperature }
    val humPoints = history.mapNotNull { it.humidity }
    val battPoints = history.map { it.batteryPercentage.toDouble() }
    val rssiPoints = history.mapNotNull { it.signalStrength?.toDouble() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Historical Telemetry Engine",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aggregated data buffers for target: $deviceId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Time Range Filter Bar (5m, 1h, 6h, 24h, 7d)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = range == selectedRange,
                        onClick = { onSelectTimeRange(range) },
                        label = { Text(range.title, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NexoraTeal,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Charts
        item {
            TelemetrySparkline(
                title = "Thermal Dynamics",
                currentValue = "${latest?.temperature?.let { (it * 10).toInt() / 10.0 } ?: "--"}",
                unit = "°C",
                dataPoints = tempPoints,
                lineColor = NexoraTeal
            )
        }

        item {
            TelemetrySparkline(
                title = "Relative Humidity",
                currentValue = "${latest?.humidity?.let { (it * 10).toInt() / 10.0 } ?: "--"}",
                unit = "%",
                dataPoints = humPoints,
                lineColor = Color(0xFF64B5F6)
            )
        }

        item {
            TelemetrySparkline(
                title = "Battery Capacity Discharge",
                currentValue = "${latest?.batteryPercentage ?: "--"}",
                unit = "%",
                dataPoints = battPoints,
                lineColor = NexoraGreen
            )
        }

        item {
            TelemetrySparkline(
                title = "Signal Strength (RSSI)",
                currentValue = "${latest?.signalStrength ?: "--"}",
                unit = "dBm",
                dataPoints = rssiPoints,
                lineColor = NexoraAmber
            )
        }
    }
}

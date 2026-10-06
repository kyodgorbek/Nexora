package com.yodgorbek.nexora.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.ui.components.DeviceCard
import com.yodgorbek.nexora.ui.components.TelemetrySparkline
import com.yodgorbek.nexora.ui.theme.NexoraAmber
import com.yodgorbek.nexora.ui.theme.NexoraCardBorder
import com.yodgorbek.nexora.ui.theme.NexoraGreen
import com.yodgorbek.nexora.ui.theme.NexoraTeal

@Composable
fun HomeScreen(
    connectedDevices: List<BleDevice>,
    latestTelemetry: DeviceTelemetry?,
    deviceHealth: DeviceHealth?,
    onNavigateToDevice: (String) -> Unit,
    onNavigateToScanner: () -> Unit,
    onDisconnectDevice: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nexora Platform",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Connect. Monitor. Understand.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexoraTeal
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NexoraTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Hub",
                        tint = NexoraTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Overview Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricOverviewCard(
                    title = "Active Devices",
                    value = "${connectedDevices.size}",
                    subtitle = "Connected via BLE",
                    color = NexoraTeal,
                    modifier = Modifier.weight(1f)
                )

                MetricOverviewCard(
                    title = "System Health",
                    value = "${deviceHealth?.overallScore ?: 100}%",
                    subtitle = deviceHealth?.statusSummary ?: "Nominal",
                    color = if ((deviceHealth?.overallScore ?: 100) >= 80) NexoraGreen else NexoraAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Real-Time Telemetry Quick Glance
        if (latestTelemetry != null) {
            item {
                Text(
                    text = "Live Telemetry Feed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricOverviewCard(
                        title = "Core Temp",
                        value = "${latestTelemetry.temperature?.let { (it * 10).toInt() / 10.0 } ?: "--"}°C",
                        subtitle = "Thermal sensor",
                        color = NexoraTeal,
                        modifier = Modifier.weight(1f)
                    )
                    MetricOverviewCard(
                        title = "Battery",
                        value = "${latestTelemetry.batteryPercentage}%",
                        subtitle = "Internal cell",
                        color = if (latestTelemetry.batteryPercentage > 20) NexoraGreen else NexoraAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Connected Devices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Connected Peripherals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${connectedDevices.size} Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexoraTeal
                )
            }
        }

        if (connectedDevices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexoraCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Connected Devices",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Switch to the Devices tab to scan and connect to local sensors or simulator nodes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(connectedDevices, key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    connectionState = ConnectionState.Connected,
                    onConnect = { },
                    onDisconnect = { onDisconnectDevice(device.id) },
                    onClick = { onNavigateToDevice(device.id) }
                )
            }
        }
    }
}

@Composable
fun MetricOverviewCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, NexoraCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

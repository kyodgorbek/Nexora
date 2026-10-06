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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceCommand
import com.yodgorbek.nexora.domain.model.DeviceDetails
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.ui.components.StatusPill
import com.yodgorbek.nexora.ui.theme.NexoraAmber
import com.yodgorbek.nexora.ui.theme.NexoraCardBorder
import com.yodgorbek.nexora.ui.theme.NexoraGreen
import com.yodgorbek.nexora.ui.theme.NexoraRed
import com.yodgorbek.nexora.ui.theme.NexoraTeal

@Composable
fun DeviceDetailsScreen(
    device: BleDevice?,
    details: DeviceDetails?,
    connectionState: ConnectionState,
    latestTelemetry: DeviceTelemetry?,
    health: DeviceHealth?,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onExecuteCommand: (DeviceCommand) -> Unit
) {
    if (device == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Device not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val isConnected = connectionState is ConnectionState.Connected

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back & Title Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(connectionState = connectionState)
            }
        }

        // Live Sensor Gauge Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SensorGaugeCard(
                    icon = Icons.Default.DeviceThermostat,
                    title = "Temperature",
                    value = "${latestTelemetry?.temperature?.let { (it * 10).toInt() / 10.0 } ?: "--"}°C",
                    status = if ((latestTelemetry?.temperature ?: 0.0) > 50.0) "High" else "Nominal",
                    color = NexoraTeal,
                    modifier = Modifier.weight(1f)
                )

                SensorGaugeCard(
                    icon = Icons.Default.WaterDrop,
                    title = "Humidity",
                    value = "${latestTelemetry?.humidity?.let { (it * 10).toInt() / 10.0 } ?: "--"}%",
                    status = "Relative RH",
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SensorGaugeCard(
                    icon = Icons.Default.BatteryFull,
                    title = "Battery Cell",
                    value = "${latestTelemetry?.batteryPercentage ?: details?.batteryPercentage ?: "--"}%",
                    status = if ((latestTelemetry?.batteryPercentage ?: 100) < 15) "Low Power" else "Healthy",
                    color = if ((latestTelemetry?.batteryPercentage ?: 100) < 15) NexoraRed else NexoraGreen,
                    modifier = Modifier.weight(1f)
                )

                SensorGaugeCard(
                    icon = Icons.Default.SignalCellularAlt,
                    title = "Link Quality",
                    value = "${latestTelemetry?.signalStrength ?: device.rssi} dBm",
                    status = "BLE 2.4GHz",
                    color = NexoraAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Device Capabilities Matrix
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NexoraCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Capabilities & Features",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val caps = device.capabilities
                    CapabilityItem("Thermal Sensing (Temperature)", caps.supportsTemperature)
                    CapabilityItem("Hygrometric Sensing (Humidity)", caps.supportsHumidity)
                    CapabilityItem("Battery Telemetry", caps.supportsBattery)
                    CapabilityItem("Variable Sampling Interval", caps.supportsSamplingInterval)
                    CapabilityItem("Remote Soft Restart", caps.supportsRestart)
                }
            }
        }

        // Hardware Controls Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NexoraCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hardware Control & Dispatch",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExecuteCommand(DeviceCommand.SetSamplingInterval(1)) },
                            enabled = isConnected,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraTeal, contentColor = Color.Black)
                        ) {
                            Text("Fast (1s)", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { onExecuteCommand(DeviceCommand.SetSamplingInterval(5)) },
                            enabled = isConnected,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NexoraTeal, contentColor = Color.Black)
                        ) {
                            Text("Eco (5s)", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { onExecuteCommand(DeviceCommand.RestartDevice) },
                            enabled = isConnected,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, NexoraAmber),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoraAmber)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restart", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorGaugeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, NexoraCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CapabilityItem(title: String, isSupported: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Icon(
            imageVector = if (isSupported) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isSupported) NexoraGreen else NexoraRed,
            modifier = Modifier.size(18.dp)
        )
    }
}

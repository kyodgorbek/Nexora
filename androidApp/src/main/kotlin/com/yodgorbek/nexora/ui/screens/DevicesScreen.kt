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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.yodgorbek.nexora.ui.components.DeviceCard
import com.yodgorbek.nexora.ui.theme.NexoraAmber
import com.yodgorbek.nexora.ui.theme.NexoraGreen
import com.yodgorbek.nexora.ui.theme.NexoraTeal

@Composable
fun DevicesScreen(
    isScanning: Boolean,
    isSimulatorMode: Boolean,
    discoveredDevices: List<BleDevice>,
    connectionStates: Map<String, ConnectionState>,
    onToggleScan: () -> Unit,
    onToggleSimulatorMode: (Boolean) -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: (String) -> Unit,
    onDeviceClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scanner Top Bar Controls
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BLE Peripheral Radar",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isScanning) "Scanning 2.4GHz LE spectrum..." else "Scanner idle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isScanning) NexoraTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onToggleScan,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) NexoraAmber else NexoraTeal,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isScanning) "Stop" else "Scan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Simulator Toggle Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Zero-Hardware Simulator Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isSimulatorMode) "Emulating 7 realistic IoT nodes (100% offline)" else "Using native hardware Bluetooth adapter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isSimulatorMode,
                        onCheckedChange = onToggleSimulatorMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NexoraTeal
                        )
                    )
                }
            }
        }

        // Section Title: Nearby Devices
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Peripherals (${discoveredDevices.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isScanning) {
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexoraGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (discoveredDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No peripherals discovered yet. Tap 'Scan' above to broadcast discovery requests.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(discoveredDevices, key = { it.id }) { device ->
                val state = connectionStates[device.id] ?: ConnectionState.Disconnected
                DeviceCard(
                    device = device,
                    connectionState = state,
                    onConnect = { onConnect(device.id) },
                    onDisconnect = { onDisconnect(device.id) },
                    onClick = { onDeviceClick(device.id) }
                )
            }
        }
    }
}

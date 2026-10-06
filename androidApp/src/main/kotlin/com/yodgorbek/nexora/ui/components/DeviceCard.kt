package com.yodgorbek.nexora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.yodgorbek.nexora.ui.theme.NexoraAmber
import com.yodgorbek.nexora.ui.theme.NexoraCardBorder
import com.yodgorbek.nexora.ui.theme.NexoraGreen
import com.yodgorbek.nexora.ui.theme.NexoraRed
import com.yodgorbek.nexora.ui.theme.NexoraTeal

@Composable
fun DeviceCard(
    device: BleDevice,
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (isConnected) NexoraTeal else NexoraCardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) NexoraTeal.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Sensors,
                            contentDescription = "Device Icon",
                            tint = if (isConnected) NexoraTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Pill
                StatusPill(connectionState = connectionState)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics row: RSSI, Type, Connectability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RSSI: ${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            device.rssi >= -65 -> NexoraGreen
                            device.rssi >= -85 -> NexoraAmber
                            else -> NexoraRed
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Type: ${device.deviceType.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NexoraRed
                        ),
                        border = BorderStroke(1.dp, NexoraRed.copy(alpha = 0.5f))
                    ) {
                        Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        enabled = !isConnecting,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexoraTeal,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (isConnecting) "Connecting..." else "Connect",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(connectionState: ConnectionState) {
    val (text, bgColor, textColor) = when (connectionState) {
        is ConnectionState.Connected -> Triple("Connected", NexoraGreen.copy(alpha = 0.15f), NexoraGreen)
        is ConnectionState.Connecting -> Triple("Connecting", NexoraAmber.copy(alpha = 0.15f), NexoraAmber)
        is ConnectionState.Reconnecting -> Triple("Reconnecting", NexoraAmber.copy(alpha = 0.15f), NexoraAmber)
        is ConnectionState.Disconnecting -> Triple("Disconnecting", NexoraCardBorder, MaterialTheme.colorScheme.onSurfaceVariant)
        is ConnectionState.Disconnected -> Triple("Disconnected", NexoraCardBorder, MaterialTheme.colorScheme.onSurfaceVariant)
        is ConnectionState.Scanning -> Triple("Scanning", NexoraTeal.copy(alpha = 0.15f), NexoraTeal)
        is ConnectionState.Error -> Triple("Error", NexoraRed.copy(alpha = 0.15f), NexoraRed)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

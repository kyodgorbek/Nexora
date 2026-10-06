package com.yodgorbek.nexora.domain.repository

import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceDetails
import kotlinx.coroutines.flow.Flow

interface BleDeviceRepository {
    fun getDiscoveredDevices(): Flow<List<BleDevice>>
    fun getKnownDevices(): Flow<List<BleDevice>>
    fun getConnectedDevices(): Flow<List<BleDevice>>
    fun observeConnectionState(deviceId: String): Flow<ConnectionState>
    suspend fun getDeviceDetails(deviceId: String): DeviceDetails?
    suspend fun saveDevice(device: BleDevice)
    suspend fun removeDevice(deviceId: String)
    suspend fun clearDiscoveredDevices()
}


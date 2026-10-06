package com.yodgorbek.nexora.ble.manager

import com.yodgorbek.nexora.ble.model.BleService
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow

interface BleManager {

    /**
     * Starts continuous BLE scanning and emits discovered devices.
     */
    fun scan(): Flow<BleDevice>

    /**
     * Connects to a target BLE device by identifier (MAC Address or CoreBluetooth UUID).
     */
    suspend fun connect(deviceId: String)

    /**
     * Disconnects from the target device.
     */
    suspend fun disconnect(deviceId: String)

    /**
     * Observes real-time connection state transitions for the specified device.
     */
    fun observeConnectionState(deviceId: String): Flow<ConnectionState>

    /**
     * Discovers all supported GATT services and characteristics on the connected peripheral.
     */
    suspend fun discoverServices(deviceId: String): List<BleService>

    /**
     * Reads raw bytes from the target GATT characteristic.
     */
    suspend fun readCharacteristic(
        deviceId: String,
        serviceUuid: String,
        characteristicUuid: String
    ): ByteArray

    /**
     * Writes raw bytes to the target GATT characteristic.
     */
    suspend fun writeCharacteristic(
        deviceId: String,
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray
    )

    /**
     * Observes real-time notifications/indications emitted by a GATT characteristic.
     */
    fun observeCharacteristic(
        deviceId: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Flow<ByteArray>
}

package com.yodgorbek.nexora.ble.manager

import com.yodgorbek.nexora.ble.model.BleService
import com.yodgorbek.nexora.ble.simulator.BleDeviceSimulator
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class DynamicBleManager(
    private val simulator: BleDeviceSimulator = BleDeviceSimulator(),
    private var hardwareManager: BleManager? = null
) : BleManager {

    private val isSimulatorActive = MutableStateFlow(true)

    fun setSimulatorActive(active: Boolean) {
        isSimulatorActive.value = active
    }

    fun setHardwareBleManager(manager: BleManager) {
        this.hardwareManager = manager
    }

    private val activeManager: BleManager
        get() = if (isSimulatorActive.value || hardwareManager == null) simulator else hardwareManager!!

    override fun scan(): Flow<BleDevice> {
        return isSimulatorActive.flatMapLatest { isSim ->
            if (isSim || hardwareManager == null) {
                simulator.scan()
            } else {
                hardwareManager!!.scan()
            }
        }
    }

    override suspend fun connect(deviceId: String) {
        activeManager.connect(deviceId)
    }

    override suspend fun disconnect(deviceId: String) {
        activeManager.disconnect(deviceId)
    }

    override fun observeConnectionState(deviceId: String): Flow<ConnectionState> {
        return isSimulatorActive.flatMapLatest { isSim ->
            if (isSim || hardwareManager == null) {
                simulator.observeConnectionState(deviceId)
            } else {
                hardwareManager!!.observeConnectionState(deviceId)
            }
        }
    }

    override suspend fun discoverServices(deviceId: String): List<BleService> {
        return activeManager.discoverServices(deviceId)
    }

    override suspend fun readCharacteristic(
        deviceId: String,
        serviceUuid: String,
        characteristicUuid: String
    ): ByteArray {
        return activeManager.readCharacteristic(deviceId, serviceUuid, characteristicUuid)
    }

    override suspend fun writeCharacteristic(
        deviceId: String,
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray
    ) {
        activeManager.writeCharacteristic(deviceId, serviceUuid, characteristicUuid, data)
    }

    override fun observeCharacteristic(
        deviceId: String,
        serviceUuid: String,
        characteristicUuid: String
    ): Flow<ByteArray> {
        return isSimulatorActive.flatMapLatest { isSim ->
            if (isSim || hardwareManager == null) {
                simulator.observeCharacteristic(deviceId, serviceUuid, characteristicUuid)
            } else {
                hardwareManager!!.observeCharacteristic(deviceId, serviceUuid, characteristicUuid)
            }
        }
    }
}

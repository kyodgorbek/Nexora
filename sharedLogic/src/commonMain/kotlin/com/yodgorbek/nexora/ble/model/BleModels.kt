package com.yodgorbek.nexora.ble.model

import kotlinx.serialization.Serializable

@Serializable
enum class CharacteristicProperty {
    READ,
    WRITE,
    WRITE_NO_RESPONSE,
    NOTIFY,
    INDICATE
}

@Serializable
data class BleCharacteristic(
    val uuid: String,
    val serviceUuid: String,
    val properties: List<CharacteristicProperty>,
    val description: String = ""
)

@Serializable
data class BleService(
    val uuid: String,
    val name: String,
    val isPrimary: Boolean = true,
    val characteristics: List<BleCharacteristic> = emptyList()
)

@Serializable
sealed interface BleError {
    data class DeviceNotFound(val deviceId: String) : BleError
    data class ConnectionFailed(val reason: String, val code: Int = -1) : BleError
    data class ServiceDiscoveryFailed(val deviceId: String) : BleError
    data class CharacteristicNotFound(val characteristicUuid: String) : BleError
    data class ReadFailed(val characteristicUuid: String, val reason: String) : BleError
    data class WriteFailed(val characteristicUuid: String, val reason: String) : BleError
    data class OperationTimeout(val operation: String) : BleError
    data class BluetoothDisabled(val message: String = "Bluetooth adapter is disabled.") : BleError
    data class PermissionDenied(val missingPermissions: List<String>) : BleError
    data class Unknown(val message: String) : BleError
}

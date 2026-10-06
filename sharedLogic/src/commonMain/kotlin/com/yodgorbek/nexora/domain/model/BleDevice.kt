package com.yodgorbek.nexora.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType {
    SENSOR_NODE,
    WEATHER_STATION,
    HEALTH_BAND,
    SMART_BEACON,
    INDUSTRIAL_PROBE,
    SIMULATED_PROBE
}

@Serializable
data class BleDevice(
    val id: String,
    val name: String,
    val address: String,
    val rssi: Int,
    val isConnectable: Boolean = true,
    val deviceType: DeviceType = DeviceType.SENSOR_NODE,
    val lastSeen: Instant,
    val capabilities: DeviceCapabilities = DeviceCapabilities.DEFAULT
)

@Serializable
data class DeviceDetails(
    val device: BleDevice,
    val firmwareVersion: String = "1.0.0",
    val hardwareVersion: String = "Rev-B",
    val serialNumber: String = "NX-000000",
    val manufacturer: String = "Nexora Open Systems",
    val batteryPercentage: Int = 100,
    val isSimulated: Boolean = false
)

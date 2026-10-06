package com.yodgorbek.nexora.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceCapabilities(
    val supportsTemperature: Boolean = true,
    val supportsHumidity: Boolean = true,
    val supportsBattery: Boolean = true,
    val supportsSignalStrength: Boolean = true,
    val supportsSamplingInterval: Boolean = true,
    val supportsTelemetryToggle: Boolean = true,
    val supportsRestart: Boolean = true,
    val supportsFirmwareUpdate: Boolean = false,
    val minSamplingIntervalSeconds: Int = 1,
    val maxSamplingIntervalSeconds: Int = 3600
) {
    companion object {
        val DEFAULT = DeviceCapabilities()
        val FULL = DeviceCapabilities(
            supportsTemperature = true,
            supportsHumidity = true,
            supportsBattery = true,
            supportsSignalStrength = true,
            supportsSamplingInterval = true,
            supportsTelemetryToggle = true,
            supportsRestart = true,
            supportsFirmwareUpdate = true
        )
        val READ_ONLY = DeviceCapabilities(
            supportsTemperature = true,
            supportsHumidity = true,
            supportsBattery = true,
            supportsSignalStrength = true,
            supportsSamplingInterval = false,
            supportsTelemetryToggle = false,
            supportsRestart = false,
            supportsFirmwareUpdate = false
        )
    }
}

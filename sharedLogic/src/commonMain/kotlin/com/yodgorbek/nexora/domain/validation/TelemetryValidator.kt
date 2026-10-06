package com.yodgorbek.nexora.domain.validation

import com.yodgorbek.nexora.domain.model.DeviceTelemetry

class TelemetryValidator {

    fun validate(telemetry: DeviceTelemetry): DeviceTelemetry {
        var isValid = true

        // Battery: 0 to 100%
        val batteryValid = telemetry.batteryPercentage in 0..100
        if (!batteryValid) isValid = false

        // Temperature: -50.0°C to +150.0°C
        val tempValid = telemetry.temperature?.let { it in -50.0..150.0 } ?: true
        if (!tempValid) isValid = false

        // Humidity: 0.0% to 100.0%
        val humidityValid = telemetry.humidity?.let { it in 0.0..100.0 } ?: true
        if (!humidityValid) isValid = false

        // Signal Strength: -130 dBm to 0 dBm
        val signalValid = telemetry.signalStrength?.let { it in -130..0 } ?: true
        if (!signalValid) isValid = false

        return telemetry.copy(isValid = isValid)
    }

    fun isAcceptable(telemetry: DeviceTelemetry): Boolean {
        return validate(telemetry).isValid
    }
}

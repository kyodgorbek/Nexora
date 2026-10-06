package com.yodgorbek.nexora.domain.validation

import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceCommand

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}

class CommandValidator {

    fun validate(
        command: DeviceCommand,
        capabilities: DeviceCapabilities,
        connectionState: ConnectionState
    ): ValidationResult {
        // 1. Connection State Check
        if (connectionState !is ConnectionState.Connected) {
            return ValidationResult.Invalid("Device is not connected (current state: $connectionState). Cannot execute command.")
        }

        // 2. Capability & Parameter Bounds Check
        return when (command) {
            is DeviceCommand.SetSamplingInterval -> {
                if (!capabilities.supportsSamplingInterval) {
                    ValidationResult.Invalid("Device does not support setting sampling interval.")
                } else if (command.seconds < capabilities.minSamplingIntervalSeconds) {
                    ValidationResult.Invalid("Sampling interval ${command.seconds}s is below minimum allowed (${capabilities.minSamplingIntervalSeconds}s).")
                } else if (command.seconds > capabilities.maxSamplingIntervalSeconds) {
                    ValidationResult.Invalid("Sampling interval ${command.seconds}s exceeds maximum allowed (${capabilities.maxSamplingIntervalSeconds}s).")
                } else {
                    ValidationResult.Valid
                }
            }

            is DeviceCommand.SetTelemetryEnabled -> {
                if (!capabilities.supportsTelemetryToggle) {
                    ValidationResult.Invalid("Device does not support toggling telemetry stream.")
                } else {
                    ValidationResult.Valid
                }
            }

            is DeviceCommand.RestartDevice -> {
                if (!capabilities.supportsRestart) {
                    ValidationResult.Invalid("Device does not support remote restart.")
                } else {
                    ValidationResult.Valid
                }
            }

            is DeviceCommand.CalibrateSensor -> {
                if (command.sensorType.isBlank()) {
                    ValidationResult.Invalid("Sensor type cannot be blank.")
                } else if (command.offset.isNaN() || command.offset.isInfinite()) {
                    ValidationResult.Invalid("Invalid calibration offset value.")
                } else {
                    ValidationResult.Valid
                }
            }

            is DeviceCommand.CustomCommand -> {
                if (command.commandId < 0 || command.commandId > 0xFFFF) {
                    ValidationResult.Invalid("Command ID out of valid 16-bit range.")
                } else if (command.payloadHex.length % 2 != 0) {
                    ValidationResult.Invalid("Payload HEX string must have an even number of characters.")
                } else {
                    ValidationResult.Valid
                }
            }
        }
    }
}

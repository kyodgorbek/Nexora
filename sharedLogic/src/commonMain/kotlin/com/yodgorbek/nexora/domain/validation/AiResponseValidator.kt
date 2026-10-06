package com.yodgorbek.nexora.domain.validation

import com.yodgorbek.nexora.ai.RawAiCommandResponse
import com.yodgorbek.nexora.ai.RawCommandType
import com.yodgorbek.nexora.ai.StructuredDeviceCommand
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceCommand

class AiResponseValidator(
    private val commandValidator: CommandValidator = CommandValidator()
) {

    /**
     * Strictly validates AI raw output against domain constraints, bounds, device capabilities,
     * and current connection state before producing a safe [StructuredDeviceCommand].
     *
     * LLMs are NEVER allowed to bypass domain validation.
     */
    fun validateAndMap(
        rawResponse: RawAiCommandResponse,
        targetDeviceId: String,
        capabilities: DeviceCapabilities,
        connectionState: ConnectionState
    ): StructuredDeviceCommand {
        val mappedCommand: DeviceCommand? = when (rawResponse.command) {
            RawCommandType.SET_SAMPLING_INTERVAL -> {
                val seconds = rawResponse.seconds
                if (seconds == null || seconds <= 0) null
                else DeviceCommand.SetSamplingInterval(seconds)
            }
            RawCommandType.SET_TELEMETRY_ENABLED -> {
                val enabled = rawResponse.enabled
                if (enabled == null) null
                else DeviceCommand.SetTelemetryEnabled(enabled)
            }
            RawCommandType.RESTART_DEVICE -> {
                DeviceCommand.RestartDevice
            }
            RawCommandType.CALIBRATE_SENSOR -> {
                val sensor = rawResponse.sensorType
                val offset = rawResponse.offset
                if (sensor.isNullOrBlank() || offset == null) null
                else DeviceCommand.CalibrateSensor(sensor, offset)
            }
            RawCommandType.UNKNOWN -> null
        }

        if (mappedCommand == null) {
            return StructuredDeviceCommand(
                targetDeviceId = targetDeviceId,
                command = null,
                rawExplanation = rawResponse.explanation.ifBlank { "Unrecognized or unsupported command intent from AI model." },
                confidence = rawResponse.confidence,
                isValid = false,
                validationError = "Unable to map AI output to a known, supported device command.",
                requiresUserConfirmation = true
            )
        }

        // Domain & capability validation
        return when (val result = commandValidator.validate(mappedCommand, capabilities, connectionState)) {
            is ValidationResult.Valid -> {
                StructuredDeviceCommand(
                    targetDeviceId = targetDeviceId,
                    command = mappedCommand,
                    rawExplanation = rawResponse.explanation,
                    confidence = rawResponse.confidence,
                    isValid = true,
                    validationError = null,
                    requiresUserConfirmation = true // Crucial: Always require user confirmation
                )
            }
            is ValidationResult.Invalid -> {
                StructuredDeviceCommand(
                    targetDeviceId = targetDeviceId,
                    command = mappedCommand,
                    rawExplanation = rawResponse.explanation,
                    confidence = rawResponse.confidence,
                    isValid = false,
                    validationError = result.reason,
                    requiresUserConfirmation = true
                )
            }
        }
    }
}

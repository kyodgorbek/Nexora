package com.yodgorbek.nexora.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed interface DeviceCommand {

    @Serializable
    data class SetSamplingInterval(
        val seconds: Int
    ) : DeviceCommand

    @Serializable
    data class SetTelemetryEnabled(
        val enabled: Boolean
    ) : DeviceCommand

    @Serializable
    data object RestartDevice : DeviceCommand

    @Serializable
    data class CalibrateSensor(
        val sensorType: String,
        val offset: Double
    ) : DeviceCommand

    @Serializable
    data class CustomCommand(
        val commandId: Int,
        val payloadHex: String
    ) : DeviceCommand
}

@Serializable
enum class CommandExecutionStatus {
    PENDING,
    VALIDATED,
    SENT,
    ACKNOWLEDGED,
    REJECTED,
    FAILED,
    TIMEOUT
}

@Serializable
data class CommandResult(
    val command: DeviceCommand,
    val status: CommandExecutionStatus,
    val message: String? = null,
    val timestamp: Instant = Clock.System.now(),
    val rawResponseHex: String? = null
)

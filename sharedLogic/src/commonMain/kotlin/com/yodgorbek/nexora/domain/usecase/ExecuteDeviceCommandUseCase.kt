package com.yodgorbek.nexora.domain.usecase

import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.ble.protocol.CommandPacketEncoder
import com.yodgorbek.nexora.ble.protocol.GattSpec
import com.yodgorbek.nexora.domain.model.CommandExecutionStatus
import com.yodgorbek.nexora.domain.model.CommandResult
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceCommand
import com.yodgorbek.nexora.domain.validation.CommandValidator
import com.yodgorbek.nexora.domain.validation.ValidationResult

class ExecuteDeviceCommandUseCase(
    private val bleManager: BleManager,
    private val commandValidator: CommandValidator = CommandValidator()
) {
    suspend operator fun invoke(
        deviceId: String,
        command: DeviceCommand,
        capabilities: DeviceCapabilities,
        currentState: ConnectionState
    ): CommandResult {
        val validation = commandValidator.validate(command, capabilities, currentState)
        if (validation is ValidationResult.Invalid) {
            return CommandResult(
                command = command,
                status = CommandExecutionStatus.REJECTED,
                message = validation.reason
            )
        }

        return try {
            val encodedBytes = CommandPacketEncoder.encode(command)
            bleManager.writeCharacteristic(
                deviceId = deviceId,
                serviceUuid = GattSpec.SERVICE_COMMAND,
                characteristicUuid = GattSpec.CHAR_COMMAND_WRITE,
                data = encodedBytes
            )
            CommandResult(
                command = command,
                status = CommandExecutionStatus.ACKNOWLEDGED,
                message = "Command successfully dispatched to peripheral."
            )
        } catch (e: Exception) {
            CommandResult(
                command = command,
                status = CommandExecutionStatus.FAILED,
                message = e.message ?: "Failed to transmit command over BLE."
            )
        }
    }
}

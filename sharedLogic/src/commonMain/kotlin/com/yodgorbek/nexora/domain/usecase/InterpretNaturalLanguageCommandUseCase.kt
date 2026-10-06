package com.yodgorbek.nexora.domain.usecase

import com.yodgorbek.nexora.ai.AiService
import com.yodgorbek.nexora.ai.NaturalLanguageCommandRequest
import com.yodgorbek.nexora.ai.StructuredDeviceCommand
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.validation.AiResponseValidator

class InterpretNaturalLanguageCommandUseCase(
    private val aiService: AiService,
    private val aiResponseValidator: AiResponseValidator = AiResponseValidator()
) {
    suspend operator fun invoke(
        request: NaturalLanguageCommandRequest,
        capabilities: DeviceCapabilities,
        currentState: ConnectionState
    ): StructuredDeviceCommand {
        val rawResponse = aiService.interpretDeviceCommand(request, capabilities)
        return aiResponseValidator.validateAndMap(
            rawResponse = rawResponse,
            targetDeviceId = request.deviceId,
            capabilities = capabilities,
            connectionState = currentState
        )
    }
}

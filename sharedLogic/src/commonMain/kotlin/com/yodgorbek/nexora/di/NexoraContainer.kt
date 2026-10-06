package com.yodgorbek.nexora.di

import com.yodgorbek.nexora.ai.AiService
import com.yodgorbek.nexora.ai.GroqAiService
import com.yodgorbek.nexora.analytics.DeviceHealthCalculator
import com.yodgorbek.nexora.analytics.LocalAnomalyDetector
import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.ble.manager.DynamicBleManager
import com.yodgorbek.nexora.ble.simulator.BleDeviceSimulator
import com.yodgorbek.nexora.data.repository.InMemoryDeviceRepository
import com.yodgorbek.nexora.data.repository.InMemorySettingsRepository
import com.yodgorbek.nexora.data.repository.InMemoryTelemetryRepository
import com.yodgorbek.nexora.domain.repository.BleDeviceRepository
import com.yodgorbek.nexora.domain.repository.SettingsRepository
import com.yodgorbek.nexora.domain.repository.TelemetryRepository
import com.yodgorbek.nexora.domain.usecase.ConnectDeviceUseCase
import com.yodgorbek.nexora.domain.usecase.DisconnectDeviceUseCase
import com.yodgorbek.nexora.domain.usecase.ExecuteDeviceCommandUseCase
import com.yodgorbek.nexora.domain.usecase.InterpretNaturalLanguageCommandUseCase
import com.yodgorbek.nexora.domain.usecase.ObserveDeviceStateUseCase
import com.yodgorbek.nexora.domain.usecase.ObserveTelemetryUseCase
import com.yodgorbek.nexora.domain.usecase.ScanDevicesUseCase
import com.yodgorbek.nexora.domain.validation.AiResponseValidator
import com.yodgorbek.nexora.domain.validation.CommandValidator
import com.yodgorbek.nexora.domain.validation.TelemetryValidator

class NexoraContainer(
    val dynamicBleManager: DynamicBleManager = DynamicBleManager(),
    val deviceRepository: BleDeviceRepository = InMemoryDeviceRepository(),
    val telemetryRepository: TelemetryRepository = InMemoryTelemetryRepository(),
    val settingsRepository: SettingsRepository = InMemorySettingsRepository(),
    val anomalyDetector: LocalAnomalyDetector = LocalAnomalyDetector(),
    val healthCalculator: DeviceHealthCalculator = DeviceHealthCalculator(),
    val commandValidator: CommandValidator = CommandValidator(),
    val telemetryValidator: TelemetryValidator = TelemetryValidator(),
    val aiResponseValidator: AiResponseValidator = AiResponseValidator()
) {
    val bleManager: BleManager get() = dynamicBleManager

    // Dynamic AI Service fetching key from Settings
    private var cachedApiKey: String? = null
    private var cachedModel: String = "llama-3.3-70b-versatile"

    val aiService: AiService by lazy {
        GroqAiService(
            apiKeyProvider = { cachedApiKey },
            modelProvider = { cachedModel }
        )
    }

    // Use cases
    val scanDevicesUseCase by lazy { ScanDevicesUseCase(dynamicBleManager, deviceRepository) }
    val connectDeviceUseCase by lazy { ConnectDeviceUseCase(dynamicBleManager) }
    val disconnectDeviceUseCase by lazy { DisconnectDeviceUseCase(dynamicBleManager) }
    val observeDeviceStateUseCase by lazy { ObserveDeviceStateUseCase(dynamicBleManager) }
    val observeTelemetryUseCase by lazy {
        ObserveTelemetryUseCase(
            bleManager = dynamicBleManager,
            telemetryRepository = telemetryRepository,
            telemetryValidator = telemetryValidator,
            anomalyDetector = anomalyDetector,
            healthCalculator = healthCalculator
        )
    }
    val executeDeviceCommandUseCase by lazy {
        ExecuteDeviceCommandUseCase(dynamicBleManager, commandValidator)
    }
    val interpretNaturalLanguageCommandUseCase by lazy {
        InterpretNaturalLanguageCommandUseCase(aiService, aiResponseValidator)
    }

    fun setSimulatorActive(active: Boolean) {
        dynamicBleManager.setSimulatorActive(active)
    }

    fun setHardwareBleManager(manager: BleManager) {
        dynamicBleManager.setHardwareBleManager(manager)
    }

    fun updateAiConfig(apiKey: String?, model: String = "llama-3.3-70b-versatile") {
        cachedApiKey = apiKey
        cachedModel = model
    }

    companion object {
        val instance: NexoraContainer by lazy { NexoraContainer() }
    }
}

package com.yodgorbek.nexora.mvi

import com.yodgorbek.nexora.ai.StructuredDeviceCommand
import com.yodgorbek.nexora.ai.TelemetryAnalysisResult
import com.yodgorbek.nexora.ble.simulator.SimulatorScenario
import com.yodgorbek.nexora.domain.model.Anomaly
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.CommandResult
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceCommand
import com.yodgorbek.nexora.domain.model.DeviceDetails
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.model.SyncState
import com.yodgorbek.nexora.domain.model.TimeRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class MviStore<Intent, State, Effect>(
    initialState: State,
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect: Flow<Effect> = _effect.asSharedFlow()

    abstract fun processIntent(intent: Intent)

    protected fun setState(reducer: State.() -> State) {
        _state.value = _state.value.reducer()
    }

    protected fun sendEffect(effect: Effect) {
        scope.launch {
            _effect.emit(effect)
        }
    }
}

// 1. Device List Contract
sealed interface DeviceListIntent {
    data object StartScan : DeviceListIntent
    data object StopScan : DeviceListIntent
    data class Connect(val deviceId: String) : DeviceListIntent
    data class Disconnect(val deviceId: String) : DeviceListIntent
    data class ToggleSimulatorMode(val enabled: Boolean) : DeviceListIntent
}

data class DeviceListState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<BleDevice> = emptyList(),
    val connectedDevices: List<BleDevice> = emptyList(),
    val isSimulatorMode: Boolean = true,
    val error: String? = null
)

sealed interface DeviceListEffect {
    data class NavigateToDetails(val deviceId: String) : DeviceListEffect
    data class ShowToast(val message: String) : DeviceListEffect
}

// 2. Device Details Contract
sealed interface DeviceDetailsIntent {
    data class Load(val deviceId: String) : DeviceDetailsIntent
    data class ExecuteCommand(val command: DeviceCommand) : DeviceDetailsIntent
    data object RefreshState : DeviceDetailsIntent
    data object Disconnect : DeviceDetailsIntent
}

data class DeviceDetailsState(
    val deviceId: String = "",
    val details: DeviceDetails? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val latestTelemetry: DeviceTelemetry? = null,
    val health: DeviceHealth? = null,
    val activeAnomalies: List<Anomaly> = emptyList(),
    val lastCommandResult: CommandResult? = null,
    val isLoading: Boolean = false
)

sealed interface DeviceDetailsEffect {
    data class ShowCommandResult(val result: CommandResult) : DeviceDetailsEffect
    data class ShowError(val message: String) : DeviceDetailsEffect
}

// 3. Telemetry Contract
sealed interface TelemetryIntent {
    data class SelectDevice(val deviceId: String) : TelemetryIntent
    data class ChangeTimeRange(val timeRange: TimeRange) : TelemetryIntent
    data class SetSamplingRate(val seconds: Int) : TelemetryIntent
    data class ToggleStream(val enabled: Boolean) : TelemetryIntent
}

data class TelemetryState(
    val selectedDeviceId: String = "",
    val timeRange: TimeRange = TimeRange.FIVE_MINUTES,
    val currentTelemetry: DeviceTelemetry? = null,
    val history: List<DeviceTelemetry> = emptyList(),
    val samplingIntervalSeconds: Int = 2,
    val isStreamActive: Boolean = true,
    val syncState: SyncState = SyncState.IDLE
)

// 4. Insights & AI Diagnostic Contract
sealed interface InsightsIntent {
    data class SelectDevice(val deviceId: String) : InsightsIntent
    data object RequestAiTelemetryAnalysis : InsightsIntent
    data class SubmitNaturalLanguageCommand(val prompt: String) : InsightsIntent
    data class ConfirmAndExecuteCommand(val command: StructuredDeviceCommand) : InsightsIntent
    data object DismissPendingCommand : InsightsIntent
}

data class InsightsState(
    val selectedDeviceId: String = "",
    val deviceName: String = "",
    val localAnomalies: List<Anomaly> = emptyList(),
    val deviceHealth: DeviceHealth? = null,
    val isAnalyzing: Boolean = false,
    val aiAnalysisResult: TelemetryAnalysisResult? = null,
    val isTranslatingCommand: Boolean = false,
    val pendingCommandConfirmation: StructuredDeviceCommand? = null,
    val commandExecutionResult: CommandResult? = null,
    val isAiConfigured: Boolean = false,
    val errorMessage: String? = null
)

sealed interface InsightsEffect {
    data class ShowConfirmationDialog(val command: StructuredDeviceCommand) : InsightsEffect
    data class CommandExecuted(val result: CommandResult) : InsightsEffect
    data class ShowError(val message: String) : InsightsEffect
}

// 5. Developer Diagnostics Contract
sealed interface DeveloperDiagnosticsIntent {
    data class SwitchScenario(val scenario: SimulatorScenario) : DeveloperDiagnosticsIntent
    data class InjectPacketLoss(val dropRate: Float) : DeveloperDiagnosticsIntent
    data object ClearLogs : DeveloperDiagnosticsIntent
    data class ToggleSyncServer(val enabled: Boolean) : DeveloperDiagnosticsIntent
}

data class DeveloperDiagnosticsState(
    val currentScenario: SimulatorScenario = SimulatorScenario.NORMAL,
    val bleStateSummary: String = "Simulator Active",
    val rawPacketLog: List<String> = emptyList(),
    val simulatedDropRate: Float = 0.0f,
    val localServerRunning: Boolean = false,
    val syncStatus: SyncState = SyncState.OFFLINE
)

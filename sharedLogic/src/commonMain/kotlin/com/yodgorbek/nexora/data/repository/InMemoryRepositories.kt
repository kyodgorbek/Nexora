package com.yodgorbek.nexora.data.repository

import com.yodgorbek.nexora.domain.model.Anomaly
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceDetails
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.model.SyncState
import com.yodgorbek.nexora.domain.model.TimeRange
import com.yodgorbek.nexora.domain.repository.BleDeviceRepository
import com.yodgorbek.nexora.domain.repository.SettingsRepository
import com.yodgorbek.nexora.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class InMemoryDeviceRepository : BleDeviceRepository {

    private val discoveredDevicesMap = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    private val knownDevicesMap = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    private val connectionStates = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())

    override fun getDiscoveredDevices(): Flow<List<BleDevice>> =
        discoveredDevicesMap.map { it.values.toList().sortedByDescending { dev -> dev.rssi } }

    override fun getKnownDevices(): Flow<List<BleDevice>> =
        knownDevicesMap.map { it.values.toList() }

    override fun getConnectedDevices(): Flow<List<BleDevice>> =
        connectionStates.map { states ->
            states.filter { it.value is ConnectionState.Connected }
                .keys
                .mapNotNull { discoveredDevicesMap.value[it] ?: knownDevicesMap.value[it] }
        }

    override fun observeConnectionState(deviceId: String): Flow<ConnectionState> =
        connectionStates.map { it[deviceId] ?: ConnectionState.Disconnected }

    override suspend fun getDeviceDetails(deviceId: String): DeviceDetails? {
        val device = discoveredDevicesMap.value[deviceId] ?: knownDevicesMap.value[deviceId] ?: return null
        return DeviceDetails(
            device = device,
            firmwareVersion = "1.2.4",
            hardwareVersion = "Rev-B2",
            serialNumber = "NX-${deviceId.takeLast(6).uppercase()}",
            manufacturer = "Nexora Open Systems",
            batteryPercentage = 95,
            isSimulated = device.id.startsWith("SIM-")
        )
    }

    private fun isPlaceholderName(name: String): Boolean {
        return name.isBlank() ||
               name.startsWith("BLE Device", ignoreCase = true) ||
               name.startsWith("Bluetooth Device", ignoreCase = true) ||
               name.startsWith("Unknown", ignoreCase = true)
    }

    override suspend fun saveDevice(device: BleDevice) {
        val current = discoveredDevicesMap.value.toMutableMap()
        val existing = current[device.id]
        val resolvedName = when {
            // If new device packet has a real name, use it immediately
            !isPlaceholderName(device.name) -> device.name
            // If existing cached entry has a real name, keep the real name
            existing != null && !isPlaceholderName(existing.name) -> existing.name
            // Otherwise use new device's name
            else -> device.name
        }
        current[device.id] = device.copy(name = resolvedName)
        discoveredDevicesMap.value = current
    }

    override suspend fun removeDevice(deviceId: String) {
        val current = knownDevicesMap.value.toMutableMap()
        current.remove(deviceId)
        knownDevicesMap.value = current
    }

    override suspend fun clearDiscoveredDevices() {
        discoveredDevicesMap.value = emptyMap()
    }

    fun updateConnectionState(deviceId: String, state: ConnectionState) {
        val current = connectionStates.value.toMutableMap()
        current[deviceId] = state
        connectionStates.value = current
    }
}

class InMemoryTelemetryRepository : TelemetryRepository {

    private val latestTelemetryMap = MutableStateFlow<Map<String, DeviceTelemetry>>(emptyMap())
    private val telemetryHistory = MutableStateFlow<Map<String, List<DeviceTelemetry>>>(emptyMap())
    private val anomaliesMap = MutableStateFlow<Map<String, List<Anomaly>>>(emptyMap())
    private val healthMap = MutableStateFlow<Map<String, DeviceHealth>>(emptyMap())
    private val syncStateFlow = MutableStateFlow(SyncState.IDLE)

    override fun observeLatestTelemetry(deviceId: String): Flow<DeviceTelemetry?> =
        latestTelemetryMap.map { it[deviceId] }

    override fun observeTelemetryHistory(deviceId: String, timeRange: TimeRange): Flow<List<DeviceTelemetry>> =
        telemetryHistory.map { history ->
            val list = history[deviceId] ?: emptyList()
            val cutoff = Clock.System.now().toEpochMilliseconds() - (timeRange.durationMinutes * 60 * 1000L)
            list.filter { it.timestamp.toEpochMilliseconds() >= cutoff }
        }

    override fun observeAnomalies(deviceId: String): Flow<List<Anomaly>> =
        anomaliesMap.map { it[deviceId] ?: emptyList() }

    override fun observeDeviceHealth(deviceId: String): Flow<DeviceHealth?> =
        healthMap.map { it[deviceId] }

    override fun observeSyncState(): Flow<SyncState> = syncStateFlow.asStateFlow()

    override suspend fun saveTelemetry(telemetry: DeviceTelemetry) {
        // Update latest
        val latest = latestTelemetryMap.value.toMutableMap()
        latest[telemetry.deviceId] = telemetry
        latestTelemetryMap.value = latest

        // Append to history (keep max 1000 samples in memory per device)
        val history = telemetryHistory.value.toMutableMap()
        val currentList = history.getOrElse(telemetry.deviceId) { emptyList() }.toMutableList()
        currentList.add(telemetry)
        if (currentList.size > 1000) {
            currentList.removeAt(0)
        }
        history[telemetry.deviceId] = currentList
        telemetryHistory.value = history
    }

    override suspend fun recordAnomaly(anomaly: Anomaly) {
        val map = anomaliesMap.value.toMutableMap()
        val list = map.getOrElse(anomaly.deviceId) { emptyList() }.toMutableList()
        if (list.none { it.id == anomaly.id }) {
            list.add(0, anomaly)
            map[anomaly.deviceId] = list
            anomaliesMap.value = map
        }
    }

    override suspend fun resolveAnomaly(anomalyId: String) {
        val map = anomaliesMap.value.toMutableMap()
        map.forEach { (deviceId, list) ->
            map[deviceId] = list.map { if (it.id == anomalyId) it.copy(isResolved = true) else it }
        }
        anomaliesMap.value = map
    }

    override suspend fun clearHistory(deviceId: String) {
        val history = telemetryHistory.value.toMutableMap()
        history.remove(deviceId)
        telemetryHistory.value = history

        val anomalies = anomaliesMap.value.toMutableMap()
        anomalies.remove(deviceId)
        anomaliesMap.value = anomalies
    }

    fun updateHealth(deviceId: String, health: DeviceHealth) {
        val current = healthMap.value.toMutableMap()
        current[deviceId] = health
        healthMap.value = current
    }

    fun setSyncState(state: SyncState) {
        syncStateFlow.value = state
    }
}

class InMemorySettingsRepository : SettingsRepository {

    private val isSimulatorMode = MutableStateFlow(true)
    private val groqApiKeyFlow = MutableStateFlow<String?>(null)
    private val groqModelFlow = MutableStateFlow("llama-3.3-70b-versatile")
    private val isAiEnabledFlow = MutableStateFlow(true)
    private val isDevModeFlow = MutableStateFlow(false)

    override fun isSimulatorModeEnabled(): Flow<Boolean> = isSimulatorMode.asStateFlow()
    override suspend fun setSimulatorModeEnabled(enabled: Boolean) {
        isSimulatorMode.value = enabled
    }

    override fun getGroqApiKey(): Flow<String?> = groqApiKeyFlow.asStateFlow()
    override suspend fun setGroqApiKey(apiKey: String?) {
        groqApiKeyFlow.value = apiKey
    }

    override fun getGroqModel(): Flow<String> = groqModelFlow.asStateFlow()
    override suspend fun setGroqModel(model: String) {
        groqModelFlow.value = model
    }

    override fun isAiEnabled(): Flow<Boolean> = isAiEnabledFlow.asStateFlow()
    override suspend fun setAiEnabled(enabled: Boolean) {
        isAiEnabledFlow.value = enabled
    }

    override fun isDeveloperModeEnabled(): Flow<Boolean> = isDevModeFlow.asStateFlow()
    override suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        isDevModeFlow.value = enabled
    }
}

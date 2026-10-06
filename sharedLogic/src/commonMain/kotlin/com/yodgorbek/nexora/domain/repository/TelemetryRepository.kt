package com.yodgorbek.nexora.domain.repository

import com.yodgorbek.nexora.domain.model.Anomaly
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.model.SyncState
import com.yodgorbek.nexora.domain.model.TimeRange
import kotlinx.coroutines.flow.Flow

interface TelemetryRepository {
    fun observeLatestTelemetry(deviceId: String): Flow<DeviceTelemetry?>
    fun observeTelemetryHistory(deviceId: String, timeRange: TimeRange): Flow<List<DeviceTelemetry>>
    fun observeAnomalies(deviceId: String): Flow<List<Anomaly>>
    fun observeDeviceHealth(deviceId: String): Flow<DeviceHealth?>
    fun observeSyncState(): Flow<SyncState>

    suspend fun saveTelemetry(telemetry: DeviceTelemetry)
    suspend fun recordAnomaly(anomaly: Anomaly)
    suspend fun resolveAnomaly(anomalyId: String)
    suspend fun clearHistory(deviceId: String)
}

interface SettingsRepository {
    fun isSimulatorModeEnabled(): Flow<Boolean>
    suspend fun setSimulatorModeEnabled(enabled: Boolean)

    fun getGroqApiKey(): Flow<String?>
    suspend fun setGroqApiKey(apiKey: String?)

    fun getGroqModel(): Flow<String>
    suspend fun setGroqModel(model: String)

    fun isAiEnabled(): Flow<Boolean>
    suspend fun setAiEnabled(enabled: Boolean)

    fun isDeveloperModeEnabled(): Flow<Boolean>
    suspend fun setDeveloperModeEnabled(enabled: Boolean)
}

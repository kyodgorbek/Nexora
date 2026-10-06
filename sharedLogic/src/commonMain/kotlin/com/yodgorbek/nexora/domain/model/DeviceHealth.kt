package com.yodgorbek.nexora.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class AnomalySeverity {
    INFO,
    WARNING,
    CRITICAL
}

@Serializable
enum class AnomalyType {
    HIGH_TEMPERATURE,
    LOW_BATTERY,
    RAPID_BATTERY_DRAIN,
    WEAK_SIGNAL,
    HIGH_PACKET_LOSS,
    FREQUENT_DISCONNECTS,
    TELEMETRY_GAP,
    SENSOR_SPIKE
}

@Serializable
data class Anomaly(
    val id: String,
    val deviceId: String,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    val title: String,
    val description: String,
    val evidence: String,
    val detectedAt: Instant = Clock.System.now(),
    val isResolved: Boolean = false,
    val aiExplanation: String? = null
)

@Serializable
data class DeviceHealth(
    val deviceId: String,
    val overallScore: Int, // 0 - 100
    val batteryHealth: Int, // 0 - 100
    val signalHealth: Int, // 0 - 100
    val stabilityScore: Int, // 0 - 100
    val telemetryConsistency: Double, // 0.0 - 1.0 (100%)
    val activeAnomaliesCount: Int,
    val disconnectCount24h: Int,
    val statusSummary: String,
    val calculatedAt: Instant = Clock.System.now()
)

@Serializable
enum class TimeRange(val title: String, val durationMinutes: Long) {
    FIVE_MINUTES("5m", 5L),
    ONE_HOUR("1h", 60L),
    SIX_HOURS("6h", 360L),
    TWENTY_FOUR_HOURS("24h", 1440L),
    SEVEN_DAYS("7d", 10080L)
}

@Serializable
enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    FAILED,
    OFFLINE
}

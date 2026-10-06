package com.yodgorbek.nexora.ai

import com.yodgorbek.nexora.domain.model.Anomaly
import com.yodgorbek.nexora.domain.model.DeviceCommand
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetryAnalysisRequest(
    val deviceId: String,
    val deviceName: String,
    val firmwareVersion: String,
    val recentTelemetry: List<DeviceTelemetry>,
    val anomalies: List<Anomaly>,
    val health: DeviceHealth,
    val userPrompt: String? = null
)

@Serializable
data class TelemetryAnalysisResult(
    val summary: String,
    val possibleCauses: List<String>,
    val evidence: List<String>,
    val confidence: Double, // 0.0 - 1.0
    val recommendations: List<String>,
    val generatedAt: Instant = Clock.System.now()
)

@Serializable
data class DeviceHealthRequest(
    val deviceId: String,
    val health: DeviceHealth,
    val recentAnomalies: List<Anomaly>
)

@Serializable
data class DeviceHealthResult(
    val diagnosis: String,
    val riskLevel: String, // "LOW", "MODERATE", "HIGH", "CRITICAL"
    val optimizationTips: List<String>,
    val generatedAt: Instant = Clock.System.now()
)

@Serializable
data class NaturalLanguageCommandRequest(
    val deviceId: String,
    val deviceName: String,
    val userPrompt: String,
    val currentSamplingInterval: Int,
    val isTelemetryEnabled: Boolean
)

@Serializable
enum class RawCommandType {
    @SerialName("SET_SAMPLING_INTERVAL")
    SET_SAMPLING_INTERVAL,

    @SerialName("SET_TELEMETRY_ENABLED")
    SET_TELEMETRY_ENABLED,

    @SerialName("RESTART_DEVICE")
    RESTART_DEVICE,

    @SerialName("CALIBRATE_SENSOR")
    CALIBRATE_SENSOR,

    @SerialName("UNKNOWN")
    UNKNOWN
}

/**
 * Raw structured output parsed directly from LLM JSON response.
 * MUST undergo schema & domain validation before converting into [DeviceCommand].
 */
@Serializable
data class RawAiCommandResponse(
    @SerialName("command")
    val command: RawCommandType = RawCommandType.UNKNOWN,
    @SerialName("seconds")
    val seconds: Int? = null,
    @SerialName("enabled")
    val enabled: Boolean? = null,
    @SerialName("sensor_type")
    val sensorType: String? = null,
    @SerialName("offset")
    val offset: Double? = null,
    @SerialName("explanation")
    val explanation: String = "",
    @SerialName("confidence")
    val confidence: Double = 0.0
)

@Serializable
data class StructuredDeviceCommand(
    val targetDeviceId: String,
    val command: DeviceCommand?,
    val rawExplanation: String,
    val confidence: Double,
    val isValid: Boolean,
    val validationError: String? = null,
    val requiresUserConfirmation: Boolean = true
)

@Serializable
data class AiInsight(
    val id: String,
    val deviceId: String,
    val title: String,
    val summary: String,
    val severity: String,
    val recommendations: List<String>,
    val timestamp: Instant = Clock.System.now()
)

package com.yodgorbek.nexora.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTelemetry(
    val deviceId: String,
    val timestamp: Instant = Clock.System.now(),
    val batteryPercentage: Int,
    val temperature: Double?,
    val humidity: Double?,
    val signalStrength: Int?,
    val values: Map<String, Double> = emptyMap(),
    val sequenceNumber: Long = 0L,
    val isValid: Boolean = true
)

@Serializable
data class TelemetrySample(
    val timestamp: Instant,
    val value: Double
)

@Serializable
data class TelemetrySeries(
    val deviceId: String,
    val metricName: String,
    val unit: String,
    val samples: List<TelemetrySample>,
    val minValue: Double,
    val maxValue: Double,
    val averageValue: Double,
    val latestValue: Double
)

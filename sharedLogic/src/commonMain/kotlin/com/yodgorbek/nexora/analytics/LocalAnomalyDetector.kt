package com.yodgorbek.nexora.analytics

import com.yodgorbek.nexora.domain.model.Anomaly
import com.yodgorbek.nexora.domain.model.AnomalySeverity
import com.yodgorbek.nexora.domain.model.AnomalyType
import com.yodgorbek.nexora.domain.model.DeviceHealth
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import kotlinx.datetime.Clock
import kotlin.math.sqrt

class LocalAnomalyDetector {

    /**
     * Evaluates a sequence of telemetry readings deterministically to identify physical or communication anomalies.
     */
    fun detectAnomalies(
        deviceId: String,
        recentTelemetry: List<DeviceTelemetry>,
        disconnectCount24h: Int = 0
    ): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        if (recentTelemetry.isEmpty()) return anomalies

        val latest = recentTelemetry.last()

        // 1. High Temperature Detection (> 50.0°C warning, > 70.0°C critical)
        latest.temperature?.let { temp ->
            if (temp >= 70.0) {
                anomalies.add(
                    Anomaly(
                        id = "ANOM-TEMP-CRIT-$deviceId",
                        deviceId = deviceId,
                        type = AnomalyType.HIGH_TEMPERATURE,
                        severity = AnomalySeverity.CRITICAL,
                        title = "Critical Thermal Overheat Detected",
                        description = "Sensor core temperature reached ${formatDouble(temp)}°C, exceeding safe operating envelope (70°C).",
                        evidence = "Latest reading: ${formatDouble(temp)}°C at ${latest.timestamp}"
                    )
                )
            } else if (temp >= 50.0) {
                anomalies.add(
                    Anomaly(
                        id = "ANOM-TEMP-WARN-$deviceId",
                        deviceId = deviceId,
                        type = AnomalyType.HIGH_TEMPERATURE,
                        severity = AnomalySeverity.WARNING,
                        title = "Elevated Temperature Warning",
                        description = "Sensor temperature is high (${formatDouble(temp)}°C). Prolonged exposure may degrade battery chemistry.",
                        evidence = "Reading: ${formatDouble(temp)}°C"
                    )
                )
            }
        }

        // 2. Low Battery (< 15% warning, < 5% critical)
        if (latest.batteryPercentage <= 5) {
            anomalies.add(
                Anomaly(
                    id = "ANOM-BATT-CRIT-$deviceId",
                    deviceId = deviceId,
                    type = AnomalyType.LOW_BATTERY,
                    severity = AnomalySeverity.CRITICAL,
                    title = "Battery Level Critical",
                    description = "Remaining capacity is ${latest.batteryPercentage}%. Device may shut down abruptly.",
                    evidence = "Capacity: ${latest.batteryPercentage}%"
                )
            )
        } else if (latest.batteryPercentage <= 15) {
            anomalies.add(
                Anomaly(
                    id = "ANOM-BATT-WARN-$deviceId",
                    deviceId = deviceId,
                    type = AnomalyType.LOW_BATTERY,
                    severity = AnomalySeverity.WARNING,
                    title = "Low Battery Alert",
                    description = "Remaining battery is ${latest.batteryPercentage}%. Recharge or connect external power soon.",
                    evidence = "Capacity: ${latest.batteryPercentage}%"
                )
            )
        }

        // 3. Rapid Battery Drain Velocity (e.g. > 5% drop over fewer than 10 samples)
        if (recentTelemetry.size >= 5) {
            val firstBatt = recentTelemetry.first().batteryPercentage
            val lastBatt = recentTelemetry.last().batteryPercentage
            val delta = firstBatt - lastBatt
            if (delta >= 6) {
                anomalies.add(
                    Anomaly(
                        id = "ANOM-BATT-DRAIN-$deviceId",
                        deviceId = deviceId,
                        type = AnomalyType.RAPID_BATTERY_DRAIN,
                        severity = AnomalySeverity.WARNING,
                        title = "Abnormal Battery Discharge Rate",
                        description = "Discharge rate of $delta% over ${recentTelemetry.size} samples exceeds nominal baseline.",
                        evidence = "Initial: $firstBatt%, Current: $lastBatt%"
                    )
                )
            }
        }

        // 4. Weak BLE Signal Quality (RSSI < -95 dBm)
        latest.signalStrength?.let { rssi ->
            if (rssi <= -95) {
                anomalies.add(
                    Anomaly(
                        id = "ANOM-WEAK-SIG-$deviceId",
                        deviceId = deviceId,
                        type = AnomalyType.WEAK_SIGNAL,
                        severity = AnomalySeverity.WARNING,
                        title = "Weak Bluetooth Link Quality",
                        description = "Received signal strength ($rssi dBm) is near receiver sensitivity threshold.",
                        evidence = "RSSI: $rssi dBm"
                    )
                )
            }
        }

        // 5. Frequent Link Disconnects
        if (disconnectCount24h >= 4) {
            anomalies.add(
                Anomaly(
                    id = "ANOM-DISCONN-$deviceId",
                    deviceId = deviceId,
                    type = AnomalyType.FREQUENT_DISCONNECTS,
                    severity = AnomalySeverity.WARNING,
                    title = "Frequent Connection Drops",
                    description = "Device has experienced $disconnectCount24h connection losses in the past 24 hours.",
                    evidence = "Disconnect events: $disconnectCount24h"
                )
            )
        }

        return anomalies
    }

    private fun formatDouble(value: Double): String {
        val rounded = (value * 10).toInt() / 10.0
        return rounded.toString()
    }
}

class DeviceHealthCalculator {

    fun calculateHealth(
        deviceId: String,
        recentTelemetry: List<DeviceTelemetry>,
        activeAnomalies: List<Anomaly>,
        disconnectCount24h: Int = 0
    ): DeviceHealth {
        if (recentTelemetry.isEmpty()) {
            return DeviceHealth(
                deviceId = deviceId,
                overallScore = 100,
                batteryHealth = 100,
                signalHealth = 100,
                stabilityScore = 100,
                telemetryConsistency = 1.0,
                activeAnomaliesCount = 0,
                disconnectCount24h = 0,
                statusSummary = "Healthy (No telemetry received yet)"
            )
        }

        val latest = recentTelemetry.last()

        // Battery score
        val batteryScore = latest.batteryPercentage.coerceIn(0, 100)

        // Signal score (-110 dBm = 0%, -50 dBm = 100%)
        val rssi = latest.signalStrength ?: -70
        val signalScore = (((rssi - (-110)) / 60.0) * 100).toInt().coerceIn(0, 100)

        // Stability score (penalize disconnects and critical anomalies)
        var stability = 100 - (disconnectCount24h * 12)
        activeAnomalies.forEach { anomaly ->
            stability -= when (anomaly.severity) {
                AnomalySeverity.CRITICAL -> 25
                AnomalySeverity.WARNING -> 10
                AnomalySeverity.INFO -> 2
            }
        }
        val stabilityScore = stability.coerceIn(0, 100)

        // Telemetry consistency (ratio of valid packets)
        val validCount = recentTelemetry.count { it.isValid }
        val consistency = if (recentTelemetry.isNotEmpty()) validCount.toDouble() / recentTelemetry.size else 1.0

        // Overall composite score
        val overall = (batteryScore * 0.25 + signalScore * 0.25 + stabilityScore * 0.50).toInt().coerceIn(0, 100)

        val summary = when {
            overall >= 85 -> "Optimal Condition"
            overall >= 65 -> "Fair Condition (Monitoring)"
            overall >= 40 -> "Degraded Performance"
            else -> "Critical Attention Required"
        }

        return DeviceHealth(
            deviceId = deviceId,
            overallScore = overall,
            batteryHealth = batteryScore,
            signalHealth = signalScore,
            stabilityScore = stabilityScore,
            telemetryConsistency = consistency,
            activeAnomaliesCount = activeAnomalies.size,
            disconnectCount24h = disconnectCount24h,
            statusSummary = summary,
            calculatedAt = Clock.System.now()
        )
    }
}

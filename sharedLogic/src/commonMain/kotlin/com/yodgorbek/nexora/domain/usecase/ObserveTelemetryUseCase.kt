package com.yodgorbek.nexora.domain.usecase

import com.yodgorbek.nexora.analytics.DeviceHealthCalculator
import com.yodgorbek.nexora.analytics.LocalAnomalyDetector
import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.ble.protocol.GattSpec
import com.yodgorbek.nexora.ble.protocol.TelemetryPacketParser
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.model.TimeRange
import com.yodgorbek.nexora.domain.repository.TelemetryRepository
import com.yodgorbek.nexora.domain.validation.TelemetryValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class ObserveTelemetryUseCase(
    private val bleManager: BleManager,
    private val telemetryRepository: TelemetryRepository,
    private val telemetryValidator: TelemetryValidator = TelemetryValidator(),
    private val anomalyDetector: LocalAnomalyDetector = LocalAnomalyDetector(),
    private val healthCalculator: DeviceHealthCalculator = DeviceHealthCalculator()
) {
    operator fun invoke(deviceId: String): Flow<DeviceTelemetry> = flow {
        bleManager.observeCharacteristic(
            deviceId = deviceId,
            serviceUuid = GattSpec.SERVICE_TELEMETRY,
            characteristicUuid = GattSpec.CHAR_TELEMETRY_STREAM
        ).collect { rawBytes ->
            val parsed = TelemetryPacketParser.parse(deviceId, rawBytes)
            if (parsed != null) {
                val validated = telemetryValidator.validate(parsed)
                telemetryRepository.saveTelemetry(validated)

                // Run real-time local anomaly detection
                val history = telemetryRepository.observeTelemetryHistory(deviceId, TimeRange.FIVE_MINUTES).first()
                val anomalies = anomalyDetector.detectAnomalies(deviceId, history)
                anomalies.forEach { anomaly ->
                    telemetryRepository.recordAnomaly(anomaly)
                }

                emit(validated)
            }
        }
    }
}

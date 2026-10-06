package com.yodgorbek.nexora

import com.yodgorbek.nexora.ai.RawAiCommandResponse
import com.yodgorbek.nexora.ai.RawCommandType
import com.yodgorbek.nexora.analytics.DeviceHealthCalculator
import com.yodgorbek.nexora.analytics.LocalAnomalyDetector
import com.yodgorbek.nexora.ble.protocol.CommandPacketEncoder
import com.yodgorbek.nexora.ble.protocol.GattSpec
import com.yodgorbek.nexora.ble.protocol.TelemetryPacketParser
import com.yodgorbek.nexora.ble.simulator.BleDeviceSimulator
import com.yodgorbek.nexora.ble.simulator.SimulatorScenario
import com.yodgorbek.nexora.domain.model.AnomalySeverity
import com.yodgorbek.nexora.domain.model.AnomalyType
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceCommand
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.validation.AiResponseValidator
import com.yodgorbek.nexora.domain.validation.CommandValidator
import com.yodgorbek.nexora.domain.validation.TelemetryValidator
import com.yodgorbek.nexora.domain.validation.ValidationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NexoraCoreTestSuite {

    private val commandValidator = CommandValidator()
    private val telemetryValidator = TelemetryValidator()
    private val aiResponseValidator = AiResponseValidator()
    private val anomalyDetector = LocalAnomalyDetector()
    private val healthCalculator = DeviceHealthCalculator()

    @Test
    fun testCommandValidation_validatesSamplingIntervalBounds() {
        val capabilities = DeviceCapabilities(
            supportsSamplingInterval = true,
            minSamplingIntervalSeconds = 1,
            maxSamplingIntervalSeconds = 60
        )

        val validCmd = DeviceCommand.SetSamplingInterval(10)
        val result = commandValidator.validate(validCmd, capabilities, ConnectionState.Connected)
        assertTrue(result is ValidationResult.Valid)

        val outOfBoundsCmd = DeviceCommand.SetSamplingInterval(120)
        val invalidResult = commandValidator.validate(outOfBoundsCmd, capabilities, ConnectionState.Connected)
        assertTrue(invalidResult is ValidationResult.Invalid)
    }

    @Test
    fun testCommandValidation_rejectsWhenDisconnected() {
        val capabilities = DeviceCapabilities.FULL
        val cmd = DeviceCommand.RestartDevice

        val result = commandValidator.validate(cmd, capabilities, ConnectionState.Disconnected)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("not connected"))
    }

    @Test
    fun testTelemetryPacketParser_encodesAndDecodesCorrectly() {
        val originalBytes = TelemetryPacketParser.encode(
            seq = 42,
            temperatureCelsius = 25.50,
            humidityPercent = 60.20,
            batteryPercent = 88,
            rssi = -64,
            pressureHpa = 1013.25
        )

        val parsed = TelemetryPacketParser.parse("TEST-DEVICE-01", originalBytes)
        assertNotNull(parsed)
        assertEquals("TEST-DEVICE-01", parsed.deviceId)
        assertEquals(42L, parsed.sequenceNumber)
        assertEquals(25.50, parsed.temperature)
        assertEquals(60.20, parsed.humidity)
        assertEquals(88, parsed.batteryPercentage)
        assertEquals(-64, parsed.signalStrength)
    }

    @Test
    fun testTelemetryValidator_identifiesInvalidReadings() {
        val invalidSample = DeviceTelemetry(
            deviceId = "DEV-01",
            batteryPercentage = 150, // Invalid > 100%
            temperature = 25.0,
            humidity = 50.0,
            signalStrength = -60
        )

        val validated = telemetryValidator.validate(invalidSample)
        assertFalse(validated.isValid)
    }

    @Test
    fun testLocalAnomalyDetector_detectsThermalRunaway() {
        val telemetryList = listOf(
            DeviceTelemetry(deviceId = "DEV-01", batteryPercentage = 80, temperature = 40.0, humidity = 40.0, signalStrength = -60),
            DeviceTelemetry(deviceId = "DEV-01", batteryPercentage = 80, temperature = 76.5, humidity = 40.0, signalStrength = -60)
        )

        val anomalies = anomalyDetector.detectAnomalies("DEV-01", telemetryList)
        assertEquals(1, anomalies.size)
        assertEquals(AnomalyType.HIGH_TEMPERATURE, anomalies.first().type)
        assertEquals(AnomalySeverity.CRITICAL, anomalies.first().severity)
    }

    @Test
    fun testLocalAnomalyDetector_detectsLowBattery() {
        val telemetryList = listOf(
            DeviceTelemetry(deviceId = "DEV-01", batteryPercentage = 4, temperature = 22.0, humidity = 40.0, signalStrength = -60)
        )

        val anomalies = anomalyDetector.detectAnomalies("DEV-01", telemetryList)
        assertEquals(1, anomalies.size)
        assertEquals(AnomalyType.LOW_BATTERY, anomalies.first().type)
        assertEquals(AnomalySeverity.CRITICAL, anomalies.first().severity)
    }

    @Test
    fun testAiResponseValidator_strictlyValidatesAndMapsSafeCommands() {
        val rawAiResponse = RawAiCommandResponse(
            command = RawCommandType.SET_SAMPLING_INTERVAL,
            seconds = 15,
            explanation = "User requested 15-second sampling cycle.",
            confidence = 0.99
        )

        val mapped = aiResponseValidator.validateAndMap(
            rawResponse = rawAiResponse,
            targetDeviceId = "DEV-01",
            capabilities = DeviceCapabilities.FULL,
            connectionState = ConnectionState.Connected
        )

        assertTrue(mapped.isValid)
        assertTrue(mapped.command is DeviceCommand.SetSamplingInterval)
        assertEquals(15, (mapped.command as DeviceCommand.SetSamplingInterval).seconds)
        assertTrue(mapped.requiresUserConfirmation)
    }

    @Test
    fun testAiResponseValidator_rejectsUnknownCommands() {
        val rawAiResponse = RawAiCommandResponse(
            command = RawCommandType.UNKNOWN,
            explanation = "I cannot translate this command.",
            confidence = 0.1
        )

        val mapped = aiResponseValidator.validateAndMap(
            rawResponse = rawAiResponse,
            targetDeviceId = "DEV-01",
            capabilities = DeviceCapabilities.FULL,
            connectionState = ConnectionState.Connected
        )

        assertFalse(mapped.isValid)
        assertNull(mapped.command)
    }

    @Test
    fun testBleDeviceSimulator_scansAndConnectsDeterministically() = runTest {
        val simulator = BleDeviceSimulator()
        val firstDevice = simulator.scan().first()
        assertNotNull(firstDevice)
        assertTrue(firstDevice.id.startsWith("SIM-NX-"))

        // Connect
        simulator.connect(SimulatorScenario.NORMAL.targetDeviceId)
        val state = simulator.observeConnectionState(SimulatorScenario.NORMAL.targetDeviceId).first()
        assertEquals(ConnectionState.Connected, state)

        // Read Model Number
        val bytes = simulator.readCharacteristic(
            SimulatorScenario.NORMAL.targetDeviceId,
            GattSpec.SERVICE_DEVICE_INFO,
            GattSpec.CHAR_MODEL_NUMBER
        )
        assertEquals("NX-SIM-V1", bytes.decodeToString())
    }
}

package com.yodgorbek.nexora.ble.simulator

import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.ble.model.BleCharacteristic
import com.yodgorbek.nexora.ble.model.BleService
import com.yodgorbek.nexora.ble.model.CharacteristicProperty
import com.yodgorbek.nexora.ble.protocol.GattSpec
import com.yodgorbek.nexora.ble.protocol.TelemetryPacketParser
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

class BleDeviceSimulator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : BleManager {

    private val activeConnections = mutableMapOf<String, MutableStateFlow<ConnectionState>>()
    private val telemetryFlows = mutableMapOf<String, MutableSharedFlow<ByteArray>>()
    private val simulationJobs = mutableMapOf<String, Job>()

    // Configurable simulated device parameters
    private val samplingIntervals = mutableMapOf<String, Int>()
    private val telemetryEnabledMap = mutableMapOf<String, Boolean>()
    private val batteryLevels = mutableMapOf<String, Int>()
    private val currentTemperatures = mutableMapOf<String, Double>()

    private val simulatedDevices: List<BleDevice> = SimulatorScenario.entries.map { scenario ->
        BleDevice(
            id = scenario.targetDeviceId,
            name = scenario.deviceName,
            address = "SIM:${scenario.targetDeviceId.takeLast(8)}",
            rssi = when (scenario) {
                SimulatorScenario.WEAK_SIGNAL -> -102
                SimulatorScenario.LOW_BATTERY -> -75
                else -> -58
            },
            isConnectable = scenario != SimulatorScenario.DISCONNECTED,
            deviceType = DeviceType.SIMULATED_PROBE,
            lastSeen = Clock.System.now(),
            capabilities = if (scenario == SimulatorScenario.WEAK_SIGNAL) {
                DeviceCapabilities.READ_ONLY
            } else {
                DeviceCapabilities.FULL
            }
        )
    }

    init {
        // Initialize default device values
        SimulatorScenario.entries.forEach { scenario ->
            val id = scenario.targetDeviceId
            activeConnections[id] = MutableStateFlow(ConnectionState.Disconnected)
            telemetryFlows[id] = MutableSharedFlow(replay = 1)
            samplingIntervals[id] = 2 // 2 seconds default interval
            telemetryEnabledMap[id] = true

            batteryLevels[id] = when (scenario) {
                SimulatorScenario.LOW_BATTERY -> 14
                SimulatorScenario.HIGH_TEMPERATURE -> 88
                else -> 96
            }

            currentTemperatures[id] = when (scenario) {
                SimulatorScenario.HIGH_TEMPERATURE -> 42.0
                else -> 22.4
            }
        }
    }

    override fun scan(): Flow<BleDevice> = flow {
        while (true) {
            simulatedDevices.forEach { device ->
                // Add minor jitter to simulated RSSI
                val jitter = Random.nextInt(-3, 4)
                emit(device.copy(rssi = (device.rssi + jitter).coerceIn(-120, -30), lastSeen = Clock.System.now()))
                delay(120)
            }
            delay(1500)
        }
    }

    override suspend fun connect(deviceId: String) {
        val stateFlow = activeConnections.getOrPut(deviceId) { MutableStateFlow(ConnectionState.Disconnected) }

        if (deviceId == SimulatorScenario.DISCONNECTED.targetDeviceId) {
            stateFlow.value = ConnectionState.Connecting
            delay(1200)
            stateFlow.value = ConnectionState.Error("Connection rejected: Device timed out or unreachable.", errorCode = 133)
            return
        }

        stateFlow.value = ConnectionState.Connecting
        delay(600) // Simulate connection handshake
        stateFlow.value = ConnectionState.Connected

        startTelemetryLoop(deviceId)
    }

    override suspend fun disconnect(deviceId: String) {
        simulationJobs[deviceId]?.cancel()
        simulationJobs.remove(deviceId)

        val stateFlow = activeConnections[deviceId]
        if (stateFlow != null) {
            stateFlow.value = ConnectionState.Disconnecting
            delay(200)
            stateFlow.value = ConnectionState.Disconnected
        }
    }

    override fun observeConnectionState(deviceId: String): Flow<ConnectionState> {
        return activeConnections.getOrPut(deviceId) { MutableStateFlow(ConnectionState.Disconnected) }.asStateFlow()
    }

    override suspend fun discoverServices(deviceId: String): List<BleService> {
        delay(300)
        return listOf(
            BleService(
                uuid = GattSpec.SERVICE_DEVICE_INFO,
                name = "Device Information Service",
                characteristics = listOf(
                    BleCharacteristic(GattSpec.CHAR_MODEL_NUMBER, GattSpec.SERVICE_DEVICE_INFO, listOf(CharacteristicProperty.READ), "Model Number"),
                    BleCharacteristic(GattSpec.CHAR_FIRMWARE_REVISION, GattSpec.SERVICE_DEVICE_INFO, listOf(CharacteristicProperty.READ), "Firmware Revision"),
                    BleCharacteristic(GattSpec.CHAR_HARDWARE_REVISION, GattSpec.SERVICE_DEVICE_INFO, listOf(CharacteristicProperty.READ), "Hardware Revision"),
                    BleCharacteristic(GattSpec.CHAR_MANUFACTURER_NAME, GattSpec.SERVICE_DEVICE_INFO, listOf(CharacteristicProperty.READ), "Manufacturer Name")
                )
            ),
            BleService(
                uuid = GattSpec.SERVICE_BATTERY,
                name = "Battery Service",
                characteristics = listOf(
                    BleCharacteristic(GattSpec.CHAR_BATTERY_LEVEL, GattSpec.SERVICE_BATTERY, listOf(CharacteristicProperty.READ, CharacteristicProperty.NOTIFY), "Battery Level")
                )
            ),
            BleService(
                uuid = GattSpec.SERVICE_TELEMETRY,
                name = "Nexora Telemetry Stream",
                characteristics = listOf(
                    BleCharacteristic(GattSpec.CHAR_TELEMETRY_STREAM, GattSpec.SERVICE_TELEMETRY, listOf(CharacteristicProperty.NOTIFY), "Telemetry Packet")
                )
            ),
            BleService(
                uuid = GattSpec.SERVICE_COMMAND,
                name = "Nexora Command Service",
                characteristics = listOf(
                    BleCharacteristic(GattSpec.CHAR_COMMAND_WRITE, GattSpec.SERVICE_COMMAND, listOf(CharacteristicProperty.WRITE), "Command In"),
                    BleCharacteristic(GattSpec.CHAR_COMMAND_NOTIFY, GattSpec.SERVICE_COMMAND, listOf(CharacteristicProperty.NOTIFY), "Command Response")
                )
            )
        )
    }

    override suspend fun readCharacteristic(deviceId: String, serviceUuid: String, characteristicUuid: String): ByteArray {
        delay(100)
        return when (characteristicUuid.lowercase()) {
            GattSpec.CHAR_MODEL_NUMBER -> "NX-SIM-V1".encodeToByteArray()
            GattSpec.CHAR_FIRMWARE_REVISION -> "1.2.4-sim".encodeToByteArray()
            GattSpec.CHAR_HARDWARE_REVISION -> "Rev-C3".encodeToByteArray()
            GattSpec.CHAR_MANUFACTURER_NAME -> "Nexora Open Systems".encodeToByteArray()
            GattSpec.CHAR_BATTERY_LEVEL -> byteArrayOf((batteryLevels[deviceId] ?: 90).toByte())
            else -> byteArrayOf(0x00)
        }
    }

    override suspend fun writeCharacteristic(deviceId: String, serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        delay(150)
        if (data.isEmpty()) return

        when (data[0].toInt()) {
            GattSpec.CMD_SET_SAMPLING_INTERVAL -> {
                if (data.size >= 3) {
                    val seconds = ((data[1].toInt() and 0xFF) shl 8) or (data[2].toInt() and 0xFF)
                    samplingIntervals[deviceId] = seconds.coerceIn(1, 3600)
                }
            }
            GattSpec.CMD_SET_TELEMETRY_ENABLED -> {
                if (data.size >= 2) {
                    telemetryEnabledMap[deviceId] = (data[1].toInt() != 0)
                }
            }
            GattSpec.CMD_RESTART -> {
                // Restart simulation
                scope.launch {
                    activeConnections[deviceId]?.value = ConnectionState.Reconnecting(attempt = 1, maxAttempts = 3, delayMillis = 1500)
                    simulationJobs[deviceId]?.cancel()
                    delay(1500)
                    activeConnections[deviceId]?.value = ConnectionState.Connected
                    startTelemetryLoop(deviceId)
                }
            }
        }
    }

    override fun observeCharacteristic(deviceId: String, serviceUuid: String, characteristicUuid: String): Flow<ByteArray> {
        return telemetryFlows.getOrPut(deviceId) { MutableSharedFlow(replay = 1) }.asSharedFlow()
    }

    private fun startTelemetryLoop(deviceId: String) {
        simulationJobs[deviceId]?.cancel()

        simulationJobs[deviceId] = scope.launch {
            var seq = 1
            val flow = telemetryFlows.getOrPut(deviceId) { MutableSharedFlow(replay = 1) }
            val scenario = SimulatorScenario.entries.find { it.targetDeviceId == deviceId } ?: SimulatorScenario.NORMAL

            var secondsInRun = 0

            while (isActive) {
                val interval = (samplingIntervals[deviceId] ?: 2).coerceAtLeast(1)
                delay(interval * 1000L)
                secondsInRun += interval

                if (telemetryEnabledMap[deviceId] == false) {
                    continue
                }

                // Handle Unstable Device Disconnect Cycle
                if (scenario == SimulatorScenario.UNSTABLE && secondsInRun % 18 == 0) {
                    val conn = activeConnections[deviceId]
                    conn?.value = ConnectionState.Error("Link loss: Simulated link drop", errorCode = 19, recoverable = true)
                    delay(2000)
                    conn?.value = ConnectionState.Reconnecting(attempt = 1, maxAttempts = 3, delayMillis = 1500)
                    delay(1500)
                    conn?.value = ConnectionState.Connected
                }

                // Update scenario variables
                var temp = currentTemperatures[deviceId] ?: 22.5
                var batt = batteryLevels[deviceId] ?: 90
                var rssi = -60

                when (scenario) {
                    SimulatorScenario.NORMAL -> {
                        temp += Random.nextDouble(-0.3, 0.3)
                        rssi = Random.nextInt(-65, -55)
                        if (seq % 30 == 0 && batt > 1) batt -= 1
                    }
                    SimulatorScenario.LOW_BATTERY -> {
                        if (batt > 2) batt -= 2 // Fast drain
                        temp += Random.nextDouble(-0.2, 0.2)
                        rssi = Random.nextInt(-78, -70)
                    }
                    SimulatorScenario.HIGH_TEMPERATURE -> {
                        temp += Random.nextDouble(1.2, 2.8) // Escalating heat
                        rssi = Random.nextInt(-65, -58)
                    }
                    SimulatorScenario.WEAK_SIGNAL -> {
                        rssi = Random.nextInt(-112, -92)
                        temp += Random.nextDouble(-0.1, 0.1)
                        if (Random.nextFloat() < 0.35f) {
                            // Drop packet completely
                            continue
                        }
                    }
                    SimulatorScenario.PACKET_LOSS -> {
                        if (Random.nextFloat() < 0.40f) {
                            // Emit corrupted byte frame
                            flow.emit(byteArrayOf(0x00, 0x00, 0x12, 0x34))
                            continue
                        }
                    }
                    else -> Unit
                }

                currentTemperatures[deviceId] = temp.coerceIn(-20.0, 110.0)
                batteryLevels[deviceId] = batt.coerceIn(0, 100)

                val packet = TelemetryPacketParser.encode(
                    seq = seq++,
                    temperatureCelsius = temp,
                    humidityPercent = (45.0 + Random.nextDouble(-2.0, 2.0)).coerceIn(10.0, 99.0),
                    batteryPercent = batt,
                    rssi = rssi,
                    pressureHpa = 1013.25 + Random.nextDouble(-1.0, 1.0)
                )

                flow.emit(packet)
            }
        }
    }
}

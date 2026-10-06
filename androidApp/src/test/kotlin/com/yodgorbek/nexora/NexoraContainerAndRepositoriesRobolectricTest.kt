package com.yodgorbek.nexora

import app.cash.turbine.test
import com.yodgorbek.nexora.data.repository.InMemoryDeviceRepository
import com.yodgorbek.nexora.data.repository.InMemorySettingsRepository
import com.yodgorbek.nexora.data.repository.InMemoryTelemetryRepository
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import com.yodgorbek.nexora.domain.model.DeviceType
import com.yodgorbek.nexora.domain.model.TimeRange
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NexoraContainerAndRepositoriesRobolectricTest {

    private lateinit var deviceRepository: InMemoryDeviceRepository
    private lateinit var telemetryRepository: InMemoryTelemetryRepository
    private lateinit var settingsRepository: InMemorySettingsRepository

    @Before
    fun setUp() {
        deviceRepository = InMemoryDeviceRepository()
        telemetryRepository = InMemoryTelemetryRepository()
        settingsRepository = InMemorySettingsRepository()
    }

    @Test
    fun testDeviceRepository_saveAndObserveDiscoveredDevices_withTurbine() = runTest {
        val testDevice = BleDevice(
            id = "AA:BB:CC:11:22:33",
            name = "Samsung Galaxy A16",
            address = "AA:BB:CC:11:22:33",
            rssi = -55,
            isConnectable = true,
            deviceType = DeviceType.SENSOR_NODE,
            lastSeen = Clock.System.now(),
            capabilities = DeviceCapabilities.DEFAULT
        )

        deviceRepository.getDiscoveredDevices().test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            deviceRepository.saveDevice(testDevice)
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("Samsung Galaxy A16", updated.first().name)
            assertEquals("AA:BB:CC:11:22:33", updated.first().id)
        }
    }

    @Test
    fun testDeviceRepository_updatesConnectionState() = runTest {
        val deviceId = "AA:BB:CC:11:22:33"
        deviceRepository.observeConnectionState(deviceId).test {
            assertEquals(ConnectionState.Disconnected, awaitItem())

            deviceRepository.updateConnectionState(deviceId, ConnectionState.Connected)
            assertEquals(ConnectionState.Connected, awaitItem())
        }
    }

    @Test
    fun testTelemetryRepository_saveAndObserveTelemetryStream() = runTest {
        val deviceId = "NODE-NX-01"
        val sampleTelemetry = DeviceTelemetry(
            deviceId = deviceId,
            sequenceNumber = 1L,
            batteryPercentage = 94,
            temperature = 23.4,
            humidity = 49.0,
            signalStrength = -58,
            values = mapOf("pressureHpa" to 1013.25)
        )

        telemetryRepository.observeLatestTelemetry(deviceId).test {
            assertEquals(null, awaitItem())

            telemetryRepository.saveTelemetry(sampleTelemetry)
            val received = awaitItem()
            assertEquals(23.4, received?.temperature)
            assertEquals(94, received?.batteryPercentage)
        }

        val history = telemetryRepository.observeTelemetryHistory(deviceId, TimeRange.FIVE_MINUTES)
        history.test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(1L, list.first().sequenceNumber)
        }
    }

    @Test
    fun testSettingsRepository_toggleSimulatorMode() = runTest {
        settingsRepository.isSimulatorModeEnabled().test {
            assertTrue(awaitItem()) // default is true

            settingsRepository.setSimulatorModeEnabled(false)
            assertFalse(awaitItem())
        }
    }
}

package com.yodgorbek.nexora

import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.domain.repository.BleDeviceRepository
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceType
import com.yodgorbek.nexora.domain.usecase.ConnectDeviceUseCase
import com.yodgorbek.nexora.domain.usecase.DisconnectDeviceUseCase
import com.yodgorbek.nexora.domain.usecase.ScanDevicesUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceUseCasesMockitoTest {

    private lateinit var mockBleManager: BleManager
    private lateinit var mockDeviceRepository: BleDeviceRepository

    private lateinit var connectDeviceUseCase: ConnectDeviceUseCase
    private lateinit var disconnectDeviceUseCase: DisconnectDeviceUseCase
    private lateinit var scanDevicesUseCase: ScanDevicesUseCase

    @Before
    fun setUp() {
        mockBleManager = mock()
        mockDeviceRepository = mock()

        connectDeviceUseCase = ConnectDeviceUseCase(mockBleManager)
        disconnectDeviceUseCase = DisconnectDeviceUseCase(mockBleManager)
        scanDevicesUseCase = ScanDevicesUseCase(mockBleManager, mockDeviceRepository)
    }

    @Test
    fun testConnectDeviceUseCase_delegatesToManager() = runTest {
        val deviceId = "MOCK-MAC-ADDRESS"

        connectDeviceUseCase.invoke(deviceId)

        verify(mockBleManager).connect(deviceId)
    }

    @Test
    fun testDisconnectDeviceUseCase_delegatesToManager() = runTest {
        val deviceId = "MOCK-MAC-ADDRESS-2"

        disconnectDeviceUseCase.invoke(deviceId)

        verify(mockBleManager).disconnect(deviceId)
    }

    @Test
    fun testScanDevicesUseCase_savesScannedDevicesToRepository() = runTest {
        val dummyDevice = BleDevice(
            id = "ID-1",
            name = "Test Sensor",
            address = "ID-1",
            rssi = -50,
            isConnectable = true,
            deviceType = DeviceType.SENSOR_NODE,
            lastSeen = Clock.System.now(),
            capabilities = DeviceCapabilities.DEFAULT
        )

        whenever(mockBleManager.scan()).thenReturn(flowOf(dummyDevice))

        val scannedList = scanDevicesUseCase.invoke().toList()

        assertEquals(1, scannedList.size)
        assertEquals("ID-1", scannedList[0].id)
        verify(mockDeviceRepository).saveDevice(dummyDevice)
    }
}

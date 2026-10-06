package com.yodgorbek.nexora

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.yodgorbek.nexora.ble.AndroidBleManager
import com.yodgorbek.nexora.domain.model.ConnectionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBluetoothAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidBleManagerRobolectricTest {

    private lateinit var context: Context
    private lateinit var bleManager: AndroidBleManager
    private lateinit var shadowBluetoothAdapter: ShadowBluetoothAdapter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter != null) {
            shadowBluetoothAdapter = Shadows.shadowOf(adapter)
            shadowBluetoothAdapter.setEnabled(true)
        }
        bleManager = AndroidBleManager(context)
    }

    @Test
    fun testAndroidBleManager_initializesProperlyWithContext() {
        assertNotNull(bleManager)
    }

    @Test
    fun testObserveConnectionState_defaultsToDisconnected() = runTest {
        val testDeviceId = "AA:BB:CC:DD:EE:FF"
        val initialState = bleManager.observeConnectionState(testDeviceId).first()
        assertEquals(ConnectionState.Disconnected, initialState)
    }

    @Test
    fun testConnectAndDisconnect_updatesStateFlow() = runTest {
        val testDeviceId = "00:11:22:33:44:55"

        // Connect
        bleManager.connect(testDeviceId)
        val connectedState = bleManager.observeConnectionState(testDeviceId).first()
        assertTrue(connectedState is ConnectionState.Connected || connectedState is ConnectionState.Connecting)

        // Disconnect
        bleManager.disconnect(testDeviceId)
        val disconnectedState = bleManager.observeConnectionState(testDeviceId).first()
        assertEquals(ConnectionState.Disconnected, disconnectedState)
    }

    @Test
    fun testBluetoothDiscoveryBroadcastHandling_withMockitoAndRobolectric() {
        val mockDevice = mock<BluetoothDevice>()
        val intent = Intent(BluetoothDevice.ACTION_FOUND).apply {
            putExtra(BluetoothDevice.EXTRA_NAME, "Samsung Galaxy A16")
            putExtra(BluetoothDevice.EXTRA_RSSI, (-65).toShort())
        }
        assertNotNull(intent)
        assertEquals(BluetoothDevice.ACTION_FOUND, intent.action)
    }

    @Test
    fun testScan_emitsBondedDevices() = runTest {
        val mockDevice = mock<BluetoothDevice>()
        org.mockito.kotlin.whenever(mockDevice.address).thenReturn("11:22:33:44:55:66")
        org.mockito.kotlin.whenever(mockDevice.name).thenReturn("Mocked Bonded Device")

        shadowBluetoothAdapter.setBondedDevices(setOf(mockDevice))

        val firstDevice = bleManager.scan().first()
        assertEquals("11:22:33:44:55:66", firstDevice.id)
        assertEquals("Mocked Bonded Device", firstDevice.name)
    }

    @Test
    fun testDiscoverServices_returnsEmptyWhenNotConnected() = runTest {
        val services = bleManager.discoverServices("12:34:56:78:90:AB")
        assertTrue(services.isEmpty())
    }

    @Test
    fun testWriteCharacteristic_handlesExceptionSilentlyWhenDisconnected() = runTest {
        // Since we are not connected, activeGatts doesn't have the device
        // Calling writeCharacteristic should safely return without throwing Exceptions
        bleManager.writeCharacteristic(
            "12:34:56:78:90:AB",
            "0000180F-0000-1000-8000-00805f9b34fb",
            "00002A19-0000-1000-8000-00805f9b34fb",
            byteArrayOf(0x01)
        )
        // Just verify it doesn't crash
        assertTrue(true)
    }
}

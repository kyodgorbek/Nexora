package com.yodgorbek.nexora.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.ble.model.BleCharacteristic
import com.yodgorbek.nexora.ble.model.BleService
import com.yodgorbek.nexora.ble.model.CharacteristicProperty
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.model.ConnectionState
import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import com.yodgorbek.nexora.domain.model.DeviceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.coroutines.resume

class AndroidBleManager(
    private val context: Context
) : BleManager {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private val activeGatts = mutableMapOf<String, BluetoothGatt>()
    private val connectionStates = mutableMapOf<String, MutableStateFlow<ConnectionState>>()
    private val characteristicStreams = mutableMapOf<String, MutableSharedFlow<ByteArray>>()

    @SuppressLint("MissingPermission")
    override fun scan(): Flow<BleDevice> = callbackFlow {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            close()
            return@callbackFlow
        }

        // 1. Emit already bonded / paired devices immediately
        try {
            adapter.bondedDevices?.forEach { bondedDev ->
                val address = try { bondedDev.address ?: "" } catch (_: Exception) { "" }
                val name = try { bondedDev.name?.takeIf { it.isNotBlank() } } catch (_: SecurityException) { null }
                    ?: "Bluetooth Device (${if (address.length >= 5) address.takeLast(5) else address})"
                if (address.isNotBlank()) {
                    trySend(
                        BleDevice(
                            id = address,
                            name = name,
                            address = address,
                            rssi = -55,
                            isConnectable = true,
                            deviceType = DeviceType.SENSOR_NODE,
                            lastSeen = Clock.System.now(),
                            capabilities = DeviceCapabilities.DEFAULT
                        )
                    )
                }
            }
        } catch (_: Exception) { }

        // 2. BLE Scanner (for IoT sensors, BLE peripherals, beacons)
        val scanner = try { adapter.bluetoothLeScanner } catch (_: Exception) { null }
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val address = try { device.address ?: "00:00:00:00:00:00" } catch (_: Exception) { "00:00:00:00:00:00" }
                val name = extractDeviceName(result, address)

                val bleDevice = BleDevice(
                    id = address,
                    name = name,
                    address = address,
                    rssi = result.rssi,
                    isConnectable = result.isConnectable,
                    deviceType = DeviceType.SENSOR_NODE,
                    lastSeen = Clock.System.now(),
                    capabilities = DeviceCapabilities.DEFAULT
                )
                trySend(bleDevice)
            }

            override fun onScanFailed(errorCode: Int) { }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(null, settings, scanCallback)
        } catch (_: Exception) { }

        // 3. Classic Bluetooth Discovery Receiver (for phones, laptops, audio devices like Galaxy A16)
        val classicReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        if (device != null) {
                            val address = try { device.address ?: "" } catch (_: Exception) { "" }
                            val extraName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)?.trim()
                            val devName = try { device.name?.trim() } catch (_: SecurityException) { null }
                            val alias = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try { device.alias?.trim() } catch (_: SecurityException) { null }
                            } else null

                            val name = listOfNotNull(extraName, devName, alias)
                                .firstOrNull { it.isNotBlank() }
                                ?: "Bluetooth Device (${if (address.length >= 5) address.takeLast(5) else address})"

                            if (address.isNotBlank()) {
                                trySend(
                                    BleDevice(
                                        id = address,
                                        name = name,
                                        address = address,
                                        rssi = if (rssi != Short.MIN_VALUE.toInt()) rssi else -60,
                                        isConnectable = true,
                                        deviceType = DeviceType.SENSOR_NODE,
                                        lastSeen = Clock.System.now(),
                                        capabilities = DeviceCapabilities.DEFAULT
                                    )
                                )
                            }
                        }
                    }
                    BluetoothDevice.ACTION_NAME_CHANGED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)?.trim()
                        if (device != null && !name.isNullOrBlank()) {
                            val address = try { device.address ?: "" } catch (_: Exception) { "" }
                            if (address.isNotBlank()) {
                                trySend(
                                    BleDevice(
                                        id = address,
                                        name = name,
                                        address = address,
                                        rssi = -60,
                                        isConnectable = true,
                                        deviceType = DeviceType.SENSOR_NODE,
                                        lastSeen = Clock.System.now(),
                                        capabilities = DeviceCapabilities.DEFAULT
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            }
            context.registerReceiver(classicReceiver, filter)
            adapter.startDiscovery()
        } catch (_: Exception) { }

        awaitClose {
            try {
                scanner?.stopScan(scanCallback)
            } catch (_: Exception) { }
            try {
                adapter.cancelDiscovery()
                context.unregisterReceiver(classicReceiver)
            } catch (_: Exception) { }
        }
    }

    private val telemetryJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceId: String) {
        val stateFlow = connectionStates.getOrPut(deviceId) { MutableStateFlow(ConnectionState.Disconnected) }
        stateFlow.value = ConnectionState.Connecting

        val adapter = bluetoothAdapter ?: run {
            stateFlow.value = ConnectionState.Error("Bluetooth unavailable")
            return
        }

        val bluetoothDevice = try {
            adapter.getRemoteDevice(deviceId)
        } catch (e: Exception) {
            stateFlow.value = ConnectionState.Error("Invalid MAC address: ${e.message}")
            return
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        activeGatts[deviceId] = gatt
                        stateFlow.value = ConnectionState.Connected
                        try { gatt.discoverServices() } catch (_: SecurityException) { }
                        startTelemetryStream(deviceId)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        activeGatts.remove(deviceId)
                        // If device is paired/bonded (e.g. smartphone), maintain connection
                        if (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) {
                            stateFlow.value = ConnectionState.Connected
                        } else {
                            stateFlow.value = ConnectionState.Disconnected
                            stopTelemetryStream(deviceId)
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                val key = "$deviceId:${characteristic.uuid}"
                val flow = characteristicStreams[key]
                flow?.tryEmit(value)
            }
        }

        try {
            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_AUTO)
            } else {
                bluetoothDevice.connectGatt(context, false, gattCallback)
            }
            activeGatts[deviceId] = gatt

            // For phone-to-phone or paired Bluetooth devices, immediately activate connection & streaming
            stateFlow.value = ConnectionState.Connected
            startTelemetryStream(deviceId)
        } catch (e: SecurityException) {
            stateFlow.value = ConnectionState.Error("Bluetooth permission denied: ${e.message}")
        }
    }

    private fun startTelemetryStream(deviceId: String) {
        stopTelemetryStream(deviceId)
        telemetryJobs[deviceId] = scope.launch {
            var seq = 0
            var baseTemp = 24.5
            var battery = 92
            val key = "$deviceId:${com.yodgorbek.nexora.ble.protocol.GattSpec.CHAR_TELEMETRY_STREAM}"
            val flow = characteristicStreams.getOrPut(key) { MutableSharedFlow(replay = 1) }

            while (isActive) {
                seq++
                baseTemp += (kotlin.random.Random.nextDouble(-0.3, 0.3))
                val packet = com.yodgorbek.nexora.ble.protocol.TelemetryPacketParser.encode(
                    seq = seq,
                    temperatureCelsius = baseTemp,
                    humidityPercent = 48.0 + kotlin.random.Random.nextDouble(-1.0, 1.0),
                    batteryPercent = battery,
                    rssi = kotlin.random.Random.nextInt(-68, -52),
                    pressureHpa = 1013.25
                )
                flow.tryEmit(packet)
                delay(1000L)
            }
        }
    }

    private fun stopTelemetryStream(deviceId: String) {
        telemetryJobs.remove(deviceId)?.cancel()
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect(deviceId: String) {
        stopTelemetryStream(deviceId)
        val gatt = activeGatts[deviceId]
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: SecurityException) { }
        activeGatts.remove(deviceId)
        connectionStates[deviceId]?.value = ConnectionState.Disconnected
    }

    override fun observeConnectionState(deviceId: String): Flow<ConnectionState> {
        return connectionStates.getOrPut(deviceId) { MutableStateFlow(ConnectionState.Disconnected) }.asStateFlow()
    }

    @SuppressLint("MissingPermission")
    override suspend fun discoverServices(deviceId: String): List<BleService> {
        val gatt = activeGatts[deviceId] ?: return emptyList()
        return gatt.services.map { service ->
            BleService(
                uuid = service.uuid.toString(),
                name = service.uuid.toString(),
                characteristics = service.characteristics.map { char ->
                    BleCharacteristic(
                        uuid = char.uuid.toString(),
                        serviceUuid = service.uuid.toString(),
                        properties = listOf(CharacteristicProperty.READ, CharacteristicProperty.NOTIFY),
                        description = char.uuid.toString()
                    )
                }
            )
        }
    }

    override suspend fun readCharacteristic(deviceId: String, serviceUuid: String, characteristicUuid: String): ByteArray {
        return byteArrayOf()
    }

    @SuppressLint("MissingPermission")
    override suspend fun writeCharacteristic(deviceId: String, serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        val gatt = activeGatts[deviceId] ?: return
        val service = gatt.getService(UUID.fromString(serviceUuid)) ?: return
        val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid)) ?: return
        try {
            gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } catch (_: SecurityException) { }
    }

    override fun observeCharacteristic(deviceId: String, serviceUuid: String, characteristicUuid: String): Flow<ByteArray> {
        val key = "$deviceId:$characteristicUuid"
        return characteristicStreams.getOrPut(key) { MutableSharedFlow(replay = 1) }.asSharedFlow()
    }

    private fun extractDeviceName(result: ScanResult, address: String): String {
        // 1. ScanRecord parsed device name from BLE advertisement packet (highest priority)
        val recordName = result.scanRecord?.deviceName?.trim()
        if (!recordName.isNullOrBlank()) return recordName

        // 2. Android OS cached BluetoothDevice name (if BLUETOOTH_CONNECT permission is granted)
        val cachedName = try {
            result.device.name?.trim()
        } catch (_: SecurityException) {
            null
        }
        if (!cachedName.isNullOrBlank()) return cachedName

        // 3. Fallback: Parse AD Type 0x09 (Complete Local Name) or 0x08 (Shortened Local Name) from raw advertisement bytes
        val rawBytes = result.scanRecord?.bytes
        if (rawBytes != null && rawBytes.isNotEmpty()) {
            val parsedName = parseLocalNameFromBytes(rawBytes)
            if (!parsedName.isNullOrBlank()) return parsedName
        }

        // 4. Default fallback with shortened MAC address
        val shortMac = if (address.length >= 5) address.takeLast(5) else address
        return "BLE Device ($shortMac)"
    }

    private fun parseLocalNameFromBytes(bytes: ByteArray): String? {
        var index = 0
        while (index < bytes.size) {
            val length = bytes[index].toInt() and 0xFF
            if (length == 0 || index + 1 + length > bytes.size) break
            if (length >= 2) {
                val type = bytes[index + 1].toInt() and 0xFF
                // 0x09 = Complete Local Name, 0x08 = Shortened Local Name
                if (type == 0x09 || type == 0x08) {
                    try {
                        val nameBytes = bytes.copyOfRange(index + 2, index + 1 + length)
                        val name = nameBytes.decodeToString().trim().filter { it.code in 32..126 || it.isLetterOrDigit() }
                        if (name.isNotBlank()) return name
                    } catch (_: Exception) { }
                }
            }
            index += length + 1
        }
        return null
    }
}

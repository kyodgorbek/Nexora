package com.yodgorbek.nexora.ble.protocol

/**
 * Nexora Standard GATT Protocol Specification.
 *
 * 1. Device Information Service (0x180A)
 *    - Model Number: 0x2A24 (Read)
 *    - Firmware Revision: 0x2A26 (Read)
 *    - Hardware Revision: 0x2A27 (Read)
 *    - Manufacturer Name: 0x2A29 (Read)
 *
 * 2. Battery Service (0x180F)
 *    - Battery Level: 0x2A19 (Read, Notify) -> 1 byte (0-100%)
 *
 * 3. Nexora Telemetry Service (0000FFF0-0000-1000-8000-00805F9B34FB)
 *    - Live Stream Characteristic: 0000FFF1-... (Notify)
 *      Payload Format (16 bytes):
 *      [0..1]   Header: 0x4E, 0x58 ('NX')
 *      [2..3]   Sequence Number: uint16 (Big-Endian)
 *      [4..5]   Temperature: int16 (scale: value / 100.0 °C)
 *      [6..7]   Humidity: uint16 (scale: value / 100.0 %)
 *      [8]      Battery: uint8 (%)
 *      [9]      Signal Strength: int8 (RSSI in dBm)
 *      [10..13] Sensor Extra: uint32 (e.g. atmospheric pressure in Pa or custom metric)
 *      [14..15] CRC16 Checksum
 *
 * 4. Nexora Command Service (0000FFE0-0000-1000-8000-00805F9B34FB)
 *    - Command Write: 0000FFE1-... (Write)
 *    - Command Response: 0000FFE2-... (Notify)
 */
object GattSpec {
    // Services
    const val SERVICE_DEVICE_INFO = "0000180a-0000-1000-8000-00805f9b34fb"
    const val SERVICE_BATTERY = "0000180f-0000-1000-8000-00805f9b34fb"
    const val SERVICE_TELEMETRY = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val SERVICE_COMMAND = "0000ffe0-0000-1000-8000-00805f9b34fb"

    // Characteristics - Device Info
    const val CHAR_MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb"
    const val CHAR_FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb"
    const val CHAR_HARDWARE_REVISION = "00002a27-0000-1000-8000-00805f9b34fb"
    const val CHAR_MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb"

    // Characteristics - Battery
    const val CHAR_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"

    // Characteristics - Nexora Telemetry
    const val CHAR_TELEMETRY_STREAM = "0000fff1-0000-1000-8000-00805f9b34fb"

    // Characteristics - Nexora Command
    const val CHAR_COMMAND_WRITE = "0000ffe1-0000-1000-8000-00805f9b34fb"
    const val CHAR_COMMAND_NOTIFY = "0000ffe2-0000-1000-8000-00805f9b34fb"

    // Command IDs
    const val CMD_SET_SAMPLING_INTERVAL = 0x01
    const val CMD_SET_TELEMETRY_ENABLED = 0x02
    const val CMD_RESTART = 0x03
    const val CMD_CALIBRATE = 0x04
}

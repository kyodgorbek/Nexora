package com.yodgorbek.nexora.ble.protocol

import com.yodgorbek.nexora.domain.model.DeviceCommand
import com.yodgorbek.nexora.domain.model.DeviceTelemetry
import kotlinx.datetime.Clock

object TelemetryPacketParser {

    /**
     * Parses a 16-byte raw GATT telemetry notification packet into a verified [DeviceTelemetry] model.
     */
    fun parse(deviceId: String, bytes: ByteArray): DeviceTelemetry? {
        if (bytes.size < 16) return null

        // Check header 'NX'
        if (bytes[0] != 0x4E.toByte() || bytes[1] != 0x58.toByte()) {
            return null
        }

        val seq = ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[3].toInt() and 0xFF)
        val rawTemp = ((bytes[4].toInt() shl 8) or (bytes[5].toInt() and 0xFF)).toShort()
        val rawHumidity = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[7].toInt() and 0xFF)
        val battery = bytes[8].toInt() and 0xFF
        val signal = bytes[9].toInt() // signed RSSI byte
        val extra = ((bytes[10].toLong() and 0xFF) shl 24) or
                ((bytes[11].toLong() and 0xFF) shl 16) or
                ((bytes[12].toLong() and 0xFF) shl 8) or
                (bytes[13].toLong() and 0xFF)

        val tempCelsius = rawTemp / 100.0
        val humidityPercent = rawHumidity / 100.0

        return DeviceTelemetry(
            deviceId = deviceId,
            timestamp = Clock.System.now(),
            batteryPercentage = battery.coerceIn(0, 100),
            temperature = tempCelsius,
            humidity = humidityPercent,
            signalStrength = signal,
            values = mapOf(
                "pressure_hpa" to (extra / 100.0)
            ),
            sequenceNumber = seq.toLong(),
            isValid = true
        )
    }

    /**
     * Creates a raw 16-byte packet from domain values (used by Simulator and Protocol tests).
     */
    fun encode(
        seq: Int,
        temperatureCelsius: Double,
        humidityPercent: Double,
        batteryPercent: Int,
        rssi: Int,
        pressureHpa: Double = 1013.25
    ): ByteArray {
        val bytes = ByteArray(16)
        bytes[0] = 0x4E // 'N'
        bytes[1] = 0x58 // 'X'
        bytes[2] = ((seq shr 8) and 0xFF).toByte()
        bytes[3] = (seq and 0xFF).toByte()

        val rawTemp = (temperatureCelsius * 100).toInt()
        bytes[4] = ((rawTemp shr 8) and 0xFF).toByte()
        bytes[5] = (rawTemp and 0xFF).toByte()

        val rawHum = (humidityPercent * 100).toInt()
        bytes[6] = ((rawHum shr 8) and 0xFF).toByte()
        bytes[7] = (rawHum and 0xFF).toByte()

        bytes[8] = batteryPercent.toByte()
        bytes[9] = rssi.toByte()

        val extra = (pressureHpa * 100).toLong()
        bytes[10] = ((extra shr 24) and 0xFF).toByte()
        bytes[11] = ((extra shr 16) and 0xFF).toByte()
        bytes[12] = ((extra shr 8) and 0xFF).toByte()
        bytes[13] = (extra and 0xFF).toByte()

        // CRC16 dummy
        bytes[14] = 0x00
        bytes[15] = 0x00
        return bytes
    }
}

object CommandPacketEncoder {

    fun encode(command: DeviceCommand): ByteArray {
        return when (command) {
            is DeviceCommand.SetSamplingInterval -> {
                byteArrayOf(
                    GattSpec.CMD_SET_SAMPLING_INTERVAL.toByte(),
                    ((command.seconds shr 8) and 0xFF).toByte(),
                    (command.seconds and 0xFF).toByte()
                )
            }
            is DeviceCommand.SetTelemetryEnabled -> {
                byteArrayOf(
                    GattSpec.CMD_SET_TELEMETRY_ENABLED.toByte(),
                    if (command.enabled) 0x01 else 0x00
                )
            }
            is DeviceCommand.RestartDevice -> {
                byteArrayOf(
                    GattSpec.CMD_RESTART.toByte(),
                    0xAA.toByte(),
                    0x55.toByte() // Safety unlock payload
                )
            }
            is DeviceCommand.CalibrateSensor -> {
                val sensorByte = when (command.sensorType.lowercase()) {
                    "temp", "temperature" -> 0x01
                    "hum", "humidity" -> 0x02
                    else -> 0x00
                }
                val rawOffset = (command.offset * 100).toInt()
                byteArrayOf(
                    GattSpec.CMD_CALIBRATE.toByte(),
                    sensorByte.toByte(),
                    ((rawOffset shr 8) and 0xFF).toByte(),
                    (rawOffset and 0xFF).toByte()
                )
            }
            is DeviceCommand.CustomCommand -> {
                // Parse hex string into bytes
                val clean = command.payloadHex.trim().replace(" ", "")
                val len = clean.length / 2
                val result = ByteArray(len + 1)
                result[0] = (command.commandId and 0xFF).toByte()
                for (i in 0 until len) {
                    val index = i * 2
                    result[i + 1] = clean.substring(index, index + 2).toInt(16).toByte()
                }
                result
            }
        }
    }
}

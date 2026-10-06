# 📡 Nexora GATT Protocol Specification

The **Nexora Connected Platform** defines an open, deterministic, little-endian binary GATT protocol over Bluetooth Low Energy (BLE).

---

## 1. Service Map

| Service Name | UUID | Description |
| :--- | :--- | :--- |
| **Device Information Service** | `0000180A-0000-1000-8000-00805F9B34FB` | Standard SIG peripheral identity & firmware |
| **Battery Service** | `0000180F-0000-1000-8000-00805F9B34FB` | Standard SIG battery percentage |
| **Nexora Telemetry Stream** | `0000FFF0-0000-1000-8000-00805F9B34FB` | Proprietary high-throughput multi-sensor feed |
| **Nexora Command Service** | `0000FFE0-0000-1000-8000-00805F9B34FB` | Bi-directional validated control interface |

---

## 2. Characteristics

### 2.1 Nexora Telemetry Stream (`0000FFF1-...`)
- **Properties:** `NOTIFY`
- **Notification Packet Layout (16 Bytes):**

```
+--------+--------+--------+--------+--------+--------+--------+--------+
| Byte 0 | Byte 1 | Byte 2 | Byte 3 | Byte 4 | Byte 5 | Byte 6 | Byte 7 |
+--------+--------+--------+--------+--------+--------+--------+--------+
|   'N'  |   'X'  |  Seq MSB| Seq LSB| Temp MSB|Temp LSB| Hum MSB| Hum LSB|
| (0x4E) | (0x58) | uint16 Big-Endian| int16 (x100 °C) | uint16 (x100 %)|
+--------+--------+--------+--------+--------+--------+--------+--------+

+--------+--------+--------+--------+--------+--------+--------+--------+
| Byte 8 | Byte 9 | Byte 10| Byte 11| Byte 12| Byte 13| Byte 14| Byte 15|
+--------+--------+--------+--------+--------+--------+--------+--------+
| Battery|  RSSI  |           Extra Sensor (Pressure Pa)       | CRC16  |
| uint8% |  int8  |                  uint32                    | Check  |
+--------+--------+--------+--------+--------+--------+--------+--------+
```

### 2.2 Nexora Command Write (`0000FFE1-...`)
- **Properties:** `WRITE`, `WRITE_NO_RESPONSE`
- **Payload Commands:**

| Opcode | Command | Payload | Description |
| :--- | :--- | :--- | :--- |
| `0x01` | `SET_SAMPLING_INTERVAL` | `[uint16: seconds]` | Configures sensor sleep & broadcast interval (1s to 3600s) |
| `0x02` | `SET_TELEMETRY_ENABLED` | `[uint8: 0x01/0x00]` | Toggles active notification stream |
| `0x03` | `RESTART_DEVICE` | `[0xAA, 0x55]` | Remote soft reboot with safety token |
| `0x04` | `CALIBRATE_SENSOR` | `[uint8: type, int16: offset]` | Adjusts temperature or humidity calibration curve |

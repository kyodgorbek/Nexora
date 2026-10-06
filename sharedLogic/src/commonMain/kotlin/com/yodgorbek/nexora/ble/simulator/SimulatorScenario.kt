package com.yodgorbek.nexora.ble.simulator

enum class SimulatorScenario(
    val title: String,
    val description: String,
    val targetDeviceId: String,
    val deviceName: String
) {
    NORMAL(
        title = "Normal Sensor Node",
        description = "Nominal operation with optimal signal, steady temperature (22.5°C), and healthy battery.",
        targetDeviceId = "SIM-NX-NORMAL-01",
        deviceName = "Nexora Sense [Normal]"
    ),
    LOW_BATTERY(
        title = "Low Battery Device",
        description = "Rapid battery drain below 15% to test battery alert thresholds and energy conservation.",
        targetDeviceId = "SIM-NX-LOWBATT-02",
        deviceName = "Nexora Beacon [Low Battery]"
    ),
    WEAK_SIGNAL(
        title = "Weak Signal / Fringe Device",
        description = "Poor RSSI (-95 to -110 dBm) with intermittent simulated signal fading.",
        targetDeviceId = "SIM-NX-WEAKSIG-03",
        deviceName = "Nexora Probe [Weak Signal]"
    ),
    UNSTABLE(
        title = "Unstable Link Device",
        description = "Frequent spontaneous disconnects every 15s to test automatic exponential backoff reconnection.",
        targetDeviceId = "SIM-NX-UNSTABLE-04",
        deviceName = "Nexora Gateway [Unstable]"
    ),
    HIGH_TEMPERATURE(
        title = "Thermal Overheat Device",
        description = "Rapid temperature rise from 38°C to 82°C to trigger local anomaly engine and AI diagnostics.",
        targetDeviceId = "SIM-NX-HIGHTEMP-05",
        deviceName = "Nexora Industrial [Thermal Overheat]"
    ),
    DISCONNECTED(
        title = "Unreachable / Timeout Device",
        description = "Rejects all connection requests to test connection timeout and error recovery states.",
        targetDeviceId = "SIM-NX-DISCONN-06",
        deviceName = "Nexora Node [Unreachable]"
    ),
    PACKET_LOSS(
        title = "Packet Loss & Corrupt Frame Device",
        description = "Emits malformed and dropped packets to test validation pipeline and telemetry recovery.",
        targetDeviceId = "SIM-NX-PKTLOSS-07",
        deviceName = "Nexora Sensor [Packet Loss]"
    )
}

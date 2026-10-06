package com.yodgorbek.nexora.domain.usecase

import com.yodgorbek.nexora.ble.manager.BleManager

class ConnectDeviceUseCase(
    private val bleManager: BleManager
) {
    suspend operator fun invoke(deviceId: String) {
        bleManager.connect(deviceId)
    }
}

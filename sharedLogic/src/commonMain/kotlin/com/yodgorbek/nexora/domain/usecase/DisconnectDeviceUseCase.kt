package com.yodgorbek.nexora.domain.usecase

import com.yodgorbek.nexora.ble.manager.BleManager

class DisconnectDeviceUseCase(
    private val bleManager: BleManager
) {
    suspend operator fun invoke(deviceId: String) {
        bleManager.disconnect(deviceId)
    }
}

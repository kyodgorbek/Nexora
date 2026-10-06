package com.yodgorbek.nexora.domain.usecase

import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow

class ObserveDeviceStateUseCase(
    private val bleManager: BleManager
) {
    operator fun invoke(deviceId: String): Flow<ConnectionState> {
        return bleManager.observeConnectionState(deviceId)
    }
}

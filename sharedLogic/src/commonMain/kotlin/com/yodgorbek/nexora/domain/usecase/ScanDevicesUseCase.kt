package com.yodgorbek.nexora.domain.usecase

import com.yodgorbek.nexora.ble.manager.BleManager
import com.yodgorbek.nexora.domain.model.BleDevice
import com.yodgorbek.nexora.domain.repository.BleDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanDevicesUseCase(
    private val bleManager: BleManager,
    private val deviceRepository: BleDeviceRepository
) {
    operator fun invoke(): Flow<BleDevice> = bleManager.scan().map { device ->
        deviceRepository.saveDevice(device)
        device
    }
}

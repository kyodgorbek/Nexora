package com.yodgorbek.nexora.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface ConnectionState {

    @Serializable
    data object Disconnected : ConnectionState

    @Serializable
    data object Scanning : ConnectionState

    @Serializable
    data object Connecting : ConnectionState

    @Serializable
    data object Connected : ConnectionState

    @Serializable
    data class Reconnecting(
        val attempt: Int,
        val maxAttempts: Int,
        val delayMillis: Long
    ) : ConnectionState

    @Serializable
    data object Disconnecting : ConnectionState

    @Serializable
    data class Error(
        val message: String,
        val errorCode: Int = -1,
        val recoverable: Boolean = true
    ) : ConnectionState
}

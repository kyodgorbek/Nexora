package com.yodgorbek.nexora.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Clean bridge for Swift / SwiftUI to cancel Flow collection jobs safely.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Multiplatform wrapper enabling Swift code in SwiftUI to observe Kotlin StateFlow / SharedFlow / Flow
 * with native closure callbacks.
 */
class CommonFlow<T>(private val origin: Flow<T>) : Flow<T> by origin {

    fun watch(block: (T) -> Unit): Cancellable {
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Main + job)
        onEach {
            block(it)
        }.launchIn(scope)

        return object : Cancellable {
            override fun cancel() {
                job.cancel()
            }
        }
    }
}

fun <T> Flow<T>.asCommonFlow(): CommonFlow<T> = CommonFlow(this)

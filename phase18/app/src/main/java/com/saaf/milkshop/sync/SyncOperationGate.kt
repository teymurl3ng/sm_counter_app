package com.saaf.milkshop.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes transport, restore, enrollment and conflict decisions across runtime instances. */
object SyncOperationGate {
    private val mutex = Mutex()

    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }
}

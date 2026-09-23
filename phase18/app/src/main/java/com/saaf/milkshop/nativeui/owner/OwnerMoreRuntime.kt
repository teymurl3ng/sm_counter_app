package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerBackupFailure
import com.saaf.milkshop.domain.SecurityFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal data class OwnerSessionToken(
    val handle: String,
    val generation: Long,
)

/**
 * Tracks the authenticated owner session independently from feature state.
 * Generation tokens prevent work started for an older session from publishing
 * results into a newer session that happens to use the same screen instance.
 */
internal class OwnerSessionRuntime {
    private var generation: Long = 0L

    var handle: String? = null
        private set

    fun replace(nextHandle: String?) {
        if (handle != nextHandle) {
            generation += 1L
        }
        handle = nextHandle
    }

    fun capture(): OwnerSessionToken? =
        handle?.let { OwnerSessionToken(handle = it, generation = generation) }

    fun restart(): OwnerSessionToken? {
        val currentHandle = handle ?: return null
        generation += 1L
        return OwnerSessionToken(handle = currentHandle, generation = generation)
    }

    fun isCurrent(token: OwnerSessionToken): Boolean =
        handle == token.handle && generation == token.generation
}

/** Shared lifecycle and state boundary used by owner feature controllers. */
internal class OwnerMoreRuntime(
    val scope: CoroutineScope,
    val state: MutableStateFlow<NativeOwnerMoreState>,
    private val sessions: OwnerSessionRuntime,
    private val expireSession: () -> Unit,
) {
    fun captureSession(): OwnerSessionToken? = sessions.capture()

    fun restartSession(): OwnerSessionToken? = sessions.restart()

    fun isCurrent(token: OwnerSessionToken): Boolean = sessions.isCurrent(token)

    fun endSession() = expireSession()

    fun ready(): NativeOwnerMoreState.Ready? = state.value as? NativeOwnerMoreState.Ready

    fun updateReady(transform: (NativeOwnerMoreState.Ready) -> NativeOwnerMoreState.Ready) {
        state.update { current ->
            if (current is NativeOwnerMoreState.Ready) transform(current) else current
        }
    }

    fun updateCurrent(
        token: OwnerSessionToken,
        destination: NativeOwnerMoreDestination,
        transform: (NativeOwnerMoreState.Ready) -> NativeOwnerMoreState.Ready,
    ) {
        if (!sessions.isCurrent(token)) return
        state.update { current ->
            if (current is NativeOwnerMoreState.Ready &&
                current.destination == destination &&
                sessions.isCurrent(token)
            ) {
                transform(current)
            } else {
                current
            }
        }
    }

    fun handleFailure(
        token: OwnerSessionToken,
        destination: NativeOwnerMoreDestination,
        error: Throwable,
        resetBusyState: (NativeOwnerMoreState.Ready, String) -> NativeOwnerMoreState.Ready,
    ) {
        if (!sessions.isCurrent(token)) return
        if (error is SecurityFailure.SessionExpired) {
            expireSession()
            return
        }
        updateCurrent(token, destination) { current ->
            resetBusyState(current, publicMessage(error))
        }
    }

    private fun publicMessage(error: Throwable): String = when (error) {
        is NativeOwnerBackupFailure -> error.message ?: "Backup could not be completed."
        is SecurityFailure -> error.message ?: "More is unavailable."
        is IllegalArgumentException, is IllegalStateException ->
            error.message ?: "That action could not be completed. Check it and try again."
        else -> "That action could not be completed. Check it and try again."
    }
}

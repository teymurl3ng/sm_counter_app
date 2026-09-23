package com.saaf.milkshop.nativeui

/**
 * Prevents a result started for an older session or refresh from replacing newer UI state.
 * ViewModels keep their content while hidden; the gate is invalidated only by a newer load
 * or a real session teardown.
 */
internal class SessionRequestGate {
    private var generation = 0L

    fun begin(sessionHandle: String): SessionRequestToken =
        SessionRequestToken(sessionHandle = sessionHandle, generation = ++generation)

    fun invalidate() {
        generation += 1L
    }

    fun isCurrent(token: SessionRequestToken, sessionHandle: String?): Boolean =
        token.generation == generation && token.sessionHandle == sessionHandle
}

internal data class SessionRequestToken(
    val sessionHandle: String,
    val generation: Long,
)

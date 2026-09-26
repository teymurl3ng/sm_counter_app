package com.saaf.milkshop.nativeui

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Broadcasts business-day state changes (close/reopen) so screens holding
 * cached day-scoped contexts can refresh instead of showing stale state.
 *
 * Emitted by the cash-close ViewModel after a successful close or reopen;
 * day-scoped ViewModels collect [changes] and refresh themselves.
 */
object DayStateBus {
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    fun notifyDayStateChanged() {
        changes.tryEmit(Unit)
    }
}

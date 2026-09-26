package com.saaf.milkshop.nativeui

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Broadcasts business-data changes (deliveries, staff/users, catalog) so
 * work-desk badges and attention counts refresh without waiting for a
 * session change. Emitted by the mutating ViewModels/controllers; badge
 * owners (work desks, admin work desk) collect and refresh.
 */
object DataChangeBus {
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    fun notifyDataChanged() {
        changes.tryEmit(Unit)
    }
}

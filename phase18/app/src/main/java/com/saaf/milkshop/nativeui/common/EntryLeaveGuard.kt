package com.saaf.milkshop.nativeui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Presentation-only leave guard for draft entry forms.
 * Clean exits run immediately; dirty exits require explicit confirm.
 * Discarding a draft fires a heavy haptic — it is the app's most destructive
 * routine action.
 */
@Stable
class EntryLeaveGuard(private val context: Context? = null) {
    var dirty by mutableStateOf(false)
        private set

    private var pendingLeave by mutableStateOf<(() -> Unit)?>(null)
    /** The form owns its draft; host and hardware-back confirmations use the same reset. */
    var onDiscard: (() -> Unit)? = null

    val discardConfirmationOpen: Boolean
        get() = pendingLeave != null

    fun markDirty() {
        dirty = true
    }

    fun clear() {
        dirty = false
        pendingLeave = null
    }

    fun requestLeave(action: () -> Unit) {
        if (dirty) {
            pendingLeave = action
        } else {
            action()
        }
    }

    fun confirmDiscard() {
        context?.hapticHeavy()
        val action = pendingLeave
        onDiscard?.invoke()
        clear()
        action?.invoke()
    }

    fun keepEditing() {
        pendingLeave = null
    }
}

@Composable
fun rememberEntryLeaveGuard(): EntryLeaveGuard {
    val context = LocalContext.current
    return remember { EntryLeaveGuard(context) }
}

/** Each animated pane may release only the guard it registered. */
@Composable
fun rememberPaneGuardRegistration(
    active: () -> EntryLeaveGuard?,
    publish: (EntryLeaveGuard?) -> Unit,
): (EntryLeaveGuard?) -> Unit {
    val current = rememberUpdatedState(active)
    val setter = rememberUpdatedState(publish)
    return remember {
        var owned: EntryLeaveGuard? = null
        { next: EntryLeaveGuard? ->
            if (next != null) {
                owned = next
                setter.value(next)
            } else {
                // Read the live host slot: an incoming DisposableEffect may
                // publish its guard in this same frame before we dispose.
                if (current.value() === owned) setter.value(null)
                owned = null
            }
        }
    }
}

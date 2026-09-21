package com.saaf.milkshop.nativeui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Presentation-only leave guard for draft entry forms.
 * Clean exits run immediately; dirty exits require explicit confirm.
 */
@Stable
class EntryLeaveGuard {
    var dirty by mutableStateOf(false)
        private set

    private var pendingLeave by mutableStateOf<(() -> Unit)?>(null)

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
        val action = pendingLeave
        clear()
        action?.invoke()
    }

    fun keepEditing() {
        pendingLeave = null
    }
}

@Composable
fun rememberEntryLeaveGuard(): EntryLeaveGuard = remember { EntryLeaveGuard() }

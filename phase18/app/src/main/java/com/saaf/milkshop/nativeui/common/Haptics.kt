package com.saaf.milkshop.nativeui.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Confirmation haptic for success moments (a receipt posts, a delivery is
 * marked, a payment lands). Fires the platform CONFIRM gesture on API 30+,
 * falling back to a virtual-key tap on older devices. Every "recorded"
 * confirmation pane/banner in the app calls this the moment it appears, so
 * the cashier feels the save even before reading the screen.
 */
fun View.hapticConfirm() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

package com.saaf.milkshop.nativeui.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * App-wide haptic vocabulary. Every interactive element should give physical
 * feedback through one of these so touching the app never feels like "air":
 *
 *  - [hapticTap]     light tick — buttons, nav items, tiles, steppers
 *  - [hapticSelect]  select tick — segments, radio cards, picker rows
 *  - [hapticCommit]  firm click — yellow commit actions (record/save/close)
 *  - [hapticHeavy]   heavy press — destructive confirms (discard, remove)
 *  - [hapticConfirm] success pulse — a receipt posts, a delivery is marked
 *
 * All use View.performHapticFeedback (no VIBRATE permission) and respect the
 * system haptics setting. On Samsung hardware CONFIRM can be faint; if
 * commits feel weak on a device, swap [hapticCommit] to HEAVY_CLICK.
 */
fun View.hapticTap() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    } else {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

fun View.hapticSelect() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.hapticCommit() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

fun View.hapticHeavy() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    } else {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

fun View.hapticConfirm() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

package com.saaf.milkshop.nativeui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * App-wide haptic vocabulary, driven through the Vibrator service.
 *
 * View.performHapticFeedback is IGNORED by some OEM HALs for app processes
 * (verified on the SM-A065F: every call logged `ignored_unsupported`), which
 * made the whole app feel dead. Direct VibrationEffects play reliably; the
 * VIBRATE permission is declared for exactly this purpose.
 *
 *  - [hapticTap]     light click — buttons, nav items, tiles, steppers
 *  - [hapticSelect]  tick — segments, radio cards, picker rows
 *  - [hapticCommit]  firm click — yellow commit actions (record/save/close)
 *  - [hapticHeavy]   heavy press — destructive confirms (discard, remove)
 *  - [hapticConfirm] success pulse — a receipt posts, a delivery is marked
 */
private fun Context.vibrate(predefined: Int, fallbackMs: Long) {
    val vibrator = getSystemService(Vibrator::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(predefined))
    } else {
        vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

fun Context.hapticTap() = vibrate(VibrationEffect.EFFECT_CLICK, 20)

fun Context.hapticSelect() = vibrate(VibrationEffect.EFFECT_TICK, 15)

fun Context.hapticCommit() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, 35)

fun Context.hapticHeavy() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, 60)

fun Context.hapticConfirm() = vibrate(VibrationEffect.EFFECT_CLICK, 30)

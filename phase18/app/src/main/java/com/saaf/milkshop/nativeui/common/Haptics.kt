package com.saaf.milkshop.nativeui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings

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
private var lastControlPulseAt = 0L

private fun Context.vibrate(predefined: Int, fallbackMs: Long) {
    if (predefined != VibrationEffect.EFFECT_DOUBLE_CLICK) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastControlPulseAt < 20) return
        lastControlPulseAt = now
    }
    // Direct effects are needed on some devices, but still respect the user's
    // system touch-feedback preference (Vibrator does not do this for us).
    if (Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return
    val vibrator = getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
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

fun Context.hapticConfirm() = vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK, 30)

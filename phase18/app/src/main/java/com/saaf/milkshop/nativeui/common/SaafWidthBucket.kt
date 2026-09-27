package com.saaf.milkshop.nativeui.common

import android.app.Activity
import android.content.pm.ActivityInfo

/** The three layout fields the app optimizes for: phone, tablet, desktop. */
enum class SaafWidthBucket { COMPACT, MEDIUM, EXPANDED }

/**
 * Phones (smallest screen width < 600dp) are locked to portrait — layouts
 * are designed against the three width buckets, and a rotated phone would
 * fall into a bucket its layouts don't target. Tablets and desktop are
 * free to rotate; their layouts follow the window width.
 */
fun Activity.lockPortraitOnPhones() {
    if (resources.configuration.smallestScreenWidthDp < 600) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

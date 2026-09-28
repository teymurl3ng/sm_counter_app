package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * IME padding with the navigation-bar overlap subtracted.
 *
 * Samsung (and some other OEMs) report WindowInsets.ime as the keyboard
 * height PLUS the gesture-navigation zone the keyboard already covers —
 * plain imePadding() then leaves a dead background strip of exactly that
 * nav-bar height between the content and the visible keys. Excluding the
 * navigation-bars inset removes the double-count so content ends flush at
 * the keyboard.
 */
@Composable
fun Modifier.imePaddingExcludingNavBar(): Modifier = windowInsetsPadding(
    WindowInsets.ime.exclude(WindowInsets.navigationBars),
)

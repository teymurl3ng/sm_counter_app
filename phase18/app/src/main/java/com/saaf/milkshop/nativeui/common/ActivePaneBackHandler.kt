package com.saaf.milkshop.nativeui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalPaneActive = compositionLocalOf { true }

/** An outgoing animated pane can still draw, but must never handle Back. */
@Composable
fun ActivePaneBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled && LocalPaneActive.current, onBack)
}

package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility entry point for existing form callers. Standard IME padding
 * subtracts insets consumed by the parent scaffold. Manually subtracting a
 * navigation-bar height here gives hosted and standalone forms different
 * keyboard geometry and can leave the focused field partly covered.
 */
@Composable
fun Modifier.imePaddingExcludingNavBar(): Modifier = imePadding()

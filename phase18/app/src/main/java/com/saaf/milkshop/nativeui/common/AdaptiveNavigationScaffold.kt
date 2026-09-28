package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Switches primary navigation from a bottom bar to a rail at the Material compact/medium boundary. */
@Composable
internal fun AdaptiveNavigationScaffold(
    snackbarHostState: SnackbarHostState,
    compactNavigation: @Composable () -> Unit,
    expandedNavigation: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    // Zeroed by default: every screen leads with the green CashierScreenHeader,
    // which pads its own title band under the status bar — the full-bleed look.
    // (ScaffoldDefaults would insert an ivory status-bar strip above it.)
    contentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val useNavigationRail = maxWidth >= 600.dp
        // While the keyboard is open the bar would sit behind the IME but its
        // slot still pads the content, painting a dead background-colored band
        // above the keyboard. Drop the bar from composition instead — forms
        // (which imePadding themselves) then end exactly at the keyboard top.
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        Row(Modifier.fillMaxSize()) {
            if (useNavigationRail) expandedNavigation()
            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = containerColor,
                contentWindowInsets = contentWindowInsets,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail && !imeVisible) compactNavigation()
                },
                content = content,
            )
        }
    }
}

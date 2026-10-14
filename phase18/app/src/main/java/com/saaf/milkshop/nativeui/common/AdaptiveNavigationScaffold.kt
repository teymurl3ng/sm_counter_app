package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LocalNavigationHosted = compositionLocalOf { false }

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
    // Role hosts own the single navigation bar and snackbar. Child screens
    // still work independently in previews/tests without duplicating chrome.
    if (LocalNavigationHosted.current) {
        Box(modifier) { content(PaddingValues(0.dp)) }
        return
    }
    BoxWithConstraints(modifier = modifier) {
        val useNavigationRail = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (useNavigationRail) expandedNavigation()
            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = containerColor,
                contentWindowInsets = contentWindowInsets,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) compactNavigation()
                },
                content = { padding ->
                    // Navigation stays at the physical bottom, behind the IME.
                    // Account for its slot before applying only the remaining
                    // animated inset. Removing the slot on the first IME frame
                    // made every form jump, and unconsumed padding left a gap.
                    Box(Modifier.fillMaxSize().padding(padding)
                        .consumeWindowInsets(padding).imePadding()) {
                        CompositionLocalProvider(LocalNavigationHosted provides true) {
                            content(PaddingValues(0.dp))
                        }
                    }
                },
            )
        }
    }
}

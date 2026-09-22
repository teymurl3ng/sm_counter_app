package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Role-owned scaffold wrapper for multi-role hosted destinations (CORE METHOD).
 * Content screens call this instead of embedding OwnerSection / ManagerSection.
 *
 * [wrapNavAction] lets dirty-draft leave guards intercept bottom-nav exits:
 * `wrapNavAction = { action -> leaveGuard.requestLeave(action) }`.
 */
typealias HostedScaffold = @Composable (
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
    wrapNavAction: (navAction: () -> Unit) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) -> Unit

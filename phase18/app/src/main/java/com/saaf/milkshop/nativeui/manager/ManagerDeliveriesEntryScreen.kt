package com.saaf.milkshop.nativeui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.operations.DeliveriesLoadFailureBody
import com.saaf.milkshop.nativeui.operations.DeliveriesLoadingBody
import com.saaf.milkshop.nativeui.operations.DeliveriesReadyContent
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesDraftActions
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState

/**
 * Manager-owned shell around the role-neutral Deliveries entry surface.
 * Never mounts OwnerDeliveriesEntryScreen or Ops Deliveries.
 */
@Composable
fun ManagerDeliveriesEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeDeliveriesState,
    onRetry: () -> Unit,
    onMarkDelivered: (Long) -> Unit,
    onLinkSale: (Long, String) -> Unit,
    draftActions: NativeDeliveriesDraftActions,
    onArchive: (Long) -> Unit,
    onBackToWork: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackToWork)

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerDeliveriesEntryRoot),
    ) {
        ManagerScaffold(
            snackbarHostState = snackbarHostState,
            section = ManagerSection.Work,
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            when (state) {
                NativeDeliveriesState.Idle,
                NativeDeliveriesState.Loading,
                -> DeliveriesLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeDeliveriesState.LoadFailure -> DeliveriesLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding),
                )
                is NativeDeliveriesState.Ready -> DeliveriesReadyContent(
                    identity = identity,
                    state = state,
                    onMarkDelivered = onMarkDelivered,
                    onLinkSale = onLinkSale,
                    draftActions = draftActions,
                    onArchive = onArchive,
                    contentPadding = padding,
                )
            }
        }
    }
}

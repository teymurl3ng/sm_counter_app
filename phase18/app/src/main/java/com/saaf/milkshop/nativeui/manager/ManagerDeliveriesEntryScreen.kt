package com.saaf.milkshop.nativeui.manager

import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.expense.ExpenseDiscardDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
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
    onRegisterGuard: (EntryLeaveGuard?) -> Unit = {},
    mgrScaffold: MgrScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    DisposableEffect(leaveGuard) {
        onRegisterGuard(leaveGuard)
        onDispose { onRegisterGuard(null) }
    }
    val ready = state as? NativeDeliveriesState.Ready
    // M24: the draft (customer/product/quantity/route/notes) is real work —
    // leaving mid-draft must ask before discarding, mirroring the expense flow.
    val draftDirty = ready != null && (
        ready.draftCustomerId != null ||
            ready.draftProductId != null ||
            ready.draftQuantity.isNotBlank() ||
            ready.draftRoute.isNotBlank() ||
            ready.draftNotes.isNotBlank()
        )
    val canLeave = ready?.creating != true
    LaunchedEffect(draftDirty) {
        if (draftDirty) leaveGuard.markDirty() else leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerDeliveriesEntryRoot),
    ) {
        mgrScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            NativeSectionTransition(
                targetState = state,
                contentKey = { if (it is NativeDeliveriesState.Idle || it is NativeDeliveriesState.Loading) NativeDeliveriesState.Loading::class else it::class },
            ) { state -> when (state) {
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
        ExpenseDiscardDialog(leaveGuard)
    }
}

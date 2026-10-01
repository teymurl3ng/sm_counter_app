package com.saaf.milkshop.nativeui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.sale.NativeSaleRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.sale.SaleDiscardDialog
import com.saaf.milkshop.nativeui.sale.SaleEntryContent
import com.saaf.milkshop.nativeui.sale.SaleLoadFailureBody
import com.saaf.milkshop.nativeui.sale.SaleLoadingBody

/**
 * Manager-owned shell around the role-neutral Sale entry surface.
 * Never mounts OwnerSaleEntryScreen or Cashier Sell.
 */
@Composable
fun ManagerSaleEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeSaleState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onRecordSale: (NativeSaleRequest) -> Unit,
    onSendAsDelivery: (String) -> Unit,
    onStartNextSale: () -> Unit,
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
    val canLeave = (state as? NativeSaleState.Ready)?.saving != true

    LaunchedEffect(state is NativeSaleState.Ready) {
        if (state !is NativeSaleState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerSaleEntryRoot),
    ) {
        mgrScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            when (state) {
                NativeSaleState.Idle,
                NativeSaleState.Loading,
                -> SaleLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeSaleState.LoadFailure -> SaleLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding),
                )
                is NativeSaleState.Ready -> SaleEntryContent(
                    identity = identity,
                    state = state,
                    onClearFeedback = onClearFeedback,
                    onRecordSale = onRecordSale,
                    onSendAsDelivery = onSendAsDelivery,
                    onStartNextSale = onStartNextSale,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
        }
        SaleDiscardDialog(leaveGuard)
    }
}

package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
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
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentDiscardDialog
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentEntryContent
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentLoadFailureBody
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentLoadingBody

/**
 * Owner-owned shell around the role-neutral Pay supplier entry surface.
 */
@Composable
fun OwnerPaySupplierEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeVendorPaymentState,
    onRetry: () -> Unit,
    onRecordPayment: (NativeVendorPaymentRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToWork: () -> Unit,
    onRegisterGuard: (EntryLeaveGuard?) -> Unit = {},
    ownScaffold: OwnScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    DisposableEffect(leaveGuard) {
        onRegisterGuard(leaveGuard)
        onDispose { onRegisterGuard(null) }
    }
    val canLeave = (state as? NativeVendorPaymentState.Ready)?.saving != true

    LaunchedEffect(state is NativeVendorPaymentState.Ready) {
        if (state !is NativeVendorPaymentState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.OwnerPaySupplierEntryRoot),
    ) {
        ownScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            NativeSectionTransition(
                targetState = state,
                contentKey = { if (it is NativeVendorPaymentState.Idle || it is NativeVendorPaymentState.Loading) NativeVendorPaymentState.Loading::class else it::class },
            ) { state -> when (state) {
                NativeVendorPaymentState.Idle,
                NativeVendorPaymentState.Loading,
                -> VendorPaymentLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeVendorPaymentState.LoadFailure -> VendorPaymentLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding),
                )
                is NativeVendorPaymentState.Ready -> VendorPaymentEntryContent(
                    identity = identity,
                    state = state,
                    onRecordPayment = onRecordPayment,
                    onRecordAnother = onRecordAnother,
                    onBack = onBackToWork,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
            }
        }
        VendorPaymentDiscardDialog(leaveGuard)
    }
}

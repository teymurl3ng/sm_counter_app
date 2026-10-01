package com.saaf.milkshop.nativeui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentDiscardDialog
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentEntryContent
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentLoadFailureBody
import com.saaf.milkshop.nativeui.vendorpayment.VendorPaymentLoadingBody

/**
 * Manager-owned shell around the role-neutral Pay supplier entry surface.
 * Never mounts OwnerPaySupplierEntryScreen.
 */
@Composable
fun ManagerPaySupplierEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeVendorPaymentState,
    onRetry: () -> Unit,
    onRecordPayment: (NativeVendorPaymentRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToWork: () -> Unit,
    mgrScaffold: MgrScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
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
            .testTag(NativeUiTags.ManagerPaySupplierEntryRoot),
    ) {
        mgrScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            when (state) {
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
        VendorPaymentDiscardDialog(leaveGuard)
    }
}

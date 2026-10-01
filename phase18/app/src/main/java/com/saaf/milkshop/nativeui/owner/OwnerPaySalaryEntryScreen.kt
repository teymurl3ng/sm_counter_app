package com.saaf.milkshop.nativeui.owner

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
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.salarypayment.NativeSalaryPaymentState
import com.saaf.milkshop.nativeui.salarypayment.SalaryPaymentDiscardDialog
import com.saaf.milkshop.nativeui.salarypayment.SalaryPaymentEntryContent
import com.saaf.milkshop.nativeui.salarypayment.SalaryPaymentLoadFailureBody
import com.saaf.milkshop.nativeui.salarypayment.SalaryPaymentLoadingBody

/**
 * Owner-owned shell around the role-neutral Pay salary entry surface.
 */
@Composable
fun OwnerPaySalaryEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeSalaryPaymentState,
    onRetry: () -> Unit,
    onRecordPayment: (NativeSalaryPaymentRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToWork: () -> Unit,
    ownScaffold: OwnScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeSalaryPaymentState.Ready)?.saving != true

    LaunchedEffect(state is NativeSalaryPaymentState.Ready) {
        if (state !is NativeSalaryPaymentState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.OwnerPaySalaryEntryRoot),
    ) {
        ownScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            NativeSectionTransition(
                targetState = state,
                contentKey = { if (it is NativeSalaryPaymentState.Idle || it is NativeSalaryPaymentState.Loading) NativeSalaryPaymentState.Loading::class else it::class },
            ) { state -> when (state) {
                NativeSalaryPaymentState.Idle,
                NativeSalaryPaymentState.Loading,
                -> SalaryPaymentLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeSalaryPaymentState.LoadFailure -> SalaryPaymentLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding),
                )
                is NativeSalaryPaymentState.Ready -> SalaryPaymentEntryContent(
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
        SalaryPaymentDiscardDialog(leaveGuard)
    }
}

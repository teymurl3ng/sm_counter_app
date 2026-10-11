package com.saaf.milkshop.nativeui.manager

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
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseDayRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.cashclose.CashCloseDiscardDialog
import com.saaf.milkshop.nativeui.cashclose.CashCloseEntryContent
import com.saaf.milkshop.nativeui.cashclose.CashCloseLoadFailureBody
import com.saaf.milkshop.nativeui.cashclose.CashCloseLoadingBody
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard

/**
 * Manager-owned shell around role-neutral Cash count and close.
 * Reopen is Owner-only — hidden here (never mounts OwnerCashCloseEntryScreen).
 */
@Composable
fun ManagerCashCloseEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeCashCloseState,
    onRetry: () -> Unit,
    onRecordCount: (NativeCashCloseCountRequest) -> Unit,
    onCloseDay: (NativeCashCloseDayRequest) -> Unit,
    onResolveDelivery: (Long) -> Unit = {},
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
    val canLeave = (state as? NativeCashCloseState.Ready)?.saving != true

    LaunchedEffect(state is NativeCashCloseState.Ready) {
        if (state !is NativeCashCloseState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerCloseRoot),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .testTag(NativeUiTags.ManagerCashCloseEntryRoot),
        ) {
            mgrScaffold(
                snackbarHostState,
                Modifier.fillMaxSize(),
            ) { padding ->
                NativeSectionTransition(
                    targetState = state,
                    contentKey = { if (it is NativeCashCloseState.Idle || it is NativeCashCloseState.Loading) NativeCashCloseState.Loading::class else it::class },
                ) { state -> when (state) {
                    NativeCashCloseState.Idle,
                    NativeCashCloseState.Loading,
                    -> CashCloseLoadingBody(
                        modifier = Modifier.padding(padding),
                    )
                    is NativeCashCloseState.LoadFailure -> CashCloseLoadFailureBody(
                        message = state.message,
                        onRetry = onRetry,
                        modifier = Modifier.padding(padding),
                    )
                    is NativeCashCloseState.Ready -> CashCloseEntryContent(
                        identity = identity,
                        state = state,
                        onRecordCount = onRecordCount,
                        onCloseDay = onCloseDay,
                    onResolveDelivery = onResolveDelivery,
                        onReopenDay = { _, _ -> },
                        onBack = onBackToWork,
                        leaveGuard = leaveGuard,
                        contentPadding = padding,
                        allowReopen = false,
                    )
                }
                }
            }
            CashCloseDiscardDialog(leaveGuard)
        }
    }
}

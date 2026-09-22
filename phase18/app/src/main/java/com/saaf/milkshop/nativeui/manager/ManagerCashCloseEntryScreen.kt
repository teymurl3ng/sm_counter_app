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
import com.saaf.milkshop.application.cashclose.NativeCashCloseCountRequest
import com.saaf.milkshop.application.cashclose.NativeCashCloseDayRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.cashclose.CashCloseDiscardDialog
import com.saaf.milkshop.nativeui.cashclose.CashCloseEntryContent
import com.saaf.milkshop.nativeui.cashclose.CashCloseLoadFailureBody
import com.saaf.milkshop.nativeui.cashclose.CashCloseLoadingBody
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
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
    onBackToWork: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
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
            ManagerScaffold(
                snackbarHostState = snackbarHostState,
                section = ManagerSection.Close,
                onNavigate = { section ->
                    if (canLeave) {
                        leaveGuard.requestLeave { onNavigate(section) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) { padding ->
                when (state) {
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
                        onReopenDay = { _, _ -> },
                        onBack = onBackToWork,
                        leaveGuard = leaveGuard,
                        contentPadding = padding,
                        allowReopen = false,
                    )
                }
            }
            CashCloseDiscardDialog(leaveGuard)
        }
    }
}

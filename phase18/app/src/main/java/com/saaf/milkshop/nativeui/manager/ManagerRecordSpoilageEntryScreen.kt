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
import com.saaf.milkshop.application.operations.NativeRecordSpoilageRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageState
import com.saaf.milkshop.nativeui.operations.RecordSpoilageDiscardDialog
import com.saaf.milkshop.nativeui.operations.RecordSpoilageLoadFailureBody
import com.saaf.milkshop.nativeui.operations.RecordSpoilageLoadingBody
import com.saaf.milkshop.nativeui.operations.RecordSpoilageReadyContent

/**
 * Manager-owned shell around the role-neutral Record Spoilage entry surface.
 * Never mounts OwnerRecordSpoilageEntryScreen or Ops Work.
 */
@Composable
fun ManagerRecordSpoilageEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeRecordSpoilageState,
    onRetry: () -> Unit,
    onSelectProduct: (Long) -> Unit,
    onOpenProductPicker: () -> Unit,
    onCloseProductPicker: () -> Unit,
    onSelectReason: (String) -> Unit,
    onOpenReasonPicker: () -> Unit,
    onCloseReasonPicker: () -> Unit,
    onRecord: (NativeRecordSpoilageRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToWork: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeRecordSpoilageState.Ready)?.saving != true

    LaunchedEffect(state is NativeRecordSpoilageState.Ready) {
        if (state !is NativeRecordSpoilageState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerRecordSpoilageEntryRoot),
    ) {
        ManagerScaffold(
            snackbarHostState = snackbarHostState,
            section = ManagerSection.Work,
            onNavigate = { section ->
                if (canLeave) {
                    leaveGuard.requestLeave { onNavigate(section) }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            when (state) {
                NativeRecordSpoilageState.Idle,
                NativeRecordSpoilageState.Loading,
                -> RecordSpoilageLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeRecordSpoilageState.LoadFailure -> RecordSpoilageLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    onBackToHome = onBackToWork,
                    modifier = Modifier.padding(padding),
                )
                is NativeRecordSpoilageState.Ready -> RecordSpoilageReadyContent(
                    identity = identity,
                    state = state,
                    onSelectProduct = onSelectProduct,
                    onOpenProductPicker = onOpenProductPicker,
                    onCloseProductPicker = onCloseProductPicker,
                    onSelectReason = onSelectReason,
                    onOpenReasonPicker = onOpenReasonPicker,
                    onCloseReasonPicker = onCloseReasonPicker,
                    onRecord = onRecord,
                    onRecordAnother = onRecordAnother,
                    onBackToHome = onBackToWork,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
        }
        RecordSpoilageDiscardDialog(leaveGuard)
    }
}

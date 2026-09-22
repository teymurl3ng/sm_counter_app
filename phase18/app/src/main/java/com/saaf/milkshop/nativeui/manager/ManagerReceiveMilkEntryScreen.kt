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
import com.saaf.milkshop.application.operations.NativeReceiveMilkPreview
import com.saaf.milkshop.application.operations.NativeReceiveMilkRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkState
import com.saaf.milkshop.nativeui.operations.ReceiveMilkDiscardDialog
import com.saaf.milkshop.nativeui.operations.ReceiveMilkLoadFailureBody
import com.saaf.milkshop.nativeui.operations.ReceiveMilkLoadingBody
import com.saaf.milkshop.nativeui.operations.ReceiveMilkReadyContent

/**
 * Manager-owned shell around the role-neutral Receive Milk entry surface.
 * Never mounts OwnerReceiveMilkEntryScreen or Ops Work.
 */
@Composable
fun ManagerReceiveMilkEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeReceiveMilkState,
    onRetry: () -> Unit,
    onSelectVendor: (Long) -> Unit,
    onOpenVendorPicker: () -> Unit,
    onCloseVendorPicker: () -> Unit,
    onPreview: (Double, Double, Double) -> NativeReceiveMilkPreview?,
    onRecord: (NativeReceiveMilkRequest) -> Unit,
    onReceiveAnother: () -> Unit,
    onBackToWork: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeReceiveMilkState.Ready)?.saving != true

    LaunchedEffect(state is NativeReceiveMilkState.Ready) {
        if (state !is NativeReceiveMilkState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerReceiveMilkEntryRoot),
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
                NativeReceiveMilkState.Idle,
                NativeReceiveMilkState.Loading,
                -> ReceiveMilkLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeReceiveMilkState.LoadFailure -> ReceiveMilkLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    onBackToHome = onBackToWork,
                    modifier = Modifier.padding(padding),
                )
                is NativeReceiveMilkState.Ready -> ReceiveMilkReadyContent(
                    identity = identity,
                    state = state,
                    onSelectVendor = onSelectVendor,
                    onOpenVendorPicker = onOpenVendorPicker,
                    onCloseVendorPicker = onCloseVendorPicker,
                    onPreview = onPreview,
                    onRecord = onRecord,
                    onReceiveAnother = onReceiveAnother,
                    onBackToHome = onBackToWork,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
        }
        ReceiveMilkDiscardDialog(leaveGuard)
    }
}

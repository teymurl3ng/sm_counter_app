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
import com.saaf.milkshop.application.operations.NativeMakeYogurtRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.operations.MakeYogurtDiscardDialog
import com.saaf.milkshop.nativeui.operations.MakeYogurtLoadFailureBody
import com.saaf.milkshop.nativeui.operations.MakeYogurtLoadingBody
import com.saaf.milkshop.nativeui.operations.MakeYogurtReadyContent
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtState

/**
 * Manager-owned shell around the role-neutral Make Yogurt entry surface.
 * Never mounts OwnerMakeYogurtEntryScreen or Ops Work.
 */
@Composable
fun ManagerMakeYogurtEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeMakeYogurtState,
    onRetry: () -> Unit,
    onRecord: (NativeMakeYogurtRequest) -> Unit,
    onMakeAnother: () -> Unit,
    onBackToWork: () -> Unit,
    mgrScaffold: MgrScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeMakeYogurtState.Ready)?.saving != true

    LaunchedEffect(state is NativeMakeYogurtState.Ready) {
        if (state !is NativeMakeYogurtState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerMakeYogurtEntryRoot),
    ) {
        mgrScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            when (state) {
                NativeMakeYogurtState.Idle,
                NativeMakeYogurtState.Loading,
                -> MakeYogurtLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeMakeYogurtState.LoadFailure -> MakeYogurtLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    onBackToHome = onBackToWork,
                    modifier = Modifier.padding(padding),
                )
                is NativeMakeYogurtState.Ready -> MakeYogurtReadyContent(
                    identity = identity,
                    state = state,
                    onRecord = onRecord,
                    onMakeAnother = onMakeAnother,
                    onBackToHome = onBackToWork,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
        }
        MakeYogurtDiscardDialog(leaveGuard)
    }
}

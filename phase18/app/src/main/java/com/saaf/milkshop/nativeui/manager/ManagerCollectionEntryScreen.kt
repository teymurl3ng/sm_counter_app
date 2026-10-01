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
import com.saaf.milkshop.application.collection.NativeCollectionRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.collection.CollectionDiscardDialog
import com.saaf.milkshop.nativeui.collection.CollectionEntryContent
import com.saaf.milkshop.nativeui.collection.CollectionLoadFailureBody
import com.saaf.milkshop.nativeui.collection.CollectionLoadingBody
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard

/**
 * Manager-owned shell around the role-neutral Collection entry surface.
 * Never mounts OwnerCollectionEntryScreen or Cashier Collect.
 */
@Composable
fun ManagerCollectionEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeCollectionState,
    onRetry: () -> Unit,
    onSelectCustomer: (Long) -> Unit,
    onRecordCollection: (NativeCollectionRequest) -> Unit,
    onCollectAnother: () -> Unit,
    onViewRecord: (Long) -> Unit,
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
    val canLeave = (state as? NativeCollectionState.Ready)?.saving != true

    LaunchedEffect(state is NativeCollectionState.Ready) {
        if (state !is NativeCollectionState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerCollectionEntryRoot),
    ) {
        mgrScaffold(
            snackbarHostState,
            Modifier.fillMaxSize(),
        ) { padding ->
            NativeSectionTransition(
                targetState = state,
                contentKey = { if (it is NativeCollectionState.Idle || it is NativeCollectionState.Loading) NativeCollectionState.Loading::class else it::class },
            ) { state -> when (state) {
                NativeCollectionState.Idle,
                NativeCollectionState.Loading,
                -> CollectionLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeCollectionState.LoadFailure -> CollectionLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding),
                )
                is NativeCollectionState.Ready -> CollectionEntryContent(
                    identity = identity,
                    state = state,
                    onSelectCustomer = onSelectCustomer,
                    onRecordCollection = onRecordCollection,
                    onCollectAnother = onCollectAnother,
                    onViewRecord = onViewRecord,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
            }
        }
        CollectionDiscardDialog(leaveGuard)
    }
}

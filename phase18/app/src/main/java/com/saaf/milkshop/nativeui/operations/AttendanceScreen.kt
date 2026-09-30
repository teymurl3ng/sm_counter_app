package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.operations.NativeAttendanceLink
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.common.hapticCommit
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.common.NativeLoadFailurePane
import com.saaf.milkshop.nativeui.common.nativeRefreshOverlay
import com.saaf.milkshop.nativeui.common.NativeColdLoadingPane

@Composable
fun AttendanceScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeAttendanceState,
    onRetry: () -> Unit,
    onMarkPresent: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { if (it is NativeAttendanceState.Idle || it is NativeAttendanceState.Loading) NativeAttendanceState.Loading::class else it::class },
    ) { state -> when (state) {
        NativeAttendanceState.Idle,
        NativeAttendanceState.Loading,
        -> AttendanceLoading(snackbarHostState, onNavigate, onLeaveConfirmed, modifier)

        is NativeAttendanceState.LoadFailure -> AttendanceLoadFailure(
            snackbarHostState = snackbarHostState,
            message = state.message,
            onRetry = onRetry,
            onNavigate = onNavigate,
            onLeaveConfirmed = onLeaveConfirmed,
            modifier = modifier,
        )

        is NativeAttendanceState.Ready -> AttendanceReady(
            snackbarHostState = snackbarHostState,
            identity = identity,
            state = state,
            onMarkPresent = onMarkPresent,
            onNavigate = onNavigate,
            onLeaveConfirmed = onLeaveConfirmed,
            modifier = modifier,
        )
    }
    }
}

@Composable
private fun AttendanceLoading(
    snackbarHostState: SnackbarHostState,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier,
) {
    WorkScaffold(snackbarHostState, OperationsSection.Attendance, onNavigate, modifier) {
        NativeColdLoadingPane(
            label = "Loading attendance…",
            modifier = Modifier.fillMaxSize().padding(it).testTag(NativeUiTags.OpsAttendanceLoading),
        )
    }
}

@Composable
private fun AttendanceLoadFailure(
    snackbarHostState: SnackbarHostState,
    message: String,
    onRetry: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier,
) {
    WorkScaffold(snackbarHostState, OperationsSection.Attendance, onNavigate, modifier) { padding ->
        NativeLoadFailurePane(
            message = message,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize().padding(padding),
            retryTestTag = NativeUiTags.OpsAttendanceRetryLoad,
        )
    }
}

@Composable
private fun AttendanceReady(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeAttendanceState.Ready,
    onMarkPresent: () -> Unit,
    onNavigate: (OperationsSection) -> Unit,
    onLeaveConfirmed: () -> Unit,
    modifier: Modifier,
) {
    WorkScaffold(snackbarHostState, OperationsSection.Attendance, onNavigate, modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize().nativeRefreshOverlay(state.refreshing)
                .padding(padding)
                .testTag(NativeUiTags.OpsAttendanceRoot),
        ) {
            CashierScreenHeader(
                title = "My attendance",
                displayName = identity.displayName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = state.context.businessDate,
                postingOpen = state.context.postingOpen,
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.context.postingOpen) {
                    Text(
                        "This business day is closed. Ask Owner to reopen it before marking attendance.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OpsAttendanceClosed),
                    )
                }
                if (state.error != null) {
                    Text(
                        state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OpsAttendanceError),
                    )
                }
                when (val link = state.context.link) {
                    NativeAttendanceLink.Unlinked -> UnlinkedCard()
                    is NativeAttendanceLink.Linked -> LinkedBody(
                        link = link,
                        postingOpen = state.context.postingOpen,
                        saving = state.saving,
                        onMarkPresent = onMarkPresent,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlinkedCard() {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsAttendanceUnlinked),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.PersonOff, contentDescription = null)
            Text(
                "Account not linked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Ask the Owner or Administrator to link this account to an employee.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinkedBody(
    link: NativeAttendanceLink.Linked,
    postingOpen: Boolean,
    saving: Boolean,
    onMarkPresent: () -> Unit,
) {
val hapticContext = LocalContext.current
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NativeUiTags.OpsAttendanceLinked),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Employee", style = MaterialTheme.typography.labelMedium)
            Text(
                link.employeeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(NativeUiTags.OpsAttendanceEmployee),
            )
            if (link.todayStatus != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag(NativeUiTags.OpsAttendanceMarked),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Today: ${link.todayStatus}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                Text(
                    "Not marked yet today.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(NativeUiTags.OpsAttendanceNotMarked),
                )
                val press = rememberPressSource()
                Button(
                    onClick = { hapticContext.hapticCommit(); onMarkPresent() },
                    enabled = postingOpen && !saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaafYellow,
                        contentColor = SaafInk,
                    ),
                    modifier = Modifier
                        .pressScale(press)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag(NativeUiTags.OpsAttendanceMarkPresent),
                    interactionSource = press,
                ) {
                    Icon(Icons.Outlined.EventAvailable, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (saving) "Marking…" else "Mark present",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

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
import com.saaf.milkshop.application.expense.NativeExpenseRequest
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.expense.ExpenseDiscardDialog
import com.saaf.milkshop.nativeui.expense.ExpenseEntryContent
import com.saaf.milkshop.nativeui.expense.ExpenseLoadFailureBody
import com.saaf.milkshop.nativeui.expense.ExpenseLoadingBody
import com.saaf.milkshop.nativeui.expense.NativeExpenseState

/**
 * Manager-owned shell around the role-neutral Expense entry surface.
 * Never mounts OwnerExpenseEntryScreen or CashierExpenseEntryScreen.
 */
@Composable
fun ManagerExpenseEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeExpenseState,
    onRetry: () -> Unit,
    onRecordExpense: (NativeExpenseRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToWork: () -> Unit,
    onNavigate: (ManagerSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeExpenseState.Ready)?.saving != true

    LaunchedEffect(state is NativeExpenseState.Ready) {
        if (state !is NativeExpenseState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToWork)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.ManagerExpenseEntryRoot),
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
                NativeExpenseState.Idle,
                NativeExpenseState.Loading,
                -> ExpenseLoadingBody(
                    modifier = Modifier.padding(padding),
                )
                is NativeExpenseState.LoadFailure -> ExpenseLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding),
                )
                is NativeExpenseState.Ready -> ExpenseEntryContent(
                    identity = identity,
                    state = state,
                    onRecordExpense = onRecordExpense,
                    onRecordAnother = onRecordAnother,
                    onBack = onBackToWork,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
        }
        ExpenseDiscardDialog(leaveGuard)
    }
}

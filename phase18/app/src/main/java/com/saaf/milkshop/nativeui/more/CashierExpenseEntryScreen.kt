package com.saaf.milkshop.nativeui.more

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import com.saaf.milkshop.nativeui.sale.CashierNavigation
import com.saaf.milkshop.nativeui.sale.CashierSection

/**
 * Cashier-owned shell around the role-neutral Expense entry surface.
 * Never mounts OwnerExpenseEntryScreen or Owner chrome.
 */
@Composable
fun CashierExpenseEntryScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    state: NativeExpenseState,
    onRetry: () -> Unit,
    onRecordExpense: (NativeExpenseRequest) -> Unit,
    onRecordAnother: () -> Unit,
    onBackToMore: () -> Unit,
    onNavigate: (CashierSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeExpenseState.Ready)?.saving != true

    LaunchedEffect(state is NativeExpenseState.Ready) {
        if (state !is NativeExpenseState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToMore)
    }

    Box(
        modifier
            .fillMaxSize()
            .testTag(NativeUiTags.CashierExpenseEntryRoot),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                CashierNavigation(
                    selected = CashierSection.More,
                    onNavigate = { section ->
                        if (canLeave) {
                            leaveGuard.requestLeave { onNavigate(section) }
                        }
                    },
                )
            },
        ) { padding ->
            when (state) {
                NativeExpenseState.Idle,
                NativeExpenseState.Loading,
                -> ExpenseLoadingBody(
                    modifier = Modifier
                        .padding(padding)
                        .statusBarsPadding(),
                )
                is NativeExpenseState.LoadFailure -> ExpenseLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier
                        .padding(padding)
                        .statusBarsPadding(),
                )
                is NativeExpenseState.Ready -> ExpenseEntryContent(
                    identity = identity,
                    state = state,
                    onRecordExpense = onRecordExpense,
                    onRecordAnother = onRecordAnother,
                    onBack = onBackToMore,
                    leaveGuard = leaveGuard,
                    contentPadding = padding,
                )
            }
        }
        ExpenseDiscardDialog(leaveGuard)
    }
}

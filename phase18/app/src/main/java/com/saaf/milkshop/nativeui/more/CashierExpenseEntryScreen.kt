package com.saaf.milkshop.nativeui.more

import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.expense.NativeExpenseRequest
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
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
    onRegisterGuard: (com.saaf.milkshop.nativeui.common.EntryLeaveGuard?) -> Unit = {},
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val canLeave = (state as? NativeExpenseState.Ready)?.saving != true

    LaunchedEffect(state is NativeExpenseState.Ready) {
        if (state !is NativeExpenseState.Ready) leaveGuard.clear()
    }
    BackHandler {
        if (canLeave) leaveGuard.requestLeave(onBackToMore)
    }

    DisposableEffect(leaveGuard) {
        onRegisterGuard(leaveGuard)
        onDispose { onRegisterGuard(null) }
    }

    com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold(
        snackbarHostState = snackbarHostState,
        compactNavigation = { CashierNavigation(CashierSection.More) { section -> leaveGuard.requestLeave { onNavigate(section) } } },
        expandedNavigation = { com.saaf.milkshop.nativeui.sale.CashierNavigationRail(CashierSection.More) { section -> leaveGuard.requestLeave { onNavigate(section) } } },
        modifier = modifier.fillMaxSize().testTag(NativeUiTags.CashierExpenseEntryRoot),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NativeSectionTransition(
            targetState = state,
            contentKey = { if (it is NativeExpenseState.Idle || it is NativeExpenseState.Loading) NativeExpenseState.Loading::class else it::class },
        ) { state -> when (state) {
                NativeExpenseState.Idle,
                NativeExpenseState.Loading,
                -> ExpenseLoadingBody(
                    modifier = Modifier,
                )
                is NativeExpenseState.LoadFailure -> ExpenseLoadFailureBody(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier,
                )
                is NativeExpenseState.Ready -> ExpenseEntryContent(
                    identity = identity,
                    state = state,
                    onRecordExpense = onRecordExpense,
                    onRecordAnother = onRecordAnother,
                    onBack = onBackToMore,
                    leaveGuard = leaveGuard,
                    contentPadding = PaddingValues(0.dp),
                    showHeader = false,
                )
            }
        }
        }
        ExpenseDiscardDialog(leaveGuard)
    }
}

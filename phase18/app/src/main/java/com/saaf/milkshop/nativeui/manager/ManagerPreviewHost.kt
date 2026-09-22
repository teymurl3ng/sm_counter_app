package com.saaf.milkshop.nativeui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseViewModel
import com.saaf.milkshop.nativeui.expense.NativeExpenseState
import com.saaf.milkshop.nativeui.expense.NativeExpenseViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsViewModel
import com.saaf.milkshop.nativeui.owner.OwnerRecordsScreen
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentViewModel
import java.time.LocalDate

/**
 * Manager-authenticated preview host (M2): Work desk + Close host + Records + bounded More.
 * Never mounts Owner/Cashier/Ops/Admin full screens or Owner-only More rows.
 */
@Composable
fun ManagerPreviewHost(
    sessionHandle: String,
    identity: NativeSignedInIdentity,
    snackbarHostState: SnackbarHostState,
    managerSection: ManagerSection,
    onManagerSectionChange: (ManagerSection) -> Unit,
    managerWorkState: NativeManagerWorkState,
    managerWorkViewModel: NativeManagerWorkViewModel,
    expenseState: NativeExpenseState,
    expenseViewModel: NativeExpenseViewModel,
    vendorPaymentState: NativeVendorPaymentState,
    vendorPaymentViewModel: NativeVendorPaymentViewModel,
    cashCloseState: NativeCashCloseState,
    cashCloseViewModel: NativeCashCloseViewModel,
    ownerRecordsState: NativeOwnerRecordsState,
    ownerRecordsViewModel: NativeOwnerRecordsViewModel,
    ownerMoreState: NativeOwnerMoreState,
    ownerMoreViewModel: NativeOwnerMoreViewModel,
    onLeaveToWelcome: () -> Unit,
    onLeaveToAccountPicker: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    SaafTheme(darkTheme = false) {
        var lastSession by rememberSaveable { mutableStateOf(identity.loginName) }

        fun closeEntries() {
            expenseViewModel.close()
            vendorPaymentViewModel.close()
            cashCloseViewModel.close()
        }

        fun returnManagerWorkHome() {
            closeEntries()
            managerWorkViewModel.backToHome()
        }

        LaunchedEffect(identity.loginName) {
            if (lastSession != identity.loginName) {
                onManagerSectionChange(ManagerSection.Work)
                managerWorkViewModel.close()
                ownerRecordsViewModel.close()
                ownerMoreViewModel.close()
                closeEntries()
            }
            lastSession = identity.loginName
        }

        LaunchedEffect(sessionHandle, managerSection) {
            when (managerSection) {
                ManagerSection.Work -> {
                    cashCloseViewModel.close()
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.close()
                    managerWorkViewModel.open(sessionHandle)
                }
                ManagerSection.Close -> {
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.close()
                    managerWorkViewModel.backToHome()
                    cashCloseViewModel.open(sessionHandle)
                }
                ManagerSection.Records -> {
                    managerWorkViewModel.close()
                    closeEntries()
                    ownerMoreViewModel.close()
                    ownerRecordsViewModel.open(sessionHandle)
                }
                ManagerSection.More -> {
                    managerWorkViewModel.close()
                    closeEntries()
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.open(sessionHandle)
                }
            }
        }

        LaunchedEffect(
            sessionHandle,
            managerSection,
            (managerWorkState as? NativeManagerWorkState.Ready)?.destination,
        ) {
            val workDestination =
                (managerWorkState as? NativeManagerWorkState.Ready)?.destination
            when {
                managerSection != ManagerSection.Work -> {
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                }
                workDestination == NativeManagerWorkDestination.RecordExpense -> {
                    vendorPaymentViewModel.close()
                    cashCloseViewModel.close()
                    expenseViewModel.open(sessionHandle)
                }
                workDestination == NativeManagerWorkDestination.PaySupplier -> {
                    expenseViewModel.close()
                    cashCloseViewModel.close()
                    vendorPaymentViewModel.open(sessionHandle)
                }
                else -> {
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                }
            }
        }

        val workDestination =
            (managerWorkState as? NativeManagerWorkState.Ready)?.destination
        val workEntryOwnsBack = managerSection == ManagerSection.Work &&
            workDestination in setOf(
                NativeManagerWorkDestination.RecordExpense,
                NativeManagerWorkDestination.PaySupplier,
            )
        val closeOwnsBack = managerSection == ManagerSection.Close
        val recordsDetailOwnsBack = managerSection == ManagerSection.Records &&
            (ownerRecordsState as? NativeOwnerRecordsState.Ready)?.detail != null
        val moreDestinationOwnsBack = managerSection == ManagerSection.More &&
            (ownerMoreState as? NativeOwnerMoreState.Ready)?.destination in setOf(
                NativeOwnerMoreDestination.PersonalSecurity,
                NativeOwnerMoreDestination.Products,
                NativeOwnerMoreDestination.Customers,
                NativeOwnerMoreDestination.Suppliers,
                NativeOwnerMoreDestination.Employees,
            )

        // Nested entry screens own Back. Work home consumes Back (stay authenticated).
        // Close / Records list / More menu return to Work.
        if (!workEntryOwnsBack && !closeOwnsBack && !recordsDetailOwnsBack && !moreDestinationOwnsBack) {
            BackHandler {
                when (managerSection) {
                    ManagerSection.Work -> Unit
                    ManagerSection.Close,
                    ManagerSection.Records,
                    ManagerSection.More,
                    -> onManagerSectionChange(ManagerSection.Work)
                }
            }
        }

        if (recordsDetailOwnsBack) {
            // OwnerRecordsScreen detail BackHandler owns Back → list.
        }
        // More destinations own Back via their screens (menu / security / catalog).

        val navigate: (ManagerSection) -> Unit = { section ->
            if (section != ManagerSection.Work) {
                returnManagerWorkHome()
            }
            if (section != ManagerSection.Records) {
                ownerRecordsViewModel.close()
            }
            if (section != ManagerSection.More) {
                ownerMoreViewModel.close()
            } else {
                ownerMoreViewModel.backToMenu()
            }
            onManagerSectionChange(section)
        }

        val businessDate = LocalDate.now().toString()
        when {
            managerSection == ManagerSection.Work &&
                workDestination == NativeManagerWorkDestination.RecordExpense -> {
                ManagerExpenseEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = expenseState,
                    onRetry = expenseViewModel::refresh,
                    onRecordExpense = expenseViewModel::record,
                    onRecordAnother = expenseViewModel::recordAnother,
                    onBackToWork = { returnManagerWorkHome() },
                    onNavigate = navigate,
                )
            }
            managerSection == ManagerSection.Work &&
                workDestination == NativeManagerWorkDestination.PaySupplier -> {
                ManagerPaySupplierEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = vendorPaymentState,
                    onRetry = vendorPaymentViewModel::refresh,
                    onRecordPayment = vendorPaymentViewModel::record,
                    onRecordAnother = vendorPaymentViewModel::recordAnother,
                    onBackToWork = { returnManagerWorkHome() },
                    onNavigate = navigate,
                )
            }
            managerSection == ManagerSection.Close -> {
                ManagerCashCloseEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = cashCloseState,
                    onRetry = cashCloseViewModel::refresh,
                    onRecordCount = cashCloseViewModel::recordCount,
                    onCloseDay = cashCloseViewModel::closeDay,
                    onBackToWork = {
                        cashCloseViewModel.close()
                        onManagerSectionChange(ManagerSection.Work)
                    },
                    onNavigate = navigate,
                )
            }
            managerSection == ManagerSection.Work -> {
                ManagerWorkScreen(
                    snackbarHostState = snackbarHostState,
                    state = managerWorkState,
                    onRetry = managerWorkViewModel::refresh,
                    onOpenDestination = managerWorkViewModel::openDestination,
                    onOpenClose = { onManagerSectionChange(ManagerSection.Close) },
                    onBackToHome = { returnManagerWorkHome() },
                    onLeaveConfirmed = {
                        // Stay on Work home; session remains authenticated.
                    },
                    onNavigate = navigate,
                )
            }
            managerSection == ManagerSection.Records -> {
                OwnerRecordsScreen(
                    snackbarHostState = snackbarHostState,
                    state = ownerRecordsState,
                    onRetry = ownerRecordsViewModel::refresh,
                    onSearchQueryChange = ownerRecordsViewModel::setSearchQuery,
                    onOpenRecord = ownerRecordsViewModel::openDetail,
                    onBackFromDetail = ownerRecordsViewModel::clearDetail,
                    onLeaveConfirmed = { onManagerSectionChange(ManagerSection.Work) },
                    hostedScaffold = { snackbar, mod, wrapNav, content ->
                        ManagerScaffold(
                            snackbarHostState = snackbar,
                            section = ManagerSection.Records,
                            onNavigate = { section -> wrapNav { navigate(section) } },
                            modifier = mod,
                            content = content,
                        )
                    },
                    roleLabel = "Manager",
                    rootTag = NativeUiTags.ManagerRecordsRoot,
                    loadingTag = NativeUiTags.ManagerRecordsLoading,
                    onDeferredGroup = { group ->
                        onShowNotice(
                            "$group write UI comes next. Corrections stay deferred for Manager.",
                        )
                    },
                )
            }
            else -> {
                ManagerMoreScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = ownerMoreState,
                    onRetry = ownerMoreViewModel::refresh,
                    onOpenPersonalSecurity = ownerMoreViewModel::openPersonalSecurity,
                    onOpenProducts = ownerMoreViewModel::openProducts,
                    onOpenCustomers = ownerMoreViewModel::openCustomers,
                    onOpenSuppliers = ownerMoreViewModel::openSuppliers,
                    onOpenEmployees = ownerMoreViewModel::openEmployees,
                    onBackToMenu = ownerMoreViewModel::backToMenu,
                    onRetryProducts = ownerMoreViewModel::refreshProducts,
                    onOpenProduct = ownerMoreViewModel::openProductDetail,
                    onBackFromProductDetail = ownerMoreViewModel::clearProductDetail,
                    onOpenProductCreate = ownerMoreViewModel::openProductCreate,
                    onBackFromProductCreate = ownerMoreViewModel::clearProductCreate,
                    onCreateProduct = ownerMoreViewModel::createProduct,
                    onOpenProductEdit = ownerMoreViewModel::openProductEdit,
                    onBackFromProductEdit = ownerMoreViewModel::clearProductEdit,
                    onUpdateProduct = ownerMoreViewModel::updateProduct,
                    onArchiveProduct = ownerMoreViewModel::archiveProduct,
                    onUnarchiveProduct = ownerMoreViewModel::unarchiveProduct,
                    onProductsShowArchivedChange = ownerMoreViewModel::setProductsShowArchived,
                    onRetryCustomers = ownerMoreViewModel::refreshCustomers,
                    onOpenCustomer = ownerMoreViewModel::openCustomerDetail,
                    onBackFromCustomerDetail = ownerMoreViewModel::clearCustomerDetail,
                    onOpenCustomerCreate = ownerMoreViewModel::openCustomerCreate,
                    onBackFromCustomerCreate = ownerMoreViewModel::clearCustomerCreate,
                    onCreateCustomer = ownerMoreViewModel::createCustomer,
                    onOpenCustomerEdit = ownerMoreViewModel::openCustomerEdit,
                    onBackFromCustomerEdit = ownerMoreViewModel::clearCustomerEdit,
                    onUpdateCustomer = ownerMoreViewModel::updateCustomer,
                    onRetrySuppliers = ownerMoreViewModel::refreshSuppliers,
                    onOpenSupplier = ownerMoreViewModel::openSupplierDetail,
                    onBackFromSupplierDetail = ownerMoreViewModel::clearSupplierDetail,
                    onOpenSupplierCreate = ownerMoreViewModel::openSupplierCreate,
                    onBackFromSupplierCreate = ownerMoreViewModel::clearSupplierCreate,
                    onCreateSupplier = ownerMoreViewModel::createSupplier,
                    onOpenSupplierEdit = ownerMoreViewModel::openSupplierEdit,
                    onBackFromSupplierEdit = ownerMoreViewModel::clearSupplierEdit,
                    onUpdateSupplier = ownerMoreViewModel::updateSupplier,
                    onRetryEmployees = ownerMoreViewModel::refreshEmployees,
                    onOpenEmployee = ownerMoreViewModel::openEmployeeDetail,
                    onBackFromEmployeeDetail = ownerMoreViewModel::clearEmployeeDetail,
                    onOpenEmployeeCreate = ownerMoreViewModel::openEmployeeCreate,
                    onBackFromEmployeeCreate = ownerMoreViewModel::clearEmployeeCreate,
                    onCreateEmployee = ownerMoreViewModel::createEmployee,
                    onOpenEmployeeEdit = ownerMoreViewModel::openEmployeeEdit,
                    onBackFromEmployeeEdit = ownerMoreViewModel::clearEmployeeEdit,
                    onUpdateEmployee = ownerMoreViewModel::updateEmployee,
                    onRequestRemoveQuickUnlock = ownerMoreViewModel::requestRemoveQuickUnlock,
                    onCancelRemoveQuickUnlock = ownerMoreViewModel::cancelRemoveQuickUnlock,
                    onRemoveQuickUnlock = ownerMoreViewModel::removeQuickUnlock,
                    onEnrollQuickPin = ownerMoreViewModel::enrollQuickPin,
                    onEnrollPhoneSecurity = ownerMoreViewModel::enrollPhoneSecurity,
                    onSwitchAccount = onLeaveToAccountPicker,
                    onSignOut = onLeaveToWelcome,
                    onNavigate = navigate,
                )
            }
        }
    }
}

@Composable
private fun ManagerStubSection(
    rootTag: String,
    title: String,
    displayName: String,
    businessDate: String,
    body: String,
) {
    Column(
        Modifier
            .fillMaxSize()
            .testTag(rootTag),
    ) {
        CashierScreenHeader(
            title = title,
            displayName = displayName,
            roleLabel = "Manager",
            businessDate = businessDate,
            postingOpen = true,
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ready",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Lightweight chrome-only host for M0 shell contract tests that do not wire Work/Close depth.
 */
@Composable
fun ManagerShellChromePreview(
    identity: NativeSignedInIdentity,
    snackbarHostState: SnackbarHostState,
    managerSection: ManagerSection,
    onManagerSectionChange: (ManagerSection) -> Unit,
) {
    SaafTheme(darkTheme = false) {
        BackHandler {
            when (managerSection) {
                ManagerSection.Work -> Unit
                ManagerSection.Close,
                ManagerSection.Records,
                ManagerSection.More,
                -> onManagerSectionChange(ManagerSection.Work)
            }
        }
        val businessDate = LocalDate.now().toString()
        ManagerScaffold(
            snackbarHostState = snackbarHostState,
            section = managerSection,
            onNavigate = onManagerSectionChange,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when (managerSection) {
                    ManagerSection.Work -> ManagerStubSection(
                        rootTag = NativeUiTags.ManagerWorkRoot,
                        title = "Today's work",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "Manager Work desk contract stub.",
                    )
                    ManagerSection.Close -> ManagerStubSection(
                        rootTag = NativeUiTags.ManagerCloseRoot,
                        title = "Cash & close",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "Cash & close contract stub.",
                    )
                    ManagerSection.Records -> ManagerStubSection(
                        rootTag = NativeUiTags.ManagerRecordsRoot,
                        title = "Records",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "Records contract stub.",
                    )
                    ManagerSection.More -> ManagerStubSection(
                        rootTag = NativeUiTags.ManagerMoreRoot,
                        title = "More",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "More contract stub.",
                    )
                }
            }
        }
    }
}

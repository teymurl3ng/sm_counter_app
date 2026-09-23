package com.saaf.milkshop.nativeui.admin

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
import com.saaf.milkshop.nativeui.expense.NativeExpenseState
import com.saaf.milkshop.nativeui.expense.NativeExpenseViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreViewModel
import com.saaf.milkshop.nativeui.owner.costingPlansActions
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsViewModel
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import java.time.LocalDate

/**
 * Admin-authenticated preview host (A2): Work + People + Records hub + system More.
 * Never mounts Owner/Cashier/Ops/Manager full screens or Owner-only recovery/Drive UI.
 */
@Composable
fun AdminPreviewHost(
    sessionHandle: String,
    identity: NativeSignedInIdentity,
    snackbarHostState: SnackbarHostState,
    adminSection: AdminSection,
    onAdminSectionChange: (AdminSection) -> Unit,
    adminWorkState: NativeAdminWorkState,
    adminWorkViewModel: NativeAdminWorkViewModel,
    expenseState: NativeExpenseState,
    expenseViewModel: NativeExpenseViewModel,
    ownerRecordsState: NativeOwnerRecordsState,
    ownerRecordsViewModel: NativeOwnerRecordsViewModel,
    ownerMoreState: NativeOwnerMoreState,
    ownerMoreViewModel: NativeOwnerMoreViewModel,
    onLeaveToWelcome: () -> Unit,
    onLeaveToAccountPicker: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    SaafTheme {
        var lastSession by rememberSaveable { mutableStateOf(identity.loginName) }
        var recordsDestination by rememberSaveable { mutableStateOf(AdminRecordsDestination.Hub) }

        fun closeExpense() {
            expenseViewModel.close()
        }

        fun returnAdminWorkHome() {
            closeExpense()
            adminWorkViewModel.backToHome()
        }

        fun resetRecordsHub() {
            recordsDestination = AdminRecordsDestination.Hub
            ownerRecordsViewModel.close()
        }

        LaunchedEffect(identity.loginName) {
            if (lastSession != identity.loginName) {
                onAdminSectionChange(AdminSection.Work)
                adminWorkViewModel.close()
                ownerRecordsViewModel.close()
                ownerMoreViewModel.close()
                closeExpense()
                recordsDestination = AdminRecordsDestination.Hub
            }
            lastSession = identity.loginName
        }

        LaunchedEffect(sessionHandle, adminSection) {
            when (adminSection) {
                AdminSection.Work -> {
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.close()
                    recordsDestination = AdminRecordsDestination.Hub
                    adminWorkViewModel.open(sessionHandle)
                }
                AdminSection.People -> {
                    adminWorkViewModel.close()
                    closeExpense()
                    ownerRecordsViewModel.close()
                    recordsDestination = AdminRecordsDestination.Hub
                    ownerMoreViewModel.open(sessionHandle)
                }
                AdminSection.Records -> {
                    adminWorkViewModel.close()
                    closeExpense()
                    ownerMoreViewModel.close()
                    ownerRecordsViewModel.close()
                    recordsDestination = AdminRecordsDestination.Hub
                }
                AdminSection.More -> {
                    adminWorkViewModel.close()
                    closeExpense()
                    ownerRecordsViewModel.close()
                    recordsDestination = AdminRecordsDestination.Hub
                    ownerMoreViewModel.open(sessionHandle)
                }
            }
        }

        LaunchedEffect(
            sessionHandle,
            adminSection,
            (adminWorkState as? NativeAdminWorkState.Ready)?.destination,
        ) {
            val workDestination =
                (adminWorkState as? NativeAdminWorkState.Ready)?.destination
            when {
                adminSection != AdminSection.Work -> closeExpense()
                workDestination == NativeAdminWorkDestination.RecordExpense -> {
                    expenseViewModel.open(sessionHandle)
                }
                else -> closeExpense()
            }
        }

        LaunchedEffect(sessionHandle, adminSection, recordsDestination) {
            if (adminSection != AdminSection.Records) return@LaunchedEffect
            when (recordsDestination) {
                AdminRecordsDestination.Hub -> {
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.close()
                }
                AdminRecordsDestination.Business -> {
                    ownerMoreViewModel.close()
                    ownerRecordsViewModel.open(sessionHandle)
                }
                AdminRecordsDestination.SecurityHistory -> {
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.open(sessionHandle)
                }
            }
        }

        LaunchedEffect(
            adminSection,
            recordsDestination,
            ownerMoreState,
        ) {
            if (
                adminSection == AdminSection.Records &&
                recordsDestination == AdminRecordsDestination.SecurityHistory &&
                ownerMoreState is NativeOwnerMoreState.Ready &&
                ownerMoreState.destination != NativeOwnerMoreDestination.SecurityHistory
            ) {
                ownerMoreViewModel.openSecurityHistory()
            }
        }

        val workDestination =
            (adminWorkState as? NativeAdminWorkState.Ready)?.destination
        val workEntryOwnsBack = adminSection == AdminSection.Work &&
            workDestination == NativeAdminWorkDestination.RecordExpense
        val peopleDestinationOwnsBack = adminSection == AdminSection.People &&
            (ownerMoreState as? NativeOwnerMoreState.Ready)?.destination in setOf(
                NativeOwnerMoreDestination.UsersRoles,
                NativeOwnerMoreDestination.Customers,
                NativeOwnerMoreDestination.Suppliers,
                NativeOwnerMoreDestination.Employees,
            )
        val recordsReady = ownerRecordsState as? NativeOwnerRecordsState.Ready
        val recordsDetailOwnsBack = adminSection == AdminSection.Records &&
            recordsDestination == AdminRecordsDestination.Business &&
            (recordsReady?.detail != null || recordsReady?.correction != null)
        val recordsBusinessListOwnsBack = adminSection == AdminSection.Records &&
            recordsDestination == AdminRecordsDestination.Business &&
            recordsReady?.detail == null &&
            recordsReady?.correction == null
        val recordsSecurityOwnsBack = adminSection == AdminSection.Records &&
            recordsDestination == AdminRecordsDestination.SecurityHistory
        val moreDestinationOwnsBack = adminSection == AdminSection.More &&
            (ownerMoreState as? NativeOwnerMoreState.Ready)?.destination in setOf(
                NativeOwnerMoreDestination.PersonalSecurity,
                NativeOwnerMoreDestination.Products,
                NativeOwnerMoreDestination.CostingPlans,
                NativeOwnerMoreDestination.ShopSettings,
                NativeOwnerMoreDestination.Backup,
            )

        if (
            !workEntryOwnsBack &&
            !peopleDestinationOwnsBack &&
            !recordsDetailOwnsBack &&
            !recordsBusinessListOwnsBack &&
            !recordsSecurityOwnsBack &&
            !moreDestinationOwnsBack
        ) {
            BackHandler {
                when (adminSection) {
                    AdminSection.Work -> Unit
                    AdminSection.People,
                    AdminSection.Records,
                    AdminSection.More,
                    -> onAdminSectionChange(AdminSection.Work)
                }
            }
        }

        val navigate: (AdminSection) -> Unit = { section ->
            if (section != AdminSection.Work) {
                returnAdminWorkHome()
            }
            if (section != AdminSection.Records) {
                resetRecordsHub()
            }
            if (section != AdminSection.People && section != AdminSection.More) {
                ownerMoreViewModel.close()
            } else {
                ownerMoreViewModel.backToMenu()
            }
            onAdminSectionChange(section)
        }

        val businessDate = LocalDate.now().toString()
        when {
            adminSection == AdminSection.Work &&
                workDestination == NativeAdminWorkDestination.RecordExpense -> {
                AdminExpenseEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = expenseState,
                    onRetry = expenseViewModel::refresh,
                    onRecordExpense = expenseViewModel::record,
                    onRecordAnother = expenseViewModel::recordAnother,
                    onBackToWork = { returnAdminWorkHome() },
                    onNavigate = navigate,
                )
            }
            adminSection == AdminSection.Work -> {
                AdminWorkScreen(
                    snackbarHostState = snackbarHostState,
                    state = adminWorkState,
                    onRetry = adminWorkViewModel::refresh,
                    onOpenDestination = adminWorkViewModel::openDestination,
                    onOpenPeople = { navigate(AdminSection.People) },
                    onBackToHome = { returnAdminWorkHome() },
                    onLeaveConfirmed = {
                        // Stay on Work home; session remains authenticated.
                    },
                    onNavigate = navigate,
                )
            }
            adminSection == AdminSection.People -> {
                AdminPeopleScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = ownerMoreState,
                    onRetry = ownerMoreViewModel::refresh,
                    onOpenUsersRoles = ownerMoreViewModel::openUsersRoles,
                    onOpenCustomers = ownerMoreViewModel::openCustomers,
                    onOpenSuppliers = ownerMoreViewModel::openSuppliers,
                    onOpenEmployees = ownerMoreViewModel::openEmployees,
                    onBackToMenu = ownerMoreViewModel::backToMenu,
                    onRetryUsersRoles = ownerMoreViewModel::refreshUsersRoles,
                    onOpenUser = ownerMoreViewModel::openUserDetail,
                    onBackFromUserDetail = ownerMoreViewModel::clearUserDetail,
                    onOpenUserEnroll = ownerMoreViewModel::openUserEnroll,
                    onBackFromUserEnroll = ownerMoreViewModel::clearUserEnroll,
                    onBeginUserEnroll = ownerMoreViewModel::beginUserEnroll,
                    onCompleteUserEnroll = ownerMoreViewModel::completeUserEnroll,
                    onOpenUserRoleChange = ownerMoreViewModel::openUserRoleChange,
                    onBackFromUserRoleChange = ownerMoreViewModel::clearUserRoleChange,
                    onChangeUserRole = ownerMoreViewModel::changeUserRole,
                    onRequestSetUserEnabled = ownerMoreViewModel::requestSetUserEnabled,
                    onConfirmSetUserEnabled = ownerMoreViewModel::confirmSetUserEnabled,
                    onCancelSetUserEnabled = ownerMoreViewModel::cancelSetUserEnabled,
                    onSubmitUsersRolesReauth = ownerMoreViewModel::submitUsersRolesReauth,
                    onCancelUsersRolesReauth = ownerMoreViewModel::cancelUsersRolesReauth,
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
                    onNavigate = navigate,
                )
            }
            adminSection == AdminSection.Records -> {
                AdminRecordsScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    destination = recordsDestination,
                    ownerRecordsState = ownerRecordsState,
                    ownerMoreState = ownerMoreState,
                    onOpenBusiness = { recordsDestination = AdminRecordsDestination.Business },
                    onOpenSecurityHistory = {
                        recordsDestination = AdminRecordsDestination.SecurityHistory
                    },
                    onBackToHub = { resetRecordsHub() },
                    onRetryRecords = ownerRecordsViewModel::refresh,
                    onSearchQueryChange = ownerRecordsViewModel::setSearchQuery,
                    onOpenRecord = ownerRecordsViewModel::openDetail,
                    onBackFromRecordDetail = ownerRecordsViewModel::clearDetail,
                    onRetrySecurityHistory = ownerMoreViewModel::refreshSecurityHistory,
                    onOpenSecurityAudit = ownerMoreViewModel::openSecurityAuditDetail,
                    onBackFromSecurityAuditDetail = ownerMoreViewModel::clearSecurityAuditDetail,
                    onLeaveConfirmed = { onAdminSectionChange(AdminSection.Work) },
                    onDeferredGroup = { group ->
                        onShowNotice(
                            "$group comes next in the Admin Records family.",
                        )
                    },
                    onSelectGroup = ownerRecordsViewModel::setActiveGroup,
                    onOpenCorrection = ownerRecordsViewModel::openCorrection,
                    onBackFromCorrection = ownerRecordsViewModel::clearCorrection,
                    onUpdateCorrection = ownerRecordsViewModel::updateCorrection,
                    onSaveCorrection = ownerRecordsViewModel::saveCorrection,
                    onNavigate = navigate,
                )
            }
            else -> {
                AdminMoreScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = ownerMoreState,
                    onRetry = ownerMoreViewModel::refresh,
                    onOpenPersonalSecurity = ownerMoreViewModel::openPersonalSecurity,
                    onOpenProducts = ownerMoreViewModel::openProducts,
                    onOpenCostingPlans = ownerMoreViewModel::openCostingPlans,
                    onOpenShopSettings = ownerMoreViewModel::openShopSettings,
                    onOpenBackup = ownerMoreViewModel::openBackup,
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
                    costingActions = ownerMoreViewModel.costingPlansActions(),
                    onRetryShopSettings = ownerMoreViewModel::refreshShopSettings,
                    onOpenShopIdentityEdit = ownerMoreViewModel::openShopIdentityEdit,
                    onBackFromShopIdentityEdit = ownerMoreViewModel::clearShopIdentityEdit,
                    onSaveShopIdentity = ownerMoreViewModel::saveShopIdentity,
                    onOpenShopRatesEdit = ownerMoreViewModel::openShopRatesEdit,
                    onBackFromShopRatesEdit = ownerMoreViewModel::clearShopRatesEdit,
                    onUpdateShopRates = ownerMoreViewModel::updateShopRates,
                    onOpenShopCategoryCreate = ownerMoreViewModel::openShopCategoryCreate,
                    onBackFromShopCategoryCreate = ownerMoreViewModel::clearShopCategoryCreate,
                    onCreateShopExpenseCategory = ownerMoreViewModel::createShopExpenseCategory,
                    onOpenShopCategory = ownerMoreViewModel::openShopCategoryDetail,
                    onBackFromShopCategoryDetail = ownerMoreViewModel::clearShopCategoryDetail,
                    onSetShopExpenseCategoryArchived = ownerMoreViewModel::setShopExpenseCategoryArchived,
                    onRetryBackup = ownerMoreViewModel::refreshBackup,
                    onOpenBackupExport = ownerMoreViewModel::openBackupExport,
                    onBackFromBackupExport = ownerMoreViewModel::clearBackupExport,
                    onExportBackup = ownerMoreViewModel::exportBackup,
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
private fun AdminStubSection(
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
            roleLabel = "Admin",
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
 * Lightweight chrome-only host for Admin shell contract tests that do not wire Work/People depth.
 */
@Composable
fun AdminShellChromePreview(
    identity: NativeSignedInIdentity,
    snackbarHostState: SnackbarHostState,
    adminSection: AdminSection,
    onAdminSectionChange: (AdminSection) -> Unit,
) {
    SaafTheme {
        BackHandler {
            when (adminSection) {
                AdminSection.Work -> Unit
                AdminSection.People,
                AdminSection.Records,
                AdminSection.More,
                -> onAdminSectionChange(AdminSection.Work)
            }
        }
        val businessDate = LocalDate.now().toString()
        AdminScaffold(
            snackbarHostState = snackbarHostState,
            section = adminSection,
            onNavigate = onAdminSectionChange,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when (adminSection) {
                    AdminSection.Work -> AdminStubSection(
                        rootTag = NativeUiTags.AdminWorkRoot,
                        title = "Work desk",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "Admin Work desk contract stub.",
                    )
                    AdminSection.People -> AdminStubSection(
                        rootTag = NativeUiTags.AdminPeopleRoot,
                        title = "People",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "Admin People contract stub.",
                    )
                    AdminSection.Records -> AdminStubSection(
                        rootTag = NativeUiTags.AdminRecordsRoot,
                        title = "Records",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "Admin Records contract stub.",
                    )
                    AdminSection.More -> AdminStubSection(
                        rootTag = NativeUiTags.AdminMoreRoot,
                        title = "More",
                        displayName = identity.displayName,
                        businessDate = businessDate,
                        body = "Admin More contract stub.",
                    )
                }
            }
        }
    }
}

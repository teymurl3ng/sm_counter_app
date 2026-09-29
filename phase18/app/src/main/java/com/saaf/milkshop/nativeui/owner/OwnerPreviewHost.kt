package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.saaf.milkshop.application.activity.NativeActivityFacade
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.preview.NativePreviewSampleData
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.collection.NativeCollectionViewModel
import com.saaf.milkshop.nativeui.expense.NativeExpenseState
import com.saaf.milkshop.nativeui.expense.NativeExpenseViewModel
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseViewModel
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesViewModel
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtState
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtViewModel
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkState
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkViewModel
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageState
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageViewModel
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.sale.NativeSaleViewModel
import com.saaf.milkshop.nativeui.salarypayment.NativeSalaryPaymentState
import com.saaf.milkshop.nativeui.salarypayment.NativeSalaryPaymentViewModel
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentViewModel
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesDraftActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owner-authenticated preview host: Overview/Work shells plus Owner*EntryScreen
 * destinations. Role-neutral entry content stays behind Owner wrappers — never
 * remounts Cashier/Ops screens with foreign bottom nav.
 */
@Composable
fun OwnerPreviewHost(
    sessionHandle: String,
    identity: NativeSignedInIdentity,
    database: MilkShopDatabase,
    security: UserSecurityService,
    saleFacade: NativeSaleFacade,
    collectionFacade: NativeCollectionFacade,
    activityFacade: NativeActivityFacade,
    snackbarHostState: SnackbarHostState,
    ownerSection: OwnerSection,
    onOwnerSectionChange: (OwnerSection) -> Unit,
    ownerOverviewState: NativeOwnerOverviewState,
    ownerOverviewViewModel: NativeOwnerOverviewViewModel,
    ownerReportsState: NativeOwnerReportsState,
    ownerReportsViewModel: NativeOwnerReportsViewModel,
    ownerWorkState: NativeOwnerWorkState,
    ownerWorkViewModel: NativeOwnerWorkViewModel,
    ownerRecordsState: NativeOwnerRecordsState,
    ownerRecordsViewModel: NativeOwnerRecordsViewModel,
    ownerMoreState: NativeOwnerMoreState,
    ownerMoreViewModel: NativeOwnerMoreViewModel,
    saleState: NativeSaleState,
    saleViewModel: NativeSaleViewModel,
    collectionState: NativeCollectionState,
    collectionViewModel: NativeCollectionViewModel,
    expenseState: NativeExpenseState,
    expenseViewModel: NativeExpenseViewModel,
    vendorPaymentState: NativeVendorPaymentState,
    vendorPaymentViewModel: NativeVendorPaymentViewModel,
    salaryPaymentState: NativeSalaryPaymentState,
    salaryPaymentViewModel: NativeSalaryPaymentViewModel,
    cashCloseState: NativeCashCloseState,
    cashCloseViewModel: NativeCashCloseViewModel,
    receiveMilkState: NativeReceiveMilkState,
    receiveMilkViewModel: NativeReceiveMilkViewModel,
    makeYogurtState: NativeMakeYogurtState,
    makeYogurtViewModel: NativeMakeYogurtViewModel,
    recordSpoilageState: NativeRecordSpoilageState,
    recordSpoilageViewModel: NativeRecordSpoilageViewModel,
    deliveriesState: NativeDeliveriesState,
    deliveriesViewModel: NativeDeliveriesViewModel,
    onLeaveToWelcome: () -> Unit,
    onLeaveToAccountPicker: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    SaafTheme {
        var lastOwnerSession by rememberSaveable { mutableStateOf<String?>(null) }
        var ownerSampleReady by remember(sessionHandle) { mutableStateOf(false) }

        fun closeOwnerWorkEntries() {
            saleViewModel.close()
            collectionViewModel.close()
            expenseViewModel.close()
            vendorPaymentViewModel.close()
            salaryPaymentViewModel.close()
            cashCloseViewModel.close()
            receiveMilkViewModel.close()
            makeYogurtViewModel.close()
            recordSpoilageViewModel.close()
            deliveriesViewModel.close()
        }

        fun returnOwnerWorkHome() {
            // Entry VMs stay warm for instant re-entry; only the Work menu
            // itself returns to its home tile list.
            ownerWorkViewModel.backToHome()
        }

        LaunchedEffect(sessionHandle) {
            if (lastOwnerSession != null && lastOwnerSession != sessionHandle) {
                onOwnerSectionChange(OwnerSection.Overview)
                ownerWorkViewModel.close()
                ownerRecordsViewModel.close()
                ownerMoreViewModel.close()
                ownerReportsViewModel.close()
                closeOwnerWorkEntries()
            }
            lastOwnerSession = sessionHandle
            ownerSampleReady = false
            val sample = runCatching {
                withContext(Dispatchers.IO) {
                    NativePreviewSampleData.ensureForOwnerOverview(
                        database = database,
                        security = security,
                        sessionHandle = sessionHandle,
                    )
                }
            }.getOrElse { error ->
                snackbarHostState.showSnackbar(
                    message = "Sample data failed: ${error.message ?: "unknown error"}",
                    duration = SnackbarDuration.Long,
                )
                null
            }
            ownerSampleReady = true
            when {
                sample == null -> Unit
                sample.errors.isNotEmpty() -> {
                    snackbarHostState.showSnackbar(
                        message = "Sample data partial: ${sample.errors.first()}",
                        duration = SnackbarDuration.Long,
                    )
                }
                sample.changed -> {
                    val message = if (sample.seededStaffAccounts > 0) {
                        NativePreviewSampleData.previewAccountsHint()
                    } else {
                        "Sample shop setup ready — products, customers, and accounts."
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = if (sample.seededStaffAccounts > 0) {
                            SnackbarDuration.Long
                        } else {
                            SnackbarDuration.Short
                        },
                    )
                }
            }
        }
        LaunchedEffect(
            sessionHandle,
            ownerSampleReady,
            ownerSection,
        ) {
            if (!ownerSampleReady) return@LaunchedEffect
            when (ownerSection) {
                OwnerSection.Overview -> {
                    if (ownerReportsState is NativeOwnerReportsState.Idle) {
                        ownerOverviewViewModel.open(sessionHandle)
                    }
                }
                OwnerSection.Work -> ownerWorkViewModel.open(sessionHandle)
                OwnerSection.Records -> ownerRecordsViewModel.open(sessionHandle)
                OwnerSection.More -> ownerMoreViewModel.open(sessionHandle)
            }
        }
        // Work entries stay warm across switches: closing siblings here forced
        // a full cold reload on every Work → back → Work round-trip. open()
        // re-validates cheaply (thin refresh bar) and close() is reserved for
        // session changes.
        LaunchedEffect(
            sessionHandle,
            ownerSection,
            (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination,
        ) {
            val workDestination =
                (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination
            if (ownerSection == OwnerSection.Work) {
                when (workDestination) {
                    NativeOwnerWorkDestination.RecordSale -> saleViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.RecordPayment -> collectionViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.RecordExpense -> expenseViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.PaySupplier -> vendorPaymentViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.PaySalary -> salaryPaymentViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.CashCountAndClose -> cashCloseViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.ReceiveMilk -> receiveMilkViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.MakeYogurt -> makeYogurtViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.RecordSpoilage -> recordSpoilageViewModel.open(sessionHandle)
                    NativeOwnerWorkDestination.Deliveries -> deliveriesViewModel.open(sessionHandle)
                    else -> Unit
                }
            }
        }
        val ownerReportsOwnsBack =
            ownerSection == OwnerSection.Overview &&
                ownerReportsState !is NativeOwnerReportsState.Idle
        val ownerWorkDestination =
            (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination
        val ownerEntryOwnsBack = ownerSection == OwnerSection.Work &&
            ownerWorkDestination in setOf(
                NativeOwnerWorkDestination.RecordSale,
                NativeOwnerWorkDestination.RecordPayment,
                NativeOwnerWorkDestination.RecordExpense,
                NativeOwnerWorkDestination.PaySupplier,
                NativeOwnerWorkDestination.PaySalary,
                NativeOwnerWorkDestination.CashCountAndClose,
                NativeOwnerWorkDestination.ReceiveMilk,
                NativeOwnerWorkDestination.MakeYogurt,
                NativeOwnerWorkDestination.RecordSpoilage,
                NativeOwnerWorkDestination.Deliveries,
            )
        val ownerRecordsReady = ownerRecordsState as? NativeOwnerRecordsState.Ready
        val ownerRecordsOwnsBack =
            ownerSection == OwnerSection.Records &&
                (ownerRecordsReady?.detail != null || ownerRecordsReady?.correction != null)
        val ownerMoreOwnsBack =
            ownerSection == OwnerSection.More &&
                (ownerMoreState as? NativeOwnerMoreState.Ready)?.destination in setOf(
                    NativeOwnerMoreDestination.PersonalSecurity,
                    NativeOwnerMoreDestination.Products,
                    NativeOwnerMoreDestination.Customers,
                    NativeOwnerMoreDestination.Suppliers,
                    NativeOwnerMoreDestination.Employees,
                    NativeOwnerMoreDestination.CostingPlans,
                    NativeOwnerMoreDestination.ShopSettings,
                    NativeOwnerMoreDestination.UsersRoles,
                    NativeOwnerMoreDestination.SecurityHistory,
                    NativeOwnerMoreDestination.Backup,
                )
        BackHandler(
            enabled = !ownerEntryOwnsBack &&
                !ownerRecordsOwnsBack &&
                !ownerMoreOwnsBack &&
                !ownerReportsOwnsBack,
        ) {
            when (ownerSection) {
                OwnerSection.Overview -> onLeaveToWelcome()
                OwnerSection.Work -> {
                    val ready = ownerWorkState as? NativeOwnerWorkState.Ready
                    if (ready != null &&
                        ready.destination != NativeOwnerWorkDestination.Home
                    ) {
                        returnOwnerWorkHome()
                    } else {
                        onLeaveToWelcome()
                    }
                }
                OwnerSection.Records -> {
                    val ready = ownerRecordsState as? NativeOwnerRecordsState.Ready
                    when {
                        ready?.correction != null -> ownerRecordsViewModel.clearCorrection()
                        ready?.detail != null -> ownerRecordsViewModel.clearDetail()
                        else -> onLeaveToWelcome()
                    }
                }
                OwnerSection.More -> {
                    val ready = ownerMoreState as? NativeOwnerMoreState.Ready
                    when (ready?.destination) {
                        NativeOwnerMoreDestination.PersonalSecurity ->
                            ownerMoreViewModel.backToMenu()
                        NativeOwnerMoreDestination.Products -> {
                            if (ready.productDetail != null) {
                                ownerMoreViewModel.clearProductDetail()
                            } else {
                                ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.Customers -> {
                            if (ready.customerDetail != null) {
                                ownerMoreViewModel.clearCustomerDetail()
                            } else {
                                ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.Suppliers -> {
                            if (ready.supplierDetail != null) {
                                ownerMoreViewModel.clearSupplierDetail()
                            } else {
                                ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.Employees -> {
                            if (ready.employeeDetail != null) {
                                ownerMoreViewModel.clearEmployeeDetail()
                            } else {
                                ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.CostingPlans -> {
                            when {
                                ready.costingReauthOpen ->
                                    ownerMoreViewModel.cancelCostingReauth()
                                ready.costingApproveOpen ->
                                    ownerMoreViewModel.clearCostingApprove()
                                ready.costingYieldApproveOpen ->
                                    ownerMoreViewModel.clearCostingYieldApprove()
                                ready.costingOpeningEditor != null ->
                                    ownerMoreViewModel.clearCostingOpeningEditor()
                                ready.costingBudgetEditorOpen ->
                                    ownerMoreViewModel.clearCostingBudgetEditor()
                                ready.costingCollectionsEditorOpen ->
                                    ownerMoreViewModel.clearCostingCollectionsEditor()
                                ready.costingPolicyDetail != null ->
                                    ownerMoreViewModel.clearCostingPolicyDetail()
                                else -> ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.ShopSettings -> {
                            when {
                                ready.shopSettingsIdentityEditOpen ->
                                    ownerMoreViewModel.clearShopIdentityEdit()
                                ready.shopSettingsRatesEditOpen ->
                                    ownerMoreViewModel.clearShopRatesEdit()
                                ready.shopSettingsCategoryCreateOpen ->
                                    ownerMoreViewModel.clearShopCategoryCreate()
                                ready.shopSettingsCategoryDetail != null ->
                                    ownerMoreViewModel.clearShopCategoryDetail()
                                else -> ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.UsersRoles -> {
                            when {
                                ready.userReauthOpen ->
                                    ownerMoreViewModel.cancelUsersRolesReauth()
                                ready.userEnrollOpen ->
                                    ownerMoreViewModel.clearUserEnroll()
                                ready.userRoleChangeOpen ->
                                    ownerMoreViewModel.clearUserRoleChange()
                                ready.userDetail != null ->
                                    ownerMoreViewModel.clearUserDetail()
                                else -> ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.SecurityHistory -> {
                            if (ready.securityAuditDetail != null) {
                                ownerMoreViewModel.clearSecurityAuditDetail()
                            } else {
                                ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.Backup ->
                            when {
                                ready.backupReauthOpen ->
                                    ownerMoreViewModel.cancelBackupReauth()
                                ready.backupExportOpen ->
                                    ownerMoreViewModel.clearBackupExport()
                                ready.backupRestoreOpen ->
                                    ownerMoreViewModel.clearBackupRestore()
                                ready.backupConnectOpen ->
                                    ownerMoreViewModel.clearBackupConnect()
                                ready.backupDisconnectConfirmOpen ->
                                    ownerMoreViewModel.cancelBackupDisconnect()
                                else -> ownerMoreViewModel.backToMenu()
                            }
                        else -> onLeaveToWelcome()
                    }
                }
            }
        }
        val navigateOwner: (OwnerSection) -> Unit = { section ->
            if (ownerSection == OwnerSection.Work && section != OwnerSection.Work) {
                returnOwnerWorkHome()
            }
            when (section) {
                OwnerSection.Overview -> {
                    if (ownerSection == OwnerSection.Overview) {
                        if (ownerReportsState !is NativeOwnerReportsState.Idle) {
                            ownerReportsViewModel.close()
                        } else {
                            onShowNotice("Overview is already open.")
                        }
                    } else {
                        onOwnerSectionChange(OwnerSection.Overview)
                    }
                }
                OwnerSection.Work -> {
                    if (ownerSection == OwnerSection.Work &&
                        ownerWorkDestination == NativeOwnerWorkDestination.Home
                    ) {
                        onShowNotice("Work is already open.")
                    } else {
                        if (ownerSection == OwnerSection.Work) {
                            returnOwnerWorkHome()
                        }
                        onOwnerSectionChange(OwnerSection.Work)
                    }
                }
                OwnerSection.Records -> {
                    val recordsReady = ownerRecordsState as? NativeOwnerRecordsState.Ready
                    if (ownerSection == OwnerSection.Records &&
                        recordsReady?.detail == null &&
                        recordsReady?.correction == null
                    ) {
                        onShowNotice("Records is already open.")
                    } else {
                        if (ownerSection == OwnerSection.Records) {
                            ownerRecordsViewModel.clearDetail()
                        }
                        onOwnerSectionChange(OwnerSection.Records)
                    }
                }
                OwnerSection.More -> {
                    if (ownerSection == OwnerSection.More &&
                        (ownerMoreState as? NativeOwnerMoreState.Ready)?.destination ==
                        NativeOwnerMoreDestination.Menu
                    ) {
                        onShowNotice("More is already open.")
                    } else {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        if (ownerSection == OwnerSection.More) {
                            ownerMoreViewModel.backToMenu()
                        }
                        onOwnerSectionChange(OwnerSection.More)
                    }
                }
            }
        }
        val sectionStateHolder = rememberSaveableStateHolder()
        NativeSectionTransition<Pair<OwnerSection, NativeOwnerWorkDestination?>>(
            targetState = ownerSection to ownerWorkDestination,
            contentKey = { it },
        ) { pane ->
            val (section, paneDestination) = pane
        sectionStateHolder.SaveableStateProvider("$sessionHandle:${section.name}:$paneDestination") {
        when {
            section == OwnerSection.More -> {
                OwnerMoreScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = ownerMoreState,
                    onRetry = ownerMoreViewModel::refresh,
                    onOpenPersonalSecurity = ownerMoreViewModel::openPersonalSecurity,
                    onOpenProducts = ownerMoreViewModel::openProducts,
                    onOpenCustomers = ownerMoreViewModel::openCustomers,
                    onOpenSuppliers = ownerMoreViewModel::openSuppliers,
                    onOpenEmployees = ownerMoreViewModel::openEmployees,
                    onOpenCostingPlans = ownerMoreViewModel::openCostingPlans,
                    onOpenShopSettings = ownerMoreViewModel::openShopSettings,
                    onOpenUsersRoles = ownerMoreViewModel::openUsersRoles,
                    onOpenSecurityHistory = ownerMoreViewModel::openSecurityHistory,
                    onOpenBackup = ownerMoreViewModel::openBackup,
                    onOpenSync = ownerMoreViewModel::openSync,
                    onRefreshSync = ownerMoreViewModel::refreshSync,
                    onEnrollSync = ownerMoreViewModel::enrollSync,
                    onEnrollSyncInShop = ownerMoreViewModel::enrollSyncInShop,
                    onSyncNow = ownerMoreViewModel::syncNow,
                    onUnenrollSync = ownerMoreViewModel::unenrollSync,
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
                    onOpenOpeningEdit = ownerMoreViewModel::openOpeningEdit,
                    onBackFromOpeningEdit = ownerMoreViewModel::backFromOpeningEdit,
                    onSaveOpening = ownerMoreViewModel::saveOpening,
                    onSubmitOpeningReauth = ownerMoreViewModel::submitOpeningReauth,
                    onCancelOpeningReauth = ownerMoreViewModel::cancelOpeningReauth,
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
                    onBeginCredentialReset = ownerMoreViewModel::beginCredentialReset,
                    onResumeCredentialSetup = ownerMoreViewModel::resumeCredentialSetup,
                    onSubmitUsersRolesReauth = ownerMoreViewModel::submitUsersRolesReauth,
                    onCancelUsersRolesReauth = ownerMoreViewModel::cancelUsersRolesReauth,
                    onRetrySecurityHistory = ownerMoreViewModel::refreshSecurityHistory,
                    onOpenSecurityAudit = ownerMoreViewModel::openSecurityAuditDetail,
                    onBackFromSecurityAuditDetail = ownerMoreViewModel::clearSecurityAuditDetail,
                    onRetryBackup = ownerMoreViewModel::refreshBackup,
                    onOpenBackupExport = ownerMoreViewModel::openBackupExport,
                    onBackFromBackupExport = ownerMoreViewModel::clearBackupExport,
                    onExportBackup = ownerMoreViewModel::exportBackup,
                    onOpenBackupRestore = ownerMoreViewModel::openBackupRestore,
                    onBackFromBackupRestore = ownerMoreViewModel::clearBackupRestore,
                    onInspectBackupRestore = ownerMoreViewModel::inspectBackupRestore,
                    onConfirmBackupRestore = ownerMoreViewModel::confirmBackupRestore,
                    onOpenBackupConnect = ownerMoreViewModel::openBackupConnect,
                    onBackFromBackupConnect = ownerMoreViewModel::clearBackupConnect,
                    onConnectDriveBackup = ownerMoreViewModel::connectDriveBackup,
                    onSetDriveBackupEnabled = ownerMoreViewModel::setDriveBackupEnabled,
                    onRunDriveBackupNow = ownerMoreViewModel::runDriveBackupNow,
                    onRequestBackupDisconnect = ownerMoreViewModel::requestBackupDisconnect,
                    onConfirmBackupDisconnect = ownerMoreViewModel::confirmBackupDisconnect,
                    onCancelBackupDisconnect = ownerMoreViewModel::cancelBackupDisconnect,
                    onSubmitBackupReauth = ownerMoreViewModel::submitBackupReauth,
                    onCancelBackupReauth = ownerMoreViewModel::cancelBackupReauth,
                    onOpenBackupCloud = ownerMoreViewModel::openBackupCloud,
                    onBackFromBackupCloud = ownerMoreViewModel::backFromBackupCloud,
                    onRefreshCloudBackups = ownerMoreViewModel::refreshCloudBackups,
                    onInspectCloudBackup = ownerMoreViewModel::inspectCloudBackup,
                    onConfirmCloudBackupRestore = ownerMoreViewModel::confirmCloudBackupRestore,
                    onRequestRemoveQuickUnlock = ownerMoreViewModel::requestRemoveQuickUnlock,
                    onCancelRemoveQuickUnlock = ownerMoreViewModel::cancelRemoveQuickUnlock,
                    onRemoveQuickUnlock = ownerMoreViewModel::removeQuickUnlock,
                    onEnrollQuickPin = ownerMoreViewModel::enrollQuickPin,
                    onEnrollPhoneSecurity = ownerMoreViewModel::enrollPhoneSecurity,
                    onSwitchAccount = onLeaveToAccountPicker,
                    onSignOut = onLeaveToWelcome,
                    onNavigate = navigateOwner,
                    onLeaveConfirmed = onLeaveToWelcome,
                )
            }
            section == OwnerSection.Records -> {
                OwnerRecordsScreen(
                    snackbarHostState = snackbarHostState,
                    state = ownerRecordsState,
                    onRetry = ownerRecordsViewModel::refresh,
                    onSearchQueryChange = ownerRecordsViewModel::setSearchQuery,
                    onOpenRecord = ownerRecordsViewModel::openDetail,
                    onBackFromDetail = ownerRecordsViewModel::clearDetail,
                    onOpenLedger = ownerRecordsViewModel::openLedger,
                    onBackFromLedger = ownerRecordsViewModel::clearLedger,
                    onLeaveConfirmed = onLeaveToWelcome,
                    hostedScaffold = { snackbar, mod, wrapNav, content ->
                        OwnerScaffold(
                            snackbarHostState = snackbar,
                            section = OwnerSection.Records,
                            onNavigate = { section -> wrapNav { navigateOwner(section) } },
                            modifier = mod,
                            content = content,
                        )
                    },
                    roleLabel = "Owner",
                    onSelectGroup = ownerRecordsViewModel::setActiveGroup,
                    onOpenCorrection = ownerRecordsViewModel::openCorrection,
                    onBackFromCorrection = ownerRecordsViewModel::clearCorrection,
                    onUpdateCorrection = ownerRecordsViewModel::updateCorrection,
                    onSaveCorrection = ownerRecordsViewModel::saveCorrection,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.RecordSale -> {
                OwnerSaleEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = saleState,
                    onRetry = saleViewModel::refresh,
                    onClearFeedback = saleViewModel::clearFeedback,
                    onRecordSale = saleViewModel::record,
                    onSendAsDelivery = saleViewModel::sendAsDelivery,
                    onStartNextSale = saleViewModel::startNextSale,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.RecordPayment -> {
                OwnerCollectionEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = collectionState,
                    onRetry = collectionViewModel::refresh,
                    onSelectCustomer = collectionViewModel::selectCustomer,
                    onRecordCollection = collectionViewModel::record,
                    onCollectAnother = collectionViewModel::collectAnother,
                    onViewRecord = {
                        onShowNotice(
                            "Owner Records comes next. Payment stays available from Work.",
                        )
                    },
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.RecordExpense -> {
                OwnerExpenseEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = expenseState,
                    onRetry = expenseViewModel::refresh,
                    onRecordExpense = expenseViewModel::record,
                    onRecordAnother = expenseViewModel::recordAnother,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.PaySupplier -> {
                OwnerPaySupplierEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = vendorPaymentState,
                    onRetry = vendorPaymentViewModel::refresh,
                    onRecordPayment = vendorPaymentViewModel::record,
                    onRecordAnother = vendorPaymentViewModel::recordAnother,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.PaySalary -> {
                OwnerPaySalaryEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = salaryPaymentState,
                    onRetry = salaryPaymentViewModel::refresh,
                    onRecordPayment = salaryPaymentViewModel::record,
                    onRecordAnother = salaryPaymentViewModel::recordAnother,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.CashCountAndClose -> {
                OwnerCashCloseEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = cashCloseState,
                    onRetry = cashCloseViewModel::refresh,
                    onRecordCount = cashCloseViewModel::recordCount,
                    onCloseDay = cashCloseViewModel::closeDay,
                    onReopenDay = cashCloseViewModel::reopenDay,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.ReceiveMilk -> {
                OwnerReceiveMilkEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = receiveMilkState,
                    onRetry = receiveMilkViewModel::refresh,
                    onSelectVendor = receiveMilkViewModel::selectVendor,
                    onOpenVendorPicker = receiveMilkViewModel::openVendorPicker,
                    onCloseVendorPicker = receiveMilkViewModel::closeVendorPicker,
                    onPreview = receiveMilkViewModel::preview,
                    onRecord = receiveMilkViewModel::record,
                    onReceiveAnother = receiveMilkViewModel::receiveAnother,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.MakeYogurt -> {
                OwnerMakeYogurtEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = makeYogurtState,
                    onRetry = makeYogurtViewModel::refresh,
                    onRecord = makeYogurtViewModel::record,
                    onMakeAnother = makeYogurtViewModel::makeAnother,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.RecordSpoilage -> {
                OwnerRecordSpoilageEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = recordSpoilageState,
                    onRetry = recordSpoilageViewModel::refresh,
                    onSelectProduct = recordSpoilageViewModel::selectProduct,
                    onOpenProductPicker = recordSpoilageViewModel::openProductPicker,
                    onCloseProductPicker = recordSpoilageViewModel::closeProductPicker,
                    onSelectReason = recordSpoilageViewModel::selectReason,
                    onOpenReasonPicker = recordSpoilageViewModel::openReasonPicker,
                    onCloseReasonPicker = recordSpoilageViewModel::closeReasonPicker,
                    onRecord = recordSpoilageViewModel::record,
                    onRecordAnother = recordSpoilageViewModel::recordAnother,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.Deliveries -> {
                OwnerDeliveriesEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = deliveriesState,
                    onRetry = deliveriesViewModel::refresh,
                    onMarkDelivered = deliveriesViewModel::markDelivered,
                    draftActions = NativeDeliveriesDraftActions(
                        selectCustomer = deliveriesViewModel::selectDraftCustomer,
                        selectProduct = deliveriesViewModel::selectDraftProduct,
                        setQuantity = deliveriesViewModel::updateDraftQuantity,
                        setRoute = deliveriesViewModel::updateDraftRoute,
                        setNotes = deliveriesViewModel::updateDraftNotes,
                        create = deliveriesViewModel::createDelivery,
                    ),
                    onArchive = deliveriesViewModel::archiveDelivery,
                        onLinkSale = deliveriesViewModel::linkSale,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            section == OwnerSection.Work -> {
                OwnerWorkScreen(
                    snackbarHostState = snackbarHostState,
                    state = ownerWorkState,
                    onRetry = ownerWorkViewModel::refresh,
                    onOpenDestination = ownerWorkViewModel::openDestination,
                    onBackToHome = { returnOwnerWorkHome() },
                    onLeaveConfirmed = onLeaveToWelcome,
                    onNavigate = navigateOwner,
                )
            }
            else -> {
                if (ownerReportsState !is NativeOwnerReportsState.Idle) {
                    OwnerReportsScreen(
                        snackbarHostState = snackbarHostState,
                        identity = identity,
                        state = ownerReportsState,
                        onBackToOverview = ownerReportsViewModel::close,
                        onRetry = ownerReportsViewModel::refresh,
                        onSelectPreset = ownerReportsViewModel::selectPreset,
                        onCustomRangePreview = {},
                        onSelectCustomRange = ownerReportsViewModel::selectCustomRange,
                        onNavigate = navigateOwner,
                    )
                } else {
                    OwnerOverviewScreen(
                        snackbarHostState = snackbarHostState,
                        state = ownerOverviewState,
                        section = ownerSection,
                        onRetry = ownerOverviewViewModel::refresh,
                        onLeaveConfirmed = onLeaveToWelcome,
                        onNavigate = navigateOwner,
                        onAttentionReview = {
                            onOwnerSectionChange(OwnerSection.Work)
                            ownerWorkViewModel.openDestination(NativeOwnerWorkDestination.CashCountAndClose)
                        },
                        onOpenCashCount = {
                            onOwnerSectionChange(OwnerSection.Work)
                            ownerWorkViewModel.openDestination(NativeOwnerWorkDestination.CashCountAndClose)
                        },
                        onOpenMonthReports = { ownerReportsViewModel.open(sessionHandle) },
                    )
                }
            }
        }
        }
        }
    }
}

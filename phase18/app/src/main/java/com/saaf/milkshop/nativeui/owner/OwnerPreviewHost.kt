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
import androidx.compose.runtime.setValue
import com.saaf.milkshop.application.activity.NativeActivityFacade
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
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentViewModel
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
    SaafTheme(darkTheme = false) {
        var lastOwnerSession by rememberSaveable { mutableStateOf<String?>(null) }
        var ownerSampleReady by remember(sessionHandle) { mutableStateOf(false) }

        fun closeOwnerWorkEntries() {
            saleViewModel.close()
            collectionViewModel.close()
            expenseViewModel.close()
            vendorPaymentViewModel.close()
            receiveMilkViewModel.close()
            makeYogurtViewModel.close()
            recordSpoilageViewModel.close()
            deliveriesViewModel.close()
        }

        fun returnOwnerWorkHome() {
            closeOwnerWorkEntries()
            ownerWorkViewModel.backToHome()
        }

        LaunchedEffect(sessionHandle) {
            if (lastOwnerSession != null && lastOwnerSession != sessionHandle) {
                onOwnerSectionChange(OwnerSection.Overview)
                ownerWorkViewModel.close()
                ownerRecordsViewModel.close()
                ownerMoreViewModel.close()
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
                        saleFacade = saleFacade,
                        collectionFacade = collectionFacade,
                        activityFacade = activityFacade,
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
                        "Sample shop data ready for Owner Overview."
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
                    closeOwnerWorkEntries()
                    ownerWorkViewModel.close()
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.close()
                    ownerOverviewViewModel.close()
                    ownerOverviewViewModel.open(sessionHandle)
                    ownerOverviewViewModel.refresh()
                }
                OwnerSection.Work -> {
                    ownerOverviewViewModel.close()
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.close()
                    ownerWorkViewModel.open(sessionHandle)
                }
                OwnerSection.Records -> {
                    closeOwnerWorkEntries()
                    ownerWorkViewModel.close()
                    ownerOverviewViewModel.close()
                    ownerMoreViewModel.close()
                    ownerRecordsViewModel.open(sessionHandle)
                }
                OwnerSection.More -> {
                    closeOwnerWorkEntries()
                    ownerWorkViewModel.close()
                    ownerOverviewViewModel.close()
                    ownerRecordsViewModel.close()
                    ownerMoreViewModel.open(sessionHandle)
                }
            }
        }
        LaunchedEffect(
            sessionHandle,
            ownerSection,
            (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination,
        ) {
            val workDestination =
                (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination
            when {
                ownerSection != OwnerSection.Work -> closeOwnerWorkEntries()
                workDestination == NativeOwnerWorkDestination.RecordSale -> {
                    collectionViewModel.close()
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    saleViewModel.open(sessionHandle)
                }
                workDestination == NativeOwnerWorkDestination.RecordPayment -> {
                    saleViewModel.close()
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    collectionViewModel.open(sessionHandle)
                }
                workDestination == NativeOwnerWorkDestination.RecordExpense -> {
                    saleViewModel.close()
                    collectionViewModel.close()
                    vendorPaymentViewModel.close()
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    expenseViewModel.open(sessionHandle)
                }
                workDestination == NativeOwnerWorkDestination.PaySupplier -> {
                    saleViewModel.close()
                    collectionViewModel.close()
                    expenseViewModel.close()
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    vendorPaymentViewModel.open(sessionHandle)
                }
                workDestination == NativeOwnerWorkDestination.ReceiveMilk -> {
                    saleViewModel.close()
                    collectionViewModel.close()
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    receiveMilkViewModel.open(sessionHandle)
                }
                workDestination == NativeOwnerWorkDestination.MakeYogurt -> {
                    saleViewModel.close()
                    collectionViewModel.close()
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                    receiveMilkViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    makeYogurtViewModel.open(sessionHandle)
                }
                workDestination == NativeOwnerWorkDestination.RecordSpoilage -> {
                    saleViewModel.close()
                    collectionViewModel.close()
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    deliveriesViewModel.close()
                    recordSpoilageViewModel.open(sessionHandle)
                }
                workDestination == NativeOwnerWorkDestination.Deliveries -> {
                    saleViewModel.close()
                    collectionViewModel.close()
                    expenseViewModel.close()
                    vendorPaymentViewModel.close()
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.open(sessionHandle)
                }
                else -> closeOwnerWorkEntries()
            }
        }
        val ownerWorkDestination =
            (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination
        val ownerEntryOwnsBack = ownerSection == OwnerSection.Work &&
            ownerWorkDestination in setOf(
                NativeOwnerWorkDestination.RecordSale,
                NativeOwnerWorkDestination.RecordPayment,
                NativeOwnerWorkDestination.RecordExpense,
                NativeOwnerWorkDestination.PaySupplier,
                NativeOwnerWorkDestination.ReceiveMilk,
                NativeOwnerWorkDestination.MakeYogurt,
                NativeOwnerWorkDestination.RecordSpoilage,
                NativeOwnerWorkDestination.Deliveries,
            )
        val ownerRecordsOwnsBack =
            ownerSection == OwnerSection.Records &&
                (ownerRecordsState as? NativeOwnerRecordsState.Ready)?.detail != null
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
        BackHandler(enabled = !ownerEntryOwnsBack && !ownerRecordsOwnsBack && !ownerMoreOwnsBack) {
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
                    if (ready?.detail != null) {
                        ownerRecordsViewModel.clearDetail()
                    } else {
                        onLeaveToWelcome()
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
                            if (ready.costingPolicyDetail != null) {
                                ownerMoreViewModel.clearCostingPolicyDetail()
                            } else {
                                ownerMoreViewModel.backToMenu()
                            }
                        }
                        NativeOwnerMoreDestination.ShopSettings ->
                            ownerMoreViewModel.backToMenu()
                        NativeOwnerMoreDestination.UsersRoles -> {
                            if (ready.userDetail != null) {
                                ownerMoreViewModel.clearUserDetail()
                            } else {
                                ownerMoreViewModel.backToMenu()
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
                            ownerMoreViewModel.backToMenu()
                        else -> onLeaveToWelcome()
                    }
                }
            }
        }
        val navigateOwner: (OwnerSection) -> Unit = { section ->
            when (section) {
                OwnerSection.Overview -> {
                    if (ownerSection == OwnerSection.Overview) {
                        onShowNotice("Overview is already open.")
                    } else {
                        closeOwnerWorkEntries()
                        ownerWorkViewModel.close()
                        ownerRecordsViewModel.close()
                        ownerMoreViewModel.close()
                        onOwnerSectionChange(OwnerSection.Overview)
                    }
                }
                OwnerSection.Work -> {
                    if (ownerSection == OwnerSection.Work &&
                        ownerWorkDestination == NativeOwnerWorkDestination.Home
                    ) {
                        onShowNotice("Work is already open.")
                    } else {
                        closeOwnerWorkEntries()
                        ownerRecordsViewModel.close()
                        ownerMoreViewModel.close()
                        ownerWorkViewModel.backToHome()
                        onOwnerSectionChange(OwnerSection.Work)
                    }
                }
                OwnerSection.Records -> {
                    closeOwnerWorkEntries()
                    ownerWorkViewModel.close()
                    ownerMoreViewModel.close()
                    if (ownerSection == OwnerSection.Records &&
                        (ownerRecordsState as? NativeOwnerRecordsState.Ready)?.detail == null
                    ) {
                        onShowNotice("Records is already open.")
                    } else {
                        ownerRecordsViewModel.clearDetail()
                        onOwnerSectionChange(OwnerSection.Records)
                    }
                }
                OwnerSection.More -> {
                    closeOwnerWorkEntries()
                    ownerWorkViewModel.close()
                    ownerRecordsViewModel.close()
                    if (ownerSection == OwnerSection.More &&
                        (ownerMoreState as? NativeOwnerMoreState.Ready)?.destination ==
                        NativeOwnerMoreDestination.Menu
                    ) {
                        onShowNotice("More is already open.")
                    } else {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        ownerMoreViewModel.backToMenu()
                        onOwnerSectionChange(OwnerSection.More)
                    }
                }
            }
        }
        when {
            ownerSection == OwnerSection.More -> {
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
                    onBackToMenu = ownerMoreViewModel::backToMenu,
                    onRetryProducts = ownerMoreViewModel::refreshProducts,
                    onOpenProduct = ownerMoreViewModel::openProductDetail,
                    onBackFromProductDetail = ownerMoreViewModel::clearProductDetail,
                    onCreateProductPreview = {
                        onShowNotice("Add product comes next in the Owner Products family.")
                    },
                    onRetryCustomers = ownerMoreViewModel::refreshCustomers,
                    onOpenCustomer = ownerMoreViewModel::openCustomerDetail,
                    onBackFromCustomerDetail = ownerMoreViewModel::clearCustomerDetail,
                    onCreateCustomerPreview = {
                        onShowNotice("Add customer comes next in the Owner Customers family.")
                    },
                    onRetrySuppliers = ownerMoreViewModel::refreshSuppliers,
                    onOpenSupplier = ownerMoreViewModel::openSupplierDetail,
                    onBackFromSupplierDetail = ownerMoreViewModel::clearSupplierDetail,
                    onCreateSupplierPreview = {
                        onShowNotice("Add supplier comes next in the Owner Suppliers family.")
                    },
                    onRetryEmployees = ownerMoreViewModel::refreshEmployees,
                    onOpenEmployee = ownerMoreViewModel::openEmployeeDetail,
                    onBackFromEmployeeDetail = ownerMoreViewModel::clearEmployeeDetail,
                    onCreateEmployeePreview = {
                        onShowNotice("Add employee comes next in the Owner Employees family.")
                    },
                    onRetryCostingPlans = ownerMoreViewModel::refreshCostingPlans,
                    onOpenCostingPolicy = ownerMoreViewModel::openCostingPolicyDetail,
                    onBackFromCostingPolicyDetail = ownerMoreViewModel::clearCostingPolicyDetail,
                    onApproveCostingPolicyPreview = {
                        onShowNotice("Approve or edit costing policy comes next in the Owner Costing & plans family.")
                    },
                    onRetryShopSettings = ownerMoreViewModel::refreshShopSettings,
                    onEditShopSettingsPreview = {
                        onShowNotice("Edit shop settings comes next…")
                    },
                    onRetryUsersRoles = ownerMoreViewModel::refreshUsersRoles,
                    onOpenUser = ownerMoreViewModel::openUserDetail,
                    onBackFromUserDetail = ownerMoreViewModel::clearUserDetail,
                    onCreateUserPreview = {
                        onShowNotice("Enroll or change staff access comes next…")
                    },
                    onRetrySecurityHistory = ownerMoreViewModel::refreshSecurityHistory,
                    onOpenSecurityAudit = ownerMoreViewModel::openSecurityAuditDetail,
                    onBackFromSecurityAuditDetail = ownerMoreViewModel::clearSecurityAuditDetail,
                    onRetryBackup = ownerMoreViewModel::refreshBackup,
                    onExportBackupPreview = {
                        onShowNotice("Export backup comes next in the Owner Backup family.")
                    },
                    onRestoreBackupPreview = {
                        onShowNotice("Restore backup comes next in the Owner Backup family.")
                    },
                    onConnectBackupPreview = {
                        onShowNotice("Connect Drive comes next in the Owner Backup family.")
                    },
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
            ownerSection == OwnerSection.Records -> {
                OwnerRecordsScreen(
                    snackbarHostState = snackbarHostState,
                    state = ownerRecordsState,
                    onRetry = ownerRecordsViewModel::refresh,
                    onSearchQueryChange = ownerRecordsViewModel::setSearchQuery,
                    onOpenRecord = ownerRecordsViewModel::openDetail,
                    onBackFromDetail = ownerRecordsViewModel::clearDetail,
                    onLeaveConfirmed = onLeaveToWelcome,
                    onNavigate = navigateOwner,
                    onDeferredGroup = { group ->
                        onShowNotice(
                            "$group comes next in the Owner Records family.",
                        )
                    },
                )
            }
            ownerSection == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.RecordSale -> {
                OwnerSaleEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = saleState,
                    onRetry = saleViewModel::refresh,
                    onClearFeedback = saleViewModel::clearFeedback,
                    onRecordSale = saleViewModel::record,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            ownerSection == OwnerSection.Work &&
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
            ownerSection == OwnerSection.Work &&
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
            ownerSection == OwnerSection.Work &&
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
            ownerSection == OwnerSection.Work &&
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
            ownerSection == OwnerSection.Work &&
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
            ownerSection == OwnerSection.Work &&
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
            ownerSection == OwnerSection.Work &&
                ownerWorkDestination == NativeOwnerWorkDestination.Deliveries -> {
                OwnerDeliveriesEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = deliveriesState,
                    onRetry = deliveriesViewModel::refresh,
                    onMarkDelivered = deliveriesViewModel::markDelivered,
                    onBackToWork = { returnOwnerWorkHome() },
                    onNavigate = navigateOwner,
                )
            }
            ownerSection == OwnerSection.Work -> {
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
                OwnerOverviewScreen(
                    snackbarHostState = snackbarHostState,
                    state = ownerOverviewState,
                    section = ownerSection,
                    onRetry = ownerOverviewViewModel::refresh,
                    onLeaveConfirmed = onLeaveToWelcome,
                    onNavigate = navigateOwner,
                    onAttentionReview = {
                        onShowNotice(
                            "Close, Records, and Backup destinations come next in the Owner family.",
                        )
                    },
                )
            }
        }
    }
}

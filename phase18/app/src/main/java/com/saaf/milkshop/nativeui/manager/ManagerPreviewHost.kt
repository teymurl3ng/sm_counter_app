package com.saaf.milkshop.nativeui.manager

import com.saaf.milkshop.nativeui.common.ActivePaneBackHandler as BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.AdaptiveNavigationScaffold
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseViewModel
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
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsViewModel
import com.saaf.milkshop.nativeui.owner.OwnerRecordsScreen
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.sale.NativeSaleViewModel
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentViewModel
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesDraftActions
import java.time.LocalDate

/**
 * Manager-authenticated preview host (M3): Work desk with Sale/Collect/Ops hosts +
 * Close + Records + bounded More. Never mounts Owner/Cashier/Ops/Admin full screens
 * or Owner-only More rows.
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
    saleState: NativeSaleState,
    saleViewModel: NativeSaleViewModel,
    collectionState: NativeCollectionState,
    collectionViewModel: NativeCollectionViewModel,
    expenseState: NativeExpenseState,
    expenseViewModel: NativeExpenseViewModel,
    vendorPaymentState: NativeVendorPaymentState,
    vendorPaymentViewModel: NativeVendorPaymentViewModel,
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
    ownerRecordsState: NativeOwnerRecordsState,
    ownerRecordsViewModel: NativeOwnerRecordsViewModel,
    ownerMoreState: NativeOwnerMoreState,
    ownerMoreViewModel: NativeOwnerMoreViewModel,
    onLeaveToWelcome: () -> Unit,
    onLeaveToAccountPicker: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    SaafTheme {
        var lastSession by rememberSaveable { mutableStateOf<String?>(null) }

        fun closeEntries() {
            saleViewModel.close()
            collectionViewModel.close()
            expenseViewModel.close()
            vendorPaymentViewModel.close()
            cashCloseViewModel.close()
            receiveMilkViewModel.close()
            makeYogurtViewModel.close()
            recordSpoilageViewModel.close()
            deliveriesViewModel.close()
        }

        fun returnManagerWorkHome() {
            // Entry VMs stay warm for instant re-entry; close() is reserved
            // for session changes.
            managerWorkViewModel.backToHome()
        }

        LaunchedEffect(sessionHandle) {
            if (lastSession != null && lastSession != sessionHandle) {
                onManagerSectionChange(ManagerSection.Work)
                managerWorkViewModel.close()
                ownerRecordsViewModel.close()
                ownerMoreViewModel.close()
                closeEntries()
            }
            lastSession = sessionHandle
        }

        LaunchedEffect(sessionHandle, managerSection) {
            when (managerSection) {
                ManagerSection.Work -> managerWorkViewModel.open(sessionHandle)
                ManagerSection.Close -> cashCloseViewModel.open(sessionHandle)
                ManagerSection.Records -> ownerRecordsViewModel.open(sessionHandle)
                ManagerSection.More -> ownerMoreViewModel.open(sessionHandle)
            }
        }

        // Work entries stay warm across switches: closing siblings here forced
        // a full cold reload on every Work → back → Work round-trip. open()
        // re-validates cheaply (thin refresh bar) and close() is reserved for
        // session changes.
        LaunchedEffect(
            sessionHandle,
            managerSection,
            (managerWorkState as? NativeManagerWorkState.Ready)?.destination,
        ) {
            val workDestination =
                (managerWorkState as? NativeManagerWorkState.Ready)?.destination
            if (managerSection == ManagerSection.Work) {
                when (workDestination) {
                    NativeManagerWorkDestination.RecordSale -> saleViewModel.open(sessionHandle)
                    NativeManagerWorkDestination.RecordPayment -> collectionViewModel.open(sessionHandle)
                    NativeManagerWorkDestination.RecordExpense -> expenseViewModel.open(sessionHandle)
                    NativeManagerWorkDestination.PaySupplier -> vendorPaymentViewModel.open(sessionHandle)
                    NativeManagerWorkDestination.ReceiveMilk -> receiveMilkViewModel.open(sessionHandle)
                    NativeManagerWorkDestination.MakeYogurt -> makeYogurtViewModel.open(sessionHandle)
                    NativeManagerWorkDestination.RecordSpoilage -> recordSpoilageViewModel.open(sessionHandle)
                    NativeManagerWorkDestination.Deliveries -> deliveriesViewModel.open(sessionHandle)
                    else -> Unit
                }
            }
        }

        val workDestination =
            (managerWorkState as? NativeManagerWorkState.Ready)?.destination
        val workEntryOwnsBack = managerSection == ManagerSection.Work &&
            workDestination in setOf(
                NativeManagerWorkDestination.RecordSale,
                NativeManagerWorkDestination.RecordPayment,
                NativeManagerWorkDestination.ReceiveMilk,
                NativeManagerWorkDestination.MakeYogurt,
                NativeManagerWorkDestination.RecordSpoilage,
                NativeManagerWorkDestination.Deliveries,
                NativeManagerWorkDestination.RecordExpense,
                NativeManagerWorkDestination.PaySupplier,
            )
        val closeOwnsBack = managerSection == ManagerSection.Close
        val recordsReady = ownerRecordsState as? NativeOwnerRecordsState.Ready
        val recordsDetailOwnsBack = managerSection == ManagerSection.Records &&
            (recordsReady?.detail != null || recordsReady?.correction != null)
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

        val navigate: (ManagerSection) -> Unit = { section ->
            if (managerSection == ManagerSection.Work && section != ManagerSection.Work) {
                returnManagerWorkHome()
            }
            if (managerSection == section) {
                when (section) {
                    ManagerSection.Work -> {
                        val ready = managerWorkState as? NativeManagerWorkState.Ready
                        if (ready != null && ready.destination != NativeManagerWorkDestination.Home) {
                            returnManagerWorkHome()
                        } else {
                            onShowNotice("Work is already open.")
                        }
                    }
                    ManagerSection.Close -> onShowNotice("Close is already open.")
                    ManagerSection.Records -> {
                        val ready = ownerRecordsState as? NativeOwnerRecordsState.Ready
                        if (ready?.detail != null || ready?.correction != null) {
                            ownerRecordsViewModel.clearDetail()
                            ownerRecordsViewModel.clearLedger()
                        } else if (ready?.ledger != null) {
                            ownerRecordsViewModel.clearLedger()
                        } else {
                            onShowNotice("Records is already open.")
                        }
                    }
                    ManagerSection.More -> {
                        val ready = ownerMoreState as? NativeOwnerMoreState.Ready
                        if (ready != null && ready.destination != NativeOwnerMoreDestination.Menu) {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            ownerMoreViewModel.backToMenu()
                        } else {
                            onShowNotice("More is already open.")
                        }
                    }
                }
            }
            onManagerSectionChange(section)
        }

        val sectionStateHolder = rememberSaveableStateHolder()
        val workReady = managerWorkState as? NativeManagerWorkState.Ready
        // Static chrome (Cashier pattern): one scaffold + one header at the
        // host, outside the pane transition — nav and header never reload.
        var activeEntryGuard by remember { mutableStateOf<EntryLeaveGuard?>(null) }
        val guardedNavigate: (ManagerSection) -> Unit = { section ->
            val guard = activeEntryGuard
            if (guard != null) guard.requestLeave { navigate(section) } else navigate(section)
        }
        val headerTitle = when {
            managerSection == ManagerSection.Close -> "Cash count & close"
            managerSection == ManagerSection.Records -> "Records"
            managerSection == ManagerSection.More -> "More"
            workDestination == NativeManagerWorkDestination.RecordSale -> "New sale"
            workDestination == NativeManagerWorkDestination.RecordPayment -> "Collect payment"
            workDestination == NativeManagerWorkDestination.RecordExpense -> "Record expense"
            workDestination == NativeManagerWorkDestination.PaySupplier -> "Pay supplier"
            workDestination == NativeManagerWorkDestination.ReceiveMilk -> "Receive milk"
            workDestination == NativeManagerWorkDestination.MakeYogurt -> "Make yogurt"
            workDestination == NativeManagerWorkDestination.RecordSpoilage -> "Record spoilage"
            workDestination == NativeManagerWorkDestination.Deliveries -> "Active deliveries"
            else -> "Today's work"
        }
        AdaptiveNavigationScaffold(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            compactNavigation = { ManagerNavigation(selected = managerSection, onNavigate = guardedNavigate) },
            expandedNavigation = { ManagerNavigationRail(selected = managerSection, onNavigate = guardedNavigate) },
        ) { hostPadding ->
            Column(Modifier.fillMaxSize().padding(hostPadding)) {
        CashierScreenHeader(
            title = headerTitle,
            displayName = identity.displayName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = workReady?.context?.businessDate ?: com.saaf.milkshop.sync.BusinessClock.today().toString(),
            postingOpen = workReady?.context?.postingOpen ?: true,
        )
        NativeSectionTransition(
            targetState = managerSection,
            contentKey = { it },
            modifier = Modifier.weight(1f),
        ) { section ->
        sectionStateHolder.SaveableStateProvider("$sessionHandle:${section.name}") {
        androidx.compose.runtime.CompositionLocalProvider(com.saaf.milkshop.nativeui.sale.LocalPaneHeaderEnabled provides false) {
        NativeSectionTransition(
            targetState = workDestination,
            contentKey = { it },
        ) { destination ->
        val registerPaneGuard = com.saaf.milkshop.nativeui.common.rememberPaneGuardRegistration({ activeEntryGuard }) { activeEntryGuard = it }
        when {
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.RecordSale -> {
                ManagerSaleEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = saleState,
                    onRetry = saleViewModel::refresh,
                    onClearFeedback = saleViewModel::clearFeedback,
                    onRecordSale = saleViewModel::record,
                onPreviewCredit = saleViewModel::previewCredit,
                    onSendAsDelivery = saleViewModel::sendAsDelivery,
                    onStartNextSale = saleViewModel::startNextSale,
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.RecordPayment -> {
                ManagerCollectionEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = collectionState,
                    onRetry = collectionViewModel::refresh,
                    onSelectCustomer = collectionViewModel::selectCustomer,
                    onRecordCollection = collectionViewModel::record,
                    onCollectAnother = collectionViewModel::collectAnother,
                    onViewRecord = {
                        onShowNotice(
                            "Manager Records stays available from the Records tab.",
                        )
                    },
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.RecordExpense -> {
                ManagerExpenseEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = expenseState,
                    onRetry = expenseViewModel::refresh,
                    onRecordExpense = expenseViewModel::record,
                    onRecordAnother = expenseViewModel::recordAnother,
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.PaySupplier -> {
                ManagerPaySupplierEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = vendorPaymentState,
                    onRetry = vendorPaymentViewModel::refresh,
                    onRecordPayment = vendorPaymentViewModel::record,
                    onRecordAnother = vendorPaymentViewModel::recordAnother,
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.ReceiveMilk -> {
                ManagerReceiveMilkEntryScreen(
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
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.MakeYogurt -> {
                ManagerMakeYogurtEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = makeYogurtState,
                    onRetry = makeYogurtViewModel::refresh,
                    onRecord = makeYogurtViewModel::record,
                    onMakeAnother = makeYogurtViewModel::makeAnother,
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.RecordSpoilage -> {
                ManagerRecordSpoilageEntryScreen(
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
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work &&
                destination == NativeManagerWorkDestination.Deliveries -> {
                ManagerDeliveriesEntryScreen(
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
                    onBackToWork = { returnManagerWorkHome() },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Close -> {
                ManagerCashCloseEntryScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = cashCloseState,
                    onRetry = cashCloseViewModel::refresh,
                    onRecordCount = cashCloseViewModel::recordCount,
                    onCloseDay = cashCloseViewModel::closeDay,
                    onResolveDelivery = cashCloseViewModel::resolveDelivery,
                    onBackToWork = {
                        onManagerSectionChange(ManagerSection.Work)
                    },
                    onRegisterGuard = registerPaneGuard,
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Work -> {
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
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == ManagerSection.Records -> {
                OwnerRecordsScreen(
                    snackbarHostState = snackbarHostState,
                    state = ownerRecordsState,
                    onRetry = ownerRecordsViewModel::refresh,
                    onSearchQueryChange = ownerRecordsViewModel::setSearchQuery,
                    onOpenRecord = ownerRecordsViewModel::openDetail,
                    onBackFromDetail = ownerRecordsViewModel::clearDetail,
                    onOpenLedger = ownerRecordsViewModel::openLedger,
                    onBackFromLedger = ownerRecordsViewModel::clearLedger,
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
                    showHeader = false,
                    rootTag = NativeUiTags.ManagerRecordsRoot,
                    loadingTag = NativeUiTags.ManagerRecordsLoading,
                    onSelectGroup = ownerRecordsViewModel::setActiveGroup,
                    onOpenCorrection = ownerRecordsViewModel::openCorrection,
                    onBackFromCorrection = ownerRecordsViewModel::clearCorrection,
                    onUpdateCorrection = ownerRecordsViewModel::updateCorrection,
                    onSaveCorrection = ownerRecordsViewModel::saveCorrection,
                    onRefreshCorrectionFacts = ownerRecordsViewModel::refreshCorrectionFacts,
                    onOpenReceipt = ownerRecordsViewModel::openReceipt,
                    onBackFromReceipt = ownerRecordsViewModel::clearReceipt,
                    onUpdateReceipt = ownerRecordsViewModel::updateReceipt,
                    onSaveReceipt = ownerRecordsViewModel::saveReceipt,
                    onRefreshReceiptFacts = ownerRecordsViewModel::refreshReceiptFacts,
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
                    mgrScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.ManagerShellRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
        }
        }
        }
        }
    }
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
    SaafTheme {
        BackHandler {
            when (managerSection) {
                ManagerSection.Work -> Unit
                ManagerSection.Close,
                ManagerSection.Records,
                ManagerSection.More,
                -> onManagerSectionChange(ManagerSection.Work)
            }
        }
        val businessDate = com.saaf.milkshop.sync.BusinessClock.today().toString()
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

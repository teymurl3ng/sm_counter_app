package com.saaf.milkshop.nativeui.sale

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
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.preview.NativePreviewSampleData
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.nativeui.activity.CashierActivityScreen
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.activity.NativeActivityState
import com.saaf.milkshop.nativeui.activity.NativeActivityViewModel
import com.saaf.milkshop.nativeui.collection.CashierCollectionScreen
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.collection.NativeCollectionViewModel
import com.saaf.milkshop.nativeui.expense.NativeExpenseState
import com.saaf.milkshop.nativeui.expense.NativeExpenseViewModel
import com.saaf.milkshop.nativeui.more.CashierExpenseEntryScreen
import com.saaf.milkshop.nativeui.more.CashierDeliveriesEntryScreen
import com.saaf.milkshop.nativeui.more.CashierMoreScreen
import com.saaf.milkshop.nativeui.more.NativeMoreDestination
import com.saaf.milkshop.nativeui.more.NativeMoreState
import com.saaf.milkshop.nativeui.more.NativeMoreViewModel
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesViewModel
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesDraftActions

/**
 * Cashier-authenticated preview host: Sell / Collect / Activity / More.
 * Counter Ledger surfaces stay on the approved light palette.
 */
@Composable
fun CashierPreviewHost(
    sessionHandle: String,
    identity: NativeSignedInIdentity,
    database: MilkShopDatabase,
    security: UserSecurityService,
    saleFacade: NativeSaleFacade,
    collectionFacade: NativeCollectionFacade,
    activityFacade: NativeActivityFacade,
    snackbarHostState: SnackbarHostState,
    cashierSection: CashierSection,
    onCashierSectionChange: (CashierSection) -> Unit,
    sellNotice: String,
    saleState: NativeSaleState,
    saleViewModel: NativeSaleViewModel,
    collectionState: NativeCollectionState,
    collectionViewModel: NativeCollectionViewModel,
    activityState: NativeActivityState,
    activityViewModel: NativeActivityViewModel,
    moreState: NativeMoreState,
    moreViewModel: NativeMoreViewModel,
    expenseState: NativeExpenseState,
    expenseViewModel: NativeExpenseViewModel,
    deliveriesState: NativeDeliveriesState,
    deliveriesViewModel: NativeDeliveriesViewModel,
    onLeaveToWelcome: () -> Unit,
    onLeaveToAccountPicker: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    // Authentication owns its fixed light boundary; signed-in work follows
    // the system theme through SaafTheme's light/dark Material color roles.
    SaafTheme {
        var sampleReady by remember(sessionHandle) { mutableStateOf(false) }
        var lastCashierSession by rememberSaveable { mutableStateOf<String?>(null) }
        var activityReturnSection by rememberSaveable { mutableStateOf(CashierSection.Sell.name) }
        LaunchedEffect(sessionHandle) {
            if (lastCashierSession != null && lastCashierSession != sessionHandle) {
                onCashierSectionChange(CashierSection.Sell)
                saleViewModel.close()
                collectionViewModel.close()
                activityViewModel.close()
                moreViewModel.close()
                expenseViewModel.close()
                deliveriesViewModel.close()
            }
            lastCashierSession = sessionHandle
            sampleReady = false
            val sample = runCatching {
                NativePreviewSampleData.ensureForSession(
                    database = database,
                    security = security,
                    sessionHandle = sessionHandle,
                )
            }.getOrNull()
            sampleReady = true
            if ((sample?.seededStaffAccounts ?: 0) > 0) {
                snackbarHostState.showSnackbar(
                    message = NativePreviewSampleData.previewAccountsHint(),
                    duration = SnackbarDuration.Long,
                )
            } else if (sample?.changed == true) {
                snackbarHostState.showSnackbar(
                    message = "Sample shop setup ready — products, customers, and accounts.",
                    duration = SnackbarDuration.Short,
                )
            }
        }
        LaunchedEffect(
            sessionHandle,
            cashierSection,
            sampleReady,
        ) {
            if (!sampleReady) return@LaunchedEffect
            // Sections stay warm: open() re-validates cheaply and close() is
            // reserved for session changes, so re-entry shows content at once.
            when (cashierSection) {
                CashierSection.Sell -> saleViewModel.open(sessionHandle)
                CashierSection.Collect -> collectionViewModel.open(sessionHandle)
                CashierSection.Activity -> Unit
                CashierSection.More -> moreViewModel.open(sessionHandle)
            }
        }
        LaunchedEffect(
            sessionHandle,
            cashierSection,
            sampleReady,
            (moreState as? NativeMoreState.Ready)?.destination,
        ) {
            if (!sampleReady || cashierSection != CashierSection.More) return@LaunchedEffect
            val destination = (moreState as? NativeMoreState.Ready)?.destination
            if (destination == NativeMoreDestination.Expense) {
                expenseViewModel.open(sessionHandle)
            }
            if (destination == NativeMoreDestination.Deliveries) {
                deliveriesViewModel.open(sessionHandle)
            }
        }
        BackHandler {
            when (cashierSection) {
                CashierSection.Collect -> onCashierSectionChange(CashierSection.Sell)
                CashierSection.Activity -> {
                    val ready = activityState as? NativeActivityState.Ready
                    if (ready?.detail != null) {
                        activityViewModel.clearDetail()
                    } else {
                        val returnTo = runCatching {
                            CashierSection.valueOf(activityReturnSection)
                        }.getOrDefault(CashierSection.Sell)
                        activityReturnSection = CashierSection.Sell.name
                        onCashierSectionChange(
                            if (returnTo == CashierSection.Collect) {
                                CashierSection.Collect
                            } else {
                                CashierSection.Sell
                            },
                        )
                    }
                }
                CashierSection.More -> {
                    val ready = moreState as? NativeMoreState.Ready
                    if (ready != null && ready.destination == NativeMoreDestination.Expense) {
                        // CashierExpenseEntryScreen owns Back + leave-guard.
                    } else if (ready != null && ready.destination != NativeMoreDestination.Menu) {
                        moreViewModel.backToMenu()
                    } else {
                        onCashierSectionChange(CashierSection.Sell)
                    }
                }
                else -> onLeaveToWelcome()
            }
        }
        val navigateCashier: (CashierSection) -> Unit = { section ->
            when (section) {
                CashierSection.Sell -> {
                    if (cashierSection == CashierSection.Sell) onShowNotice(sellNotice)
                    else onCashierSectionChange(CashierSection.Sell)
                }
                CashierSection.Collect -> {
                    if (cashierSection == CashierSection.Collect) {
                        onShowNotice("Collect is already open.")
                    } else {
                        onCashierSectionChange(CashierSection.Collect)
                    }
                }
                CashierSection.Activity -> {
                    if (cashierSection == CashierSection.Activity) {
                        onShowNotice("Activity is already open.")
                    } else {
                        activityReturnSection = CashierSection.Sell.name
                        activityViewModel.open(sessionHandle)
                        onCashierSectionChange(CashierSection.Activity)
                    }
                }
                CashierSection.More -> {
                    if (cashierSection == CashierSection.More) {
                        onShowNotice("More is already open.")
                    } else {
                        moreViewModel.open(sessionHandle)
                        onCashierSectionChange(CashierSection.More)
                    }
                }
            }
        }
        val sectionStateHolder = rememberSaveableStateHolder()
        // Section swaps crossfade; the state key still restores per-section UI.
        NativeSectionTransition<Pair<CashierSection, NativeMoreDestination?>>(
            targetState = cashierSection to (moreState as? NativeMoreState.Ready)?.destination,
            contentKey = { it },
        ) { pane ->
            val (section, paneDestination) = pane
        sectionStateHolder.SaveableStateProvider("$sessionHandle:${section.name}") {
        when (section) {
            CashierSection.Sell -> CashierSaleScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = saleState,
                onRetry = saleViewModel::refresh,
                onClearFeedback = saleViewModel::clearFeedback,
                onRecordSale = saleViewModel::record,
                    onSendAsDelivery = saleViewModel::sendAsDelivery,
                    onStartNextSale = saleViewModel::startNextSale,
                onLeaveConfirmed = onLeaveToWelcome,
                onNavigate = navigateCashier,
            )
            CashierSection.Collect -> CashierCollectionScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = collectionState,
                onRetry = collectionViewModel::refresh,
                onSelectCustomer = collectionViewModel::selectCustomer,
                onRecordCollection = collectionViewModel::record,
                onCollectAnother = collectionViewModel::collectAnother,
                onViewRecord = { eventId ->
                    activityReturnSection = CashierSection.Collect.name
                    activityViewModel.openDetailFromCollect(sessionHandle, eventId)
                    onCashierSectionChange(CashierSection.Activity)
                },
                onLeaveConfirmed = { onCashierSectionChange(CashierSection.Sell) },
                onNavigate = navigateCashier,
            )
            CashierSection.Activity -> CashierActivityScreen(
                snackbarHostState = snackbarHostState,
                identity = identity,
                state = activityState,
                onRetry = activityViewModel::refresh,
                onOpenRecord = activityViewModel::openDetail,
                onBackFromDetail = {
                    val returnTo = runCatching {
                        CashierSection.valueOf(activityReturnSection)
                    }.getOrDefault(CashierSection.Sell)
                    if (returnTo == CashierSection.Collect) {
                        activityReturnSection = CashierSection.Sell.name
                        activityViewModel.clearDetail()
                        onCashierSectionChange(CashierSection.Collect)
                    } else {
                        activityViewModel.clearDetail()
                    }
                },
                onLeaveConfirmed = {
                    activityReturnSection = CashierSection.Sell.name
                    onCashierSectionChange(CashierSection.Sell)
                },
                onNavigate = navigateCashier,
            )
            CashierSection.More -> {
                val moreReady = moreState as? NativeMoreState.Ready
                if (moreReady?.destination == NativeMoreDestination.Expense) {
                    CashierExpenseEntryScreen(
                        snackbarHostState = snackbarHostState,
                        identity = identity,
                        state = expenseState,
                        onRetry = expenseViewModel::refresh,
                        onRecordExpense = expenseViewModel::record,
                        onRecordAnother = expenseViewModel::recordAnother,
                        onBackToMore = {
                            moreViewModel.backToMenu()
                        },
                        onNavigate = { section ->
                            moreViewModel.backToMenu()
                            navigateCashier(section)
                        },
                    )
                } else if (moreReady?.destination == NativeMoreDestination.Deliveries) {
                    CashierDeliveriesEntryScreen(
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
                        onBackToMore = {
                            moreViewModel.backToMenu()
                        },
                        onNavigate = { section ->
                            moreViewModel.backToMenu()
                            navigateCashier(section)
                        },
                    )
                } else {
                    CashierMoreScreen(
                        snackbarHostState = snackbarHostState,
                        identity = identity,
                        state = moreState,
                        onRetry = moreViewModel::refresh,
                        onOpenCountCash = moreViewModel::openCountCash,
                        onOpenExpense = {
                            moreViewModel.openExpense()
                            expenseViewModel.open(sessionHandle)
                        },
                        onOpenDeliveries = {
                            moreViewModel.openDeliveries()
                            deliveriesViewModel.open(sessionHandle)
                        },
                        onOpenPersonalSecurity = moreViewModel::openPersonalSecurity,
                        onBackToMenu = {
                            moreViewModel.backToMenu()
                        },
                        onRecordCashCount = moreViewModel::recordCashCount,
                        onRequestRemoveQuickUnlock = moreViewModel::requestRemoveQuickUnlock,
                        onCancelRemoveQuickUnlock = moreViewModel::cancelRemoveQuickUnlock,
                        onRemoveQuickUnlock = moreViewModel::removeQuickUnlock,
                        onEnrollQuickPin = moreViewModel::enrollQuickPin,
                        onEnrollPhoneSecurity = moreViewModel::enrollPhoneSecurity,
                        onSwitchAccount = onLeaveToAccountPicker,
                        onSignOut = onLeaveToWelcome,
                        onNavigate = navigateCashier,
                    )
                }
            }
        }
        }
        }
    }
}

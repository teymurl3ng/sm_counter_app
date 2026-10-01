package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesDraftActions

/**
 * Operations-authenticated preview host: Work/Deliveries/Attendance/More plus
 * Work entry screens (Receive Milk, Make Yogurt, Record Spoilage).
 */
@Composable
fun OperationsPreviewHost(
    sessionHandle: String,
    identity: NativeSignedInIdentity,
    snackbarHostState: SnackbarHostState,
    operationsSection: OperationsSection,
    onOperationsSectionChange: (OperationsSection) -> Unit,
    operationsWorkState: NativeOperationsWorkState,
    operationsWorkViewModel: NativeOperationsWorkViewModel,
    receiveMilkState: NativeReceiveMilkState,
    receiveMilkViewModel: NativeReceiveMilkViewModel,
    makeYogurtState: NativeMakeYogurtState,
    makeYogurtViewModel: NativeMakeYogurtViewModel,
    recordSpoilageState: NativeRecordSpoilageState,
    recordSpoilageViewModel: NativeRecordSpoilageViewModel,
    deliveriesState: NativeDeliveriesState,
    deliveriesViewModel: NativeDeliveriesViewModel,
    attendanceState: NativeAttendanceState,
    attendanceViewModel: NativeAttendanceViewModel,
    operationsMoreState: NativeOperationsMoreState,
    operationsMoreViewModel: NativeOperationsMoreViewModel,
    onLeaveToWelcome: () -> Unit,
    onLeaveToAccountPicker: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    SaafTheme {
        var lastOpsSession by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(sessionHandle) {
            if (lastOpsSession != null && lastOpsSession != sessionHandle) {
                onOperationsSectionChange(OperationsSection.Work)
                operationsWorkViewModel.close()
                deliveriesViewModel.close()
                attendanceViewModel.close()
                operationsMoreViewModel.close()
                receiveMilkViewModel.close()
                makeYogurtViewModel.close()
                recordSpoilageViewModel.close()
            }
            lastOpsSession = sessionHandle
        }
        LaunchedEffect(
            sessionHandle,
            operationsSection,
        ) {
            when (operationsSection) {
                OperationsSection.Work -> operationsWorkViewModel.open(sessionHandle)
                OperationsSection.Deliveries -> deliveriesViewModel.open(sessionHandle)
                OperationsSection.Attendance -> attendanceViewModel.open(sessionHandle)
                OperationsSection.More -> operationsMoreViewModel.open(sessionHandle)
            }
        }
        // Work entries stay warm across switches: closing siblings here forced
        // a full cold reload on every destination round-trip. open()
        // re-validates cheaply and close() is reserved for session changes.
        LaunchedEffect(
            sessionHandle,
            operationsSection,
            (operationsWorkState as? NativeOperationsWorkState.Ready)?.destination,
        ) {
            val destination =
                (operationsWorkState as? NativeOperationsWorkState.Ready)?.destination
            if (operationsSection == OperationsSection.Work) {
                when (destination) {
                    NativeOperationsWorkDestination.ReceiveMilk -> receiveMilkViewModel.open(sessionHandle)
                    NativeOperationsWorkDestination.MakeYogurt -> makeYogurtViewModel.open(sessionHandle)
                    NativeOperationsWorkDestination.RecordSpoilage -> recordSpoilageViewModel.open(sessionHandle)
                    else -> Unit
                }
            }
        }
        val workDestination =
            (operationsWorkState as? NativeOperationsWorkState.Ready)?.destination
        BackHandler(
            enabled = operationsSection != OperationsSection.Work ||
                (
                    workDestination != NativeOperationsWorkDestination.ReceiveMilk &&
                        workDestination != NativeOperationsWorkDestination.MakeYogurt &&
                        workDestination != NativeOperationsWorkDestination.RecordSpoilage
                    ),
        ) {
            when (operationsSection) {
                OperationsSection.Work -> {
                    val ready = operationsWorkState as? NativeOperationsWorkState.Ready
                    if (ready != null &&
                        ready.destination != NativeOperationsWorkDestination.Home
                    ) {
                        operationsWorkViewModel.backToHome()
                    } else {
                        onLeaveToWelcome()
                    }
                }
                OperationsSection.More -> {
                    val ready = operationsMoreState as? NativeOperationsMoreState.Ready
                    if (ready != null &&
                        ready.destination != NativeOperationsMoreDestination.Menu
                    ) {
                        operationsMoreViewModel.backToMenu()
                    } else {
                        onOperationsSectionChange(OperationsSection.Work)
                    }
                }
                else -> onOperationsSectionChange(OperationsSection.Work)
            }
        }
        val navigateOps: (OperationsSection) -> Unit = { section ->
            if (operationsSection == section) {
                when (section) {
                    OperationsSection.Work -> {
                        val ready = operationsWorkState as? NativeOperationsWorkState.Ready
                        if (ready != null && ready.destination != NativeOperationsWorkDestination.Home) {
                            operationsWorkViewModel.backToHome()
                        } else {
                            onShowNotice("Work is already open.")
                        }
                    }
                    OperationsSection.Deliveries -> onShowNotice("Deliveries is already open.")
                    OperationsSection.Attendance -> onShowNotice("Attendance is already open.")
                    OperationsSection.More -> {
                        val ready = operationsMoreState as? NativeOperationsMoreState.Ready
                        if (ready != null && ready.destination != NativeOperationsMoreDestination.Menu) {
                            operationsMoreViewModel.backToMenu()
                        } else {
                            onShowNotice("More is already open.")
                        }
                    }
                }
            } else {
                if (operationsSection == OperationsSection.Work) {
                    operationsWorkViewModel.backToHome()
                }
                onOperationsSectionChange(section)
            }
        }
        val workReady = operationsWorkState as? NativeOperationsWorkState.Ready
        val sectionStateHolder = rememberSaveableStateHolder()
        // Static chrome (Cashier pattern): one scaffold + one header at the
        // host, outside the pane transition — nav and header never reload.
        var activeEntryGuard by remember { mutableStateOf<EntryLeaveGuard?>(null) }
        val guardedNavigateOps: (OperationsSection) -> Unit = { section ->
            val guard = activeEntryGuard
            if (guard != null) guard.requestLeave { navigateOps(section) } else navigateOps(section)
        }
        val moreReady = operationsMoreState as? NativeOperationsMoreState.Ready
        val headerTitle = when {
            operationsSection == OperationsSection.Deliveries -> "Active deliveries"
            operationsSection == OperationsSection.Attendance -> "My attendance"
            operationsSection == OperationsSection.More -> "More"
            workDestination == NativeOperationsWorkDestination.ReceiveMilk -> "Receive milk"
            workDestination == NativeOperationsWorkDestination.MakeYogurt -> "Make yogurt"
            workDestination == NativeOperationsWorkDestination.RecordSpoilage -> "Record spoilage"
            else -> "Work"
        }
        AdaptiveNavigationScaffold(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            compactNavigation = { OperationsNavigation(selected = operationsSection, onNavigate = guardedNavigateOps) },
            expandedNavigation = { OperationsNavigationRail(selected = operationsSection, onNavigate = guardedNavigateOps) },
        ) { hostPadding ->
            Column(Modifier.fillMaxSize().padding(hostPadding)) {
        CashierScreenHeader(
            title = headerTitle,
            displayName = identity.displayName,
            roleLabel = formatCashierRoleLabel(identity.role.wireValue),
            businessDate = workReady?.context?.businessDate ?: "",
            postingOpen = workReady?.context?.postingOpen ?: true,
        )
        NativeSectionTransition<Pair<OperationsSection, NativeOperationsWorkDestination?>>(
            targetState = operationsSection to (operationsWorkState as? NativeOperationsWorkState.Ready)?.destination,
            contentKey = { it },
            modifier = Modifier.weight(1f),
        ) { pane ->
            val (section, paneDestination) = pane
        sectionStateHolder.SaveableStateProvider("$sessionHandle:${section.name}:$paneDestination") {
        when {
            section == OperationsSection.Deliveries -> {
                DeliveriesScreen(
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
                    workScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.OpsWorkRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                    onLeaveConfirmed = onLeaveToWelcome,
                )
            }
            section == OperationsSection.Attendance -> {
                AttendanceScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = attendanceState,
                    onRetry = attendanceViewModel::refresh,
                    onMarkPresent = attendanceViewModel::markPresent,
                    attendanceScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.OpsWorkRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                    onLeaveConfirmed = onLeaveToWelcome,
                )
            }
            section == OperationsSection.More -> {
                OperationsMoreScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = operationsMoreState,
                    onRetry = operationsMoreViewModel::refresh,
                    onOpenTodayWork = operationsMoreViewModel::openTodayWork,
                    onOpenPersonalSecurity = operationsMoreViewModel::openPersonalSecurity,
                    onBackToMenu = operationsMoreViewModel::backToMenu,
                    onRequestRemoveQuickUnlock = operationsMoreViewModel::requestRemoveQuickUnlock,
                    onCancelRemoveQuickUnlock = operationsMoreViewModel::cancelRemoveQuickUnlock,
                    onRemoveQuickUnlock = operationsMoreViewModel::removeQuickUnlock,
                    onEnrollQuickPin = operationsMoreViewModel::enrollQuickPin,
                    onEnrollPhoneSecurity = operationsMoreViewModel::enrollPhoneSecurity,
                    onSwitchAccount = onLeaveToAccountPicker,
                    onSignOut = onLeaveToWelcome,
                    moreScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.OpsMoreRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                    onLeaveConfirmed = onLeaveToWelcome,
                )
            }
            section == OperationsSection.Work &&
                workReady?.destination == NativeOperationsWorkDestination.ReceiveMilk -> {
                ReceiveMilkScreen(
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
                    onBackToHome = {
                        operationsWorkViewModel.backToHome()
                    },
                    onRegisterGuard = { activeEntryGuard = it },
                    workScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.OpsWorkRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == OperationsSection.Work &&
                workReady?.destination == NativeOperationsWorkDestination.MakeYogurt -> {
                MakeYogurtScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = makeYogurtState,
                    onRetry = makeYogurtViewModel::refresh,
                    onRecord = makeYogurtViewModel::record,
                    onMakeAnother = makeYogurtViewModel::makeAnother,
                    onBackToHome = {
                        operationsWorkViewModel.backToHome()
                    },
                    onRegisterGuard = { activeEntryGuard = it },
                    workScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.OpsWorkRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            section == OperationsSection.Work &&
                workReady?.destination == NativeOperationsWorkDestination.RecordSpoilage -> {
                RecordSpoilageScreen(
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
                    onBackToHome = {
                        operationsWorkViewModel.backToHome()
                    },
                    onRegisterGuard = { activeEntryGuard = it },
                    workScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.OpsWorkRoot)) {
                            content(PaddingValues(0.dp))
                        }
                    },
                )
            }
            else -> {
                OperationsWorkScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = operationsWorkState,
                    section = operationsSection,
                    onRetry = operationsWorkViewModel::refresh,
                    onOpenReceiveMilk = {
                        operationsWorkViewModel.openReceiveMilk()
                        receiveMilkViewModel.open(sessionHandle)
                    },
                    onOpenMakeYogurt = {
                        operationsWorkViewModel.openMakeYogurt()
                        makeYogurtViewModel.open(sessionHandle)
                    },
                    onOpenRecordSpoilage = {
                        operationsWorkViewModel.openRecordSpoilage()
                        recordSpoilageViewModel.open(sessionHandle)
                    },
                    onBackToHome = operationsWorkViewModel::backToHome,
                    onLeaveConfirmed = onLeaveToWelcome,
                    workScaffold = { s, m, content ->
                        Box(modifier = m.testTag(NativeUiTags.OpsWorkRoot)) {
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

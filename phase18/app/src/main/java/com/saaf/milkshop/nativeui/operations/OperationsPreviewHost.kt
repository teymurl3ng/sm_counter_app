package com.saaf.milkshop.nativeui.operations

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.nativeui.theme.SaafTheme

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
    SaafTheme(darkTheme = false) {
        var lastOpsSession by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(sessionHandle) {
            if (lastOpsSession != null && lastOpsSession != sessionHandle) {
                onOperationsSectionChange(OperationsSection.Work)
            }
            lastOpsSession = sessionHandle
        }
        LaunchedEffect(
            sessionHandle,
            operationsSection,
        ) {
            when (operationsSection) {
                OperationsSection.Work -> {
                    deliveriesViewModel.close()
                    attendanceViewModel.close()
                    operationsMoreViewModel.close()
                    operationsWorkViewModel.open(sessionHandle)
                }
                OperationsSection.Deliveries -> {
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    attendanceViewModel.close()
                    operationsMoreViewModel.close()
                    operationsWorkViewModel.backToHome()
                    deliveriesViewModel.open(sessionHandle)
                }
                OperationsSection.Attendance -> {
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    operationsMoreViewModel.close()
                    operationsWorkViewModel.backToHome()
                    attendanceViewModel.open(sessionHandle)
                }
                OperationsSection.More -> {
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    deliveriesViewModel.close()
                    attendanceViewModel.close()
                    operationsWorkViewModel.backToHome()
                    operationsMoreViewModel.open(sessionHandle)
                }
            }
        }
        LaunchedEffect(
            sessionHandle,
            operationsSection,
            (operationsWorkState as? NativeOperationsWorkState.Ready)?.destination,
        ) {
            val destination =
                (operationsWorkState as? NativeOperationsWorkState.Ready)?.destination
            when {
                operationsSection != OperationsSection.Work -> {
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                }
                destination == NativeOperationsWorkDestination.ReceiveMilk -> {
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    receiveMilkViewModel.open(sessionHandle)
                }
                destination == NativeOperationsWorkDestination.MakeYogurt -> {
                    receiveMilkViewModel.close()
                    recordSpoilageViewModel.close()
                    makeYogurtViewModel.open(sessionHandle)
                }
                destination == NativeOperationsWorkDestination.RecordSpoilage -> {
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.open(sessionHandle)
                }
                destination != null -> {
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
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
                        receiveMilkViewModel.close()
                        makeYogurtViewModel.close()
                        recordSpoilageViewModel.close()
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
                        operationsMoreViewModel.close()
                        onOperationsSectionChange(OperationsSection.Work)
                    }
                }
                else -> onOperationsSectionChange(OperationsSection.Work)
            }
        }
        val navigateOps: (OperationsSection) -> Unit = { section ->
            when (section) {
                OperationsSection.Work -> {
                    if (operationsSection == OperationsSection.Work) {
                        onShowNotice("Work is already open.")
                    } else {
                        onOperationsSectionChange(OperationsSection.Work)
                    }
                }
                OperationsSection.Deliveries,
                OperationsSection.Attendance,
                OperationsSection.More,
                -> {
                    receiveMilkViewModel.close()
                    makeYogurtViewModel.close()
                    recordSpoilageViewModel.close()
                    if (section != OperationsSection.Deliveries) {
                        deliveriesViewModel.close()
                    }
                    if (section != OperationsSection.Attendance) {
                        attendanceViewModel.close()
                    }
                    if (section != OperationsSection.More) {
                        operationsMoreViewModel.close()
                    }
                    operationsWorkViewModel.backToHome()
                    onOperationsSectionChange(section)
                }
            }
        }
        val workReady = operationsWorkState as? NativeOperationsWorkState.Ready
        when {
            operationsSection == OperationsSection.Deliveries -> {
                DeliveriesScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = deliveriesState,
                    onRetry = deliveriesViewModel::refresh,
                    onMarkDelivered = deliveriesViewModel::markDelivered,
                    onNavigate = navigateOps,
                    onLeaveConfirmed = onLeaveToWelcome,
                )
            }
            operationsSection == OperationsSection.Attendance -> {
                AttendanceScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = attendanceState,
                    onRetry = attendanceViewModel::refresh,
                    onMarkPresent = attendanceViewModel::markPresent,
                    onNavigate = navigateOps,
                    onLeaveConfirmed = onLeaveToWelcome,
                )
            }
            operationsSection == OperationsSection.More -> {
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
                    onNavigate = navigateOps,
                    onLeaveConfirmed = onLeaveToWelcome,
                )
            }
            operationsSection == OperationsSection.Work &&
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
                        receiveMilkViewModel.close()
                        operationsWorkViewModel.backToHome()
                    },
                    onNavigate = navigateOps,
                )
            }
            operationsSection == OperationsSection.Work &&
                workReady?.destination == NativeOperationsWorkDestination.MakeYogurt -> {
                MakeYogurtScreen(
                    snackbarHostState = snackbarHostState,
                    identity = identity,
                    state = makeYogurtState,
                    onRetry = makeYogurtViewModel::refresh,
                    onRecord = makeYogurtViewModel::record,
                    onMakeAnother = makeYogurtViewModel::makeAnother,
                    onBackToHome = {
                        makeYogurtViewModel.close()
                        operationsWorkViewModel.backToHome()
                    },
                    onNavigate = navigateOps,
                )
            }
            operationsSection == OperationsSection.Work &&
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
                        recordSpoilageViewModel.close()
                        operationsWorkViewModel.backToHome()
                    },
                    onNavigate = navigateOps,
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
                    onNavigate = navigateOps,
                )
            }
        }
    }
}

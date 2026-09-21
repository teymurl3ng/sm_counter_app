package com.saaf.milkshop.nativeui

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.R
import com.saaf.milkshop.application.activity.NativeActivityFacade
import com.saaf.milkshop.application.authentication.NativeAuthenticationFacade
import com.saaf.milkshop.application.authentication.NativeQuickUnlockFacade
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.more.NativeMoreFacade
import com.saaf.milkshop.application.operations.NativeAttendanceFacade
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.application.operations.NativeMakeYogurtFacade
import com.saaf.milkshop.application.operations.NativeOperationsMoreFacade
import com.saaf.milkshop.application.operations.NativeOperationsWorkFacade
import com.saaf.milkshop.application.operations.NativeReceiveMilkFacade
import com.saaf.milkshop.application.operations.NativeRecordSpoilageFacade
import com.saaf.milkshop.application.owner.NativeOwnerOverviewFacade
import com.saaf.milkshop.application.owner.NativeOwnerWorkFacade
import com.saaf.milkshop.application.preview.NativePreviewSampleData
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.nativeui.activity.CashierActivityScreen
import com.saaf.milkshop.nativeui.activity.NativeActivityState
import com.saaf.milkshop.nativeui.activity.NativeActivityViewModel
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationHost
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationState
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationViewModel
import com.saaf.milkshop.nativeui.collection.CashierCollectionScreen
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.collection.NativeCollectionViewModel
import com.saaf.milkshop.nativeui.more.CashierMoreScreen
import com.saaf.milkshop.nativeui.more.NativeMoreDestination
import com.saaf.milkshop.nativeui.more.NativeMoreState
import com.saaf.milkshop.nativeui.more.NativeMoreViewModel
import com.saaf.milkshop.nativeui.operations.AttendanceScreen
import com.saaf.milkshop.nativeui.operations.DeliveriesScreen
import com.saaf.milkshop.nativeui.operations.MakeYogurtScreen
import com.saaf.milkshop.nativeui.operations.NativeAttendanceState
import com.saaf.milkshop.nativeui.operations.NativeAttendanceViewModel
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesViewModel
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtState
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtViewModel
import com.saaf.milkshop.nativeui.operations.NativeOperationsMoreDestination
import com.saaf.milkshop.nativeui.operations.NativeOperationsMoreState
import com.saaf.milkshop.nativeui.operations.NativeOperationsMoreViewModel
import com.saaf.milkshop.nativeui.operations.NativeOperationsWorkDestination
import com.saaf.milkshop.nativeui.operations.NativeOperationsWorkState
import com.saaf.milkshop.nativeui.operations.NativeOperationsWorkViewModel
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkState
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkViewModel
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageState
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageViewModel
import com.saaf.milkshop.nativeui.operations.OperationsMoreScreen
import com.saaf.milkshop.nativeui.operations.OperationsSection
import com.saaf.milkshop.nativeui.operations.OperationsWorkScreen
import com.saaf.milkshop.nativeui.operations.ReceiveMilkScreen
import com.saaf.milkshop.nativeui.operations.RecordSpoilageScreen
import com.saaf.milkshop.nativeui.owner.NativeOwnerOverviewState
import com.saaf.milkshop.nativeui.owner.NativeOwnerOverviewViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerWorkDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerWorkState
import com.saaf.milkshop.nativeui.owner.NativeOwnerWorkViewModel
import com.saaf.milkshop.nativeui.owner.OwnerOverviewScreen
import com.saaf.milkshop.nativeui.owner.OwnerSaleEntryScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.owner.OwnerWorkScreen
import com.saaf.milkshop.nativeui.sale.CashierSaleScreen
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.sale.NativeSaleViewModel
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.welcome.WelcomeScreen
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockDeviceGate
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockRecordStore
import com.saaf.milkshop.quickunlock.QuickUnlockService
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

class NativePreviewActivity : FragmentActivity() {
    private lateinit var applicationState: MilkShopApplication
    private lateinit var authenticationFacade: NativeAuthenticationFacade
    private lateinit var quickUnlockService: QuickUnlockService
    private lateinit var quickUnlockFacade: NativeQuickUnlockFacade
    private lateinit var saleFacade: NativeSaleFacade
    private lateinit var collectionFacade: NativeCollectionFacade
    private lateinit var activityFacade: NativeActivityFacade
    private lateinit var moreFacade: NativeMoreFacade
    private lateinit var operationsWorkFacade: NativeOperationsWorkFacade
    private lateinit var receiveMilkFacade: NativeReceiveMilkFacade
    private lateinit var makeYogurtFacade: NativeMakeYogurtFacade
    private lateinit var recordSpoilageFacade: NativeRecordSpoilageFacade
    private lateinit var deliveriesFacade: NativeDeliveriesFacade
    private lateinit var attendanceFacade: NativeAttendanceFacade
    private lateinit var operationsMoreFacade: NativeOperationsMoreFacade
    private lateinit var ownerOverviewFacade: NativeOwnerOverviewFacade
    private lateinit var ownerWorkFacade: NativeOwnerWorkFacade

    private val authenticationViewModel by viewModels<NativeAuthenticationViewModel> {
        val app = application as MilkShopApplication
        NativeAuthenticationViewModel.Factory(
            facade = authenticationFacade,
            onSessionChanged = { app.currentSessionId = it },
        )
    }

    private val saleViewModel by viewModels<NativeSaleViewModel> {
        NativeSaleViewModel.Factory(
            facade = saleFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val collectionViewModel by viewModels<NativeCollectionViewModel> {
        NativeCollectionViewModel.Factory(
            facade = collectionFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val activityViewModel by viewModels<NativeActivityViewModel> {
        NativeActivityViewModel.Factory(
            facade = activityFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val moreViewModel by viewModels<NativeMoreViewModel> {
        NativeMoreViewModel.Factory(
            facade = moreFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val operationsWorkViewModel by viewModels<NativeOperationsWorkViewModel> {
        NativeOperationsWorkViewModel.Factory(
            facade = operationsWorkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val receiveMilkViewModel by viewModels<NativeReceiveMilkViewModel> {
        NativeReceiveMilkViewModel.Factory(
            facade = receiveMilkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val makeYogurtViewModel by viewModels<NativeMakeYogurtViewModel> {
        NativeMakeYogurtViewModel.Factory(
            facade = makeYogurtFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val recordSpoilageViewModel by viewModels<NativeRecordSpoilageViewModel> {
        NativeRecordSpoilageViewModel.Factory(
            facade = recordSpoilageFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val deliveriesViewModel by viewModels<NativeDeliveriesViewModel> {
        NativeDeliveriesViewModel.Factory(
            facade = deliveriesFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val attendanceViewModel by viewModels<NativeAttendanceViewModel> {
        NativeAttendanceViewModel.Factory(
            facade = attendanceFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val operationsMoreViewModel by viewModels<NativeOperationsMoreViewModel> {
        NativeOperationsMoreViewModel.Factory(
            facade = operationsMoreFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val ownerOverviewViewModel by viewModels<NativeOwnerOverviewViewModel> {
        NativeOwnerOverviewViewModel.Factory(
            facade = ownerOverviewFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val ownerWorkViewModel by viewModels<NativeOwnerWorkViewModel> {
        NativeOwnerWorkViewModel.Factory(
            facade = ownerWorkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as MilkShopApplication
        applicationState = app
        authenticationFacade = NativeAuthenticationFacade(app.database, app.userSecurity)
        quickUnlockService = QuickUnlockService(
            database = app.database,
            security = app.userSecurity,
            store = AndroidQuickUnlockRecordStore(applicationContext),
            deviceGate = AndroidQuickUnlockDeviceGate(this),
        )
        quickUnlockFacade = NativeQuickUnlockFacade(quickUnlockService, authenticationFacade)
        saleFacade = NativeSaleFacade(app.database, app.userSecurity)
        collectionFacade = NativeCollectionFacade(app.database, app.userSecurity)
        activityFacade = NativeActivityFacade(app.database, app.userSecurity)
        moreFacade = NativeMoreFacade(app.database, app.userSecurity, quickUnlockService)
        operationsWorkFacade = NativeOperationsWorkFacade(app.database, app.userSecurity)
        receiveMilkFacade = NativeReceiveMilkFacade(app.database, app.userSecurity)
        makeYogurtFacade = NativeMakeYogurtFacade(app.database, app.userSecurity)
        recordSpoilageFacade = NativeRecordSpoilageFacade(app.database, app.userSecurity)
        deliveriesFacade = NativeDeliveriesFacade(app.database, app.userSecurity)
        attendanceFacade = NativeAttendanceFacade(app.database, app.userSecurity)
        operationsMoreFacade = NativeOperationsMoreFacade(app.database, app.userSecurity, quickUnlockService)
        ownerOverviewFacade = NativeOwnerOverviewFacade(
            database = app.database,
            security = app.userSecurity,
            cloudBackupSettings = app.cloudBackupSettings,
        )
        ownerWorkFacade = NativeOwnerWorkFacade(app.database, app.userSecurity)
        authenticationViewModel.attachQuickUnlock(quickUnlockFacade)
        val openOperator = intent.getBooleanExtra(EXTRA_OPEN_OPERATOR, false)
        val openOwner = intent.getBooleanExtra(EXTRA_OPEN_OWNER, false)
        setContent {
            SaafTheme {
                val authenticationState by authenticationViewModel.state.collectAsState()
                val saleState by saleViewModel.state.collectAsState()
                val collectionState by collectionViewModel.state.collectAsState()
                val activityState by activityViewModel.state.collectAsState()
                val moreState by moreViewModel.state.collectAsState()
                val operationsWorkState by operationsWorkViewModel.state.collectAsState()
                val receiveMilkState by receiveMilkViewModel.state.collectAsState()
                val makeYogurtState by makeYogurtViewModel.state.collectAsState()
                val recordSpoilageState by recordSpoilageViewModel.state.collectAsState()
                val deliveriesState by deliveriesViewModel.state.collectAsState()
                val attendanceState by attendanceViewModel.state.collectAsState()
                val operationsMoreState by operationsMoreViewModel.state.collectAsState()
                val ownerOverviewState by ownerOverviewViewModel.state.collectAsState()
                val ownerWorkState by ownerWorkViewModel.state.collectAsState()
                NativeFoundationPreview(
                    database = app.database,
                    security = app.userSecurity,
                    saleFacade = saleFacade,
                    collectionFacade = collectionFacade,
                    activityFacade = activityFacade,
                    authenticationState = authenticationState,
                    authenticationViewModel = authenticationViewModel,
                    saleState = saleState,
                    saleViewModel = saleViewModel,
                    collectionState = collectionState,
                    collectionViewModel = collectionViewModel,
                    activityState = activityState,
                    activityViewModel = activityViewModel,
                    moreState = moreState,
                    moreViewModel = moreViewModel,
                    operationsWorkState = operationsWorkState,
                    operationsWorkViewModel = operationsWorkViewModel,
                    receiveMilkState = receiveMilkState,
                    receiveMilkViewModel = receiveMilkViewModel,
                    makeYogurtState = makeYogurtState,
                    makeYogurtViewModel = makeYogurtViewModel,
                    recordSpoilageState = recordSpoilageState,
                    recordSpoilageViewModel = recordSpoilageViewModel,
                    deliveriesState = deliveriesState,
                    deliveriesViewModel = deliveriesViewModel,
                    attendanceState = attendanceState,
                    attendanceViewModel = attendanceViewModel,
                    operationsMoreState = operationsMoreState,
                    operationsMoreViewModel = operationsMoreViewModel,
                    ownerOverviewState = ownerOverviewState,
                    ownerOverviewViewModel = ownerOverviewViewModel,
                    ownerWorkState = ownerWorkState,
                    ownerWorkViewModel = ownerWorkViewModel,
                    openOperatorOnStart = openOperator,
                    openOwnerOnStart = openOwner,
                )
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_OPERATOR = "open_operator"
        const val EXTRA_OPEN_OWNER = "open_owner"
    }

    override fun onStart() {
        super.onStart()
        if (::applicationState.isInitialized) {
            applicationState.nativeSessions.onForeground()
            authenticationViewModel.validateAuthenticatedSession()
        }
    }

    override fun onStop() {
        if (::applicationState.isInitialized) applicationState.nativeSessions.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
        if (::quickUnlockFacade.isInitialized) authenticationViewModel.detachQuickUnlock(quickUnlockFacade)
        if (::quickUnlockService.isInitialized) quickUnlockService.close()
        super.onDestroy()
    }
}

private enum class PreviewDestination { Welcome, Access }

@Composable
private fun NativeFoundationPreview(
    database: MilkShopDatabase,
    security: UserSecurityService,
    saleFacade: NativeSaleFacade,
    collectionFacade: NativeCollectionFacade,
    activityFacade: NativeActivityFacade,
    authenticationState: NativeAuthenticationState,
    authenticationViewModel: NativeAuthenticationViewModel,
    saleState: NativeSaleState,
    saleViewModel: NativeSaleViewModel,
    collectionState: NativeCollectionState,
    collectionViewModel: NativeCollectionViewModel,
    activityState: NativeActivityState,
    activityViewModel: NativeActivityViewModel,
    moreState: NativeMoreState,
    moreViewModel: NativeMoreViewModel,
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
    ownerOverviewState: NativeOwnerOverviewState,
    ownerOverviewViewModel: NativeOwnerOverviewViewModel,
    ownerWorkState: NativeOwnerWorkState,
    ownerWorkViewModel: NativeOwnerWorkViewModel,
    openOperatorOnStart: Boolean = false,
    openOwnerOnStart: Boolean = false,
) {
    var destination by rememberSaveable {
        mutableStateOf(
            if (openOperatorOnStart || openOwnerOnStart) {
                PreviewDestination.Access
            } else {
                PreviewDestination.Welcome
            },
        )
    }
    var cashierSection by rememberSaveable { mutableStateOf(CashierSection.Sell) }
    var operationsSection by rememberSaveable { mutableStateOf(OperationsSection.Work) }
    var ownerSection by rememberSaveable { mutableStateOf(OwnerSection.Overview) }
    var activityReturnSection by rememberSaveable { mutableStateOf(CashierSection.Sell.name) }
    var wantOperator by rememberSaveable { mutableStateOf(openOperatorOnStart) }
    var wantOwner by rememberSaveable { mutableStateOf(openOwnerOnStart && !openOperatorOnStart) }
    var operatorSwitchInFlight by remember { mutableStateOf(false) }
    var ownerSwitchInFlight by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val sellNotice = stringResource(R.string.native_nav_sell_notice)

    androidx.compose.runtime.LaunchedEffect(authenticationState, wantOwner) {
        if (!wantOwner || ownerSwitchInFlight || wantOperator) return@LaunchedEffect
        when (val state = authenticationState) {
            is NativeAuthenticationState.Authenticated -> {
                if (state.identity.role == AppRole.OWNER) {
                    wantOwner = false
                    destination = PreviewDestination.Access
                    return@LaunchedEffect
                }
                ownerSwitchInFlight = true
                authenticationViewModel.openPreviewOwnerOverview()
            }
            is NativeAuthenticationState.AccountPicker,
            is NativeAuthenticationState.OwnerSetup,
            -> {
                ownerSwitchInFlight = true
                authenticationViewModel.openPreviewOwnerOverview()
            }
            is NativeAuthenticationState.Loading -> Unit
            is NativeAuthenticationState.RecoveryCode -> {
                authenticationViewModel.acknowledgeRecoveryCode()
            }
            else -> {
                ownerSwitchInFlight = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState) {
        if (authenticationState is NativeAuthenticationState.Authenticated &&
            authenticationState.identity.role == AppRole.OWNER
        ) {
            ownerSwitchInFlight = false
            wantOwner = false
        }
        val failedPicker = authenticationState as? NativeAuthenticationState.AccountPicker
        val failedOwner = authenticationState as? NativeAuthenticationState.OwnerSetup
        if (failedPicker?.error != null || failedOwner?.error != null) {
            ownerSwitchInFlight = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState, wantOperator) {
        if (!wantOperator || operatorSwitchInFlight) return@LaunchedEffect
        when (val state = authenticationState) {
            is NativeAuthenticationState.Authenticated -> {
                if (state.identity.role == AppRole.OPERATOR) {
                    wantOperator = false
                    destination = PreviewDestination.Access
                    return@LaunchedEffect
                }
                operatorSwitchInFlight = true
                authenticationViewModel.openPreviewOperatorWork()
            }
            is NativeAuthenticationState.AccountPicker,
            is NativeAuthenticationState.OwnerSetup,
            -> {
                operatorSwitchInFlight = true
                authenticationViewModel.openPreviewOperatorWork()
            }
            is NativeAuthenticationState.Loading -> Unit
            is NativeAuthenticationState.RecoveryCode -> {
                authenticationViewModel.acknowledgeRecoveryCode()
            }
            else -> {
                operatorSwitchInFlight = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState) {
        if (authenticationState is NativeAuthenticationState.Authenticated &&
            authenticationState.identity.role == AppRole.OPERATOR
        ) {
            operatorSwitchInFlight = false
            wantOperator = false
        }
        val failedPicker = authenticationState as? NativeAuthenticationState.AccountPicker
        val failedOwner = authenticationState as? NativeAuthenticationState.OwnerSetup
        if (failedPicker?.error != null || failedOwner?.error != null) {
            operatorSwitchInFlight = false
        }
    }

    fun showPreviewNotice(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    fun closeCashierShell() {
        saleViewModel.close()
        collectionViewModel.close()
        activityViewModel.close()
        moreViewModel.close()
    }

    fun closeOwnerWorkEntries() {
        saleViewModel.close()
        collectionViewModel.close()
        receiveMilkViewModel.close()
        makeYogurtViewModel.close()
        recordSpoilageViewModel.close()
        deliveriesViewModel.close()
    }

    fun leaveToWelcome() {
        closeCashierShell()
        operationsWorkViewModel.close()
        receiveMilkViewModel.close()
        makeYogurtViewModel.close()
        recordSpoilageViewModel.close()
        deliveriesViewModel.close()
        attendanceViewModel.close()
        operationsMoreViewModel.close()
        ownerOverviewViewModel.close()
        ownerWorkViewModel.close()
        authenticationViewModel.leaveAuthenticatedArea()
        destination = PreviewDestination.Welcome
        cashierSection = CashierSection.Sell
        operationsSection = OperationsSection.Work
        ownerSection = OwnerSection.Overview
    }

    fun leaveToAccountPicker() {
        closeCashierShell()
        operationsWorkViewModel.close()
        receiveMilkViewModel.close()
        makeYogurtViewModel.close()
        recordSpoilageViewModel.close()
        deliveriesViewModel.close()
        attendanceViewModel.close()
        operationsMoreViewModel.close()
        ownerOverviewViewModel.close()
        ownerWorkViewModel.close()
        authenticationViewModel.beginAccountSwitch()
        destination = PreviewDestination.Access
    }

    val showingAuthenticated = destination == PreviewDestination.Access &&
        authenticationState is NativeAuthenticationState.Authenticated
    val authenticatedRole = (authenticationState as? NativeAuthenticationState.Authenticated)
        ?.identity?.role
    val showingOperator = showingAuthenticated && authenticatedRole == AppRole.OPERATOR
    val showingOwner = showingAuthenticated && authenticatedRole == AppRole.OWNER
    val showingCashier = showingAuthenticated && !showingOperator && !showingOwner

    androidx.compose.runtime.LaunchedEffect(showingCashier, showingOperator, showingOwner) {
        if (!showingCashier && !showingOwner) closeCashierShell()
        if (!showingOperator && !showingOwner) {
            operationsWorkViewModel.close()
            receiveMilkViewModel.close()
            makeYogurtViewModel.close()
            recordSpoilageViewModel.close()
            deliveriesViewModel.close()
            attendanceViewModel.close()
            operationsMoreViewModel.close()
        }
        if (!showingOwner) {
            ownerOverviewViewModel.close()
            ownerWorkViewModel.close()
        }
    }

    SystemBarAppearance(
        useDarkIcons = when {
            destination == PreviewDestination.Welcome -> false
            else -> true
        },
    )

    when (destination) {
        PreviewDestination.Welcome -> WelcomeScreen(
            onContinue = { destination = PreviewDestination.Access },
        )
        PreviewDestination.Access -> {
            if (showingOwner) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                SaafTheme(darkTheme = false) {
                    var lastOwnerSession by rememberSaveable { mutableStateOf<String?>(null) }
                    var ownerSampleReady by remember(authenticated.sessionHandle) { mutableStateOf(false) }
                    fun returnOwnerWorkHome() {
                        closeOwnerWorkEntries()
                        ownerWorkViewModel.backToHome()
                    }
                    androidx.compose.runtime.LaunchedEffect(authenticated.sessionHandle) {
                        if (lastOwnerSession != null && lastOwnerSession != authenticated.sessionHandle) {
                            ownerSection = OwnerSection.Overview
                            ownerWorkViewModel.close()
                            closeOwnerWorkEntries()
                        }
                        lastOwnerSession = authenticated.sessionHandle
                        ownerSampleReady = false
                        val sample = runCatching {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                NativePreviewSampleData.ensureForOwnerOverview(
                                    database = database,
                                    security = security,
                                    sessionHandle = authenticated.sessionHandle,
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
                                snackbarHostState.showSnackbar(
                                    message = "Sample shop data ready for Owner Overview.",
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        }
                    }
                    androidx.compose.runtime.LaunchedEffect(
                        authenticated.sessionHandle,
                        ownerSampleReady,
                        ownerSection,
                    ) {
                        if (!ownerSampleReady) return@LaunchedEffect
                        when (ownerSection) {
                            OwnerSection.Overview -> {
                                closeOwnerWorkEntries()
                                ownerWorkViewModel.close()
                                ownerOverviewViewModel.close()
                                ownerOverviewViewModel.open(authenticated.sessionHandle)
                                ownerOverviewViewModel.refresh()
                            }
                            OwnerSection.Work -> {
                                ownerOverviewViewModel.close()
                                ownerWorkViewModel.open(authenticated.sessionHandle)
                            }
                            OwnerSection.Records,
                            OwnerSection.More,
                            -> {
                                closeOwnerWorkEntries()
                                ownerWorkViewModel.close()
                                ownerOverviewViewModel.close()
                            }
                        }
                    }
                    androidx.compose.runtime.LaunchedEffect(
                        authenticated.sessionHandle,
                        ownerSection,
                        (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination,
                    ) {
                        val workDestination =
                            (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination
                        when {
                            ownerSection != OwnerSection.Work -> closeOwnerWorkEntries()
                            workDestination == NativeOwnerWorkDestination.RecordSale -> {
                                collectionViewModel.close()
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.close()
                                deliveriesViewModel.close()
                                saleViewModel.open(authenticated.sessionHandle)
                            }
                            workDestination == NativeOwnerWorkDestination.RecordPayment -> {
                                saleViewModel.close()
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.close()
                                deliveriesViewModel.close()
                                collectionViewModel.open(authenticated.sessionHandle)
                            }
                            workDestination == NativeOwnerWorkDestination.ReceiveMilk -> {
                                saleViewModel.close()
                                collectionViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.close()
                                deliveriesViewModel.close()
                                receiveMilkViewModel.open(authenticated.sessionHandle)
                            }
                            workDestination == NativeOwnerWorkDestination.MakeYogurt -> {
                                saleViewModel.close()
                                collectionViewModel.close()
                                receiveMilkViewModel.close()
                                recordSpoilageViewModel.close()
                                deliveriesViewModel.close()
                                makeYogurtViewModel.open(authenticated.sessionHandle)
                            }
                            workDestination == NativeOwnerWorkDestination.RecordSpoilage -> {
                                saleViewModel.close()
                                collectionViewModel.close()
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                deliveriesViewModel.close()
                                recordSpoilageViewModel.open(authenticated.sessionHandle)
                            }
                            workDestination == NativeOwnerWorkDestination.Deliveries -> {
                                saleViewModel.close()
                                collectionViewModel.close()
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.close()
                                deliveriesViewModel.open(authenticated.sessionHandle)
                            }
                            else -> closeOwnerWorkEntries()
                        }
                    }
                    val ownerWorkDestination =
                        (ownerWorkState as? NativeOwnerWorkState.Ready)?.destination
                    val ownerEntryOwnsBack = ownerSection == OwnerSection.Work &&
                        ownerWorkDestination in setOf(
                            NativeOwnerWorkDestination.RecordSale,
                            NativeOwnerWorkDestination.ReceiveMilk,
                            NativeOwnerWorkDestination.MakeYogurt,
                            NativeOwnerWorkDestination.RecordSpoilage,
                            NativeOwnerWorkDestination.Deliveries,
                        )
                    BackHandler(enabled = !ownerEntryOwnsBack) {
                        when (ownerSection) {
                            OwnerSection.Overview -> leaveToWelcome()
                            OwnerSection.Work -> {
                                val ready = ownerWorkState as? NativeOwnerWorkState.Ready
                                if (ready != null &&
                                    ready.destination != NativeOwnerWorkDestination.Home
                                ) {
                                    returnOwnerWorkHome()
                                } else {
                                    leaveToWelcome()
                                }
                            }
                            else -> ownerSection = OwnerSection.Overview
                        }
                    }
                    val navigateOwner: (OwnerSection) -> Unit = { section ->
                        when (section) {
                            OwnerSection.Overview -> {
                                if (ownerSection == OwnerSection.Overview) {
                                    showPreviewNotice("Overview is already open.")
                                } else {
                                    closeOwnerWorkEntries()
                                    ownerWorkViewModel.close()
                                    ownerSection = OwnerSection.Overview
                                }
                            }
                            OwnerSection.Work -> {
                                if (ownerSection == OwnerSection.Work &&
                                    ownerWorkDestination == NativeOwnerWorkDestination.Home
                                ) {
                                    showPreviewNotice("Work is already open.")
                                } else {
                                    closeOwnerWorkEntries()
                                    ownerWorkViewModel.backToHome()
                                    ownerSection = OwnerSection.Work
                                }
                            }
                            OwnerSection.Records,
                            OwnerSection.More,
                            -> {
                                closeOwnerWorkEntries()
                                ownerWorkViewModel.close()
                                ownerSection = section
                            }
                        }
                    }
                    val navigateOwnerOpsEntry: (OperationsSection) -> Unit = { section ->
                        if (section == OperationsSection.Work) {
                            returnOwnerWorkHome()
                        } else {
                            showPreviewNotice("Return to Owner Work for other commands.")
                            returnOwnerWorkHome()
                        }
                    }
                    val navigateOwnerCashierEntry: (CashierSection) -> Unit = { section ->
                        if (section == CashierSection.Sell || section == CashierSection.Collect) {
                            Unit
                        } else {
                            showPreviewNotice("Return to Owner Work for other commands.")
                            returnOwnerWorkHome()
                        }
                    }
                    when {
                        ownerSection == OwnerSection.Work &&
                            ownerWorkDestination == NativeOwnerWorkDestination.RecordSale -> {
                            OwnerSaleEntryScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
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
                            CashierCollectionScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
                                state = collectionState,
                                onRetry = collectionViewModel::refresh,
                                onSelectCustomer = collectionViewModel::selectCustomer,
                                onRecordCollection = collectionViewModel::record,
                                onCollectAnother = collectionViewModel::collectAnother,
                                onViewRecord = {
                                    showPreviewNotice(
                                        "Owner Records comes next. Payment stays available from Work.",
                                    )
                                },
                                onLeaveConfirmed = { returnOwnerWorkHome() },
                                onNavigate = navigateOwnerCashierEntry,
                            )
                        }
                        ownerSection == OwnerSection.Work &&
                            ownerWorkDestination == NativeOwnerWorkDestination.ReceiveMilk -> {
                            ReceiveMilkScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
                                state = receiveMilkState,
                                onRetry = receiveMilkViewModel::refresh,
                                onSelectVendor = receiveMilkViewModel::selectVendor,
                                onOpenVendorPicker = receiveMilkViewModel::openVendorPicker,
                                onCloseVendorPicker = receiveMilkViewModel::closeVendorPicker,
                                onPreview = receiveMilkViewModel::preview,
                                onRecord = receiveMilkViewModel::record,
                                onReceiveAnother = receiveMilkViewModel::receiveAnother,
                                onBackToHome = { returnOwnerWorkHome() },
                                onNavigate = navigateOwnerOpsEntry,
                            )
                        }
                        ownerSection == OwnerSection.Work &&
                            ownerWorkDestination == NativeOwnerWorkDestination.MakeYogurt -> {
                            MakeYogurtScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
                                state = makeYogurtState,
                                onRetry = makeYogurtViewModel::refresh,
                                onRecord = makeYogurtViewModel::record,
                                onMakeAnother = makeYogurtViewModel::makeAnother,
                                onBackToHome = { returnOwnerWorkHome() },
                                onNavigate = navigateOwnerOpsEntry,
                            )
                        }
                        ownerSection == OwnerSection.Work &&
                            ownerWorkDestination == NativeOwnerWorkDestination.RecordSpoilage -> {
                            RecordSpoilageScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
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
                                onBackToHome = { returnOwnerWorkHome() },
                                onNavigate = navigateOwnerOpsEntry,
                            )
                        }
                        ownerSection == OwnerSection.Work &&
                            ownerWorkDestination == NativeOwnerWorkDestination.Deliveries -> {
                            DeliveriesScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
                                state = deliveriesState,
                                onRetry = deliveriesViewModel::refresh,
                                onMarkDelivered = deliveriesViewModel::markDelivered,
                                onNavigate = navigateOwnerOpsEntry,
                                onLeaveConfirmed = { returnOwnerWorkHome() },
                            )
                        }
                        ownerSection == OwnerSection.Work -> {
                            OwnerWorkScreen(
                                snackbarHostState = snackbarHostState,
                                state = ownerWorkState,
                                onRetry = ownerWorkViewModel::refresh,
                                onOpenDestination = ownerWorkViewModel::openDestination,
                                onBackToHome = { returnOwnerWorkHome() },
                                onLeaveConfirmed = { leaveToWelcome() },
                                onNavigate = navigateOwner,
                            )
                        }
                        else -> {
                            OwnerOverviewScreen(
                                snackbarHostState = snackbarHostState,
                                state = ownerOverviewState,
                                section = ownerSection,
                                onRetry = ownerOverviewViewModel::refresh,
                                onLeaveConfirmed = { leaveToWelcome() },
                                onNavigate = navigateOwner,
                                onAttentionReview = {
                                    showPreviewNotice(
                                        "Close, Records, and Backup destinations come next in the Owner family.",
                                    )
                                },
                            )
                        }
                    }
                }
            } else if (showingOperator) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                SaafTheme(darkTheme = false) {
                    var lastOpsSession by rememberSaveable { mutableStateOf<String?>(null) }
                    androidx.compose.runtime.LaunchedEffect(authenticated.sessionHandle) {
                        if (lastOpsSession != null && lastOpsSession != authenticated.sessionHandle) {
                            operationsSection = OperationsSection.Work
                        }
                        lastOpsSession = authenticated.sessionHandle
                    }
                    androidx.compose.runtime.LaunchedEffect(
                        authenticated.sessionHandle,
                        operationsSection,
                    ) {
                        when (operationsSection) {
                            OperationsSection.Work -> {
                                deliveriesViewModel.close()
                                attendanceViewModel.close()
                                operationsMoreViewModel.close()
                                operationsWorkViewModel.open(authenticated.sessionHandle)
                            }
                            OperationsSection.Deliveries -> {
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.close()
                                attendanceViewModel.close()
                                operationsMoreViewModel.close()
                                operationsWorkViewModel.backToHome()
                                deliveriesViewModel.open(authenticated.sessionHandle)
                            }
                            OperationsSection.Attendance -> {
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.close()
                                deliveriesViewModel.close()
                                operationsMoreViewModel.close()
                                operationsWorkViewModel.backToHome()
                                attendanceViewModel.open(authenticated.sessionHandle)
                            }
                            OperationsSection.More -> {
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.close()
                                deliveriesViewModel.close()
                                attendanceViewModel.close()
                                operationsWorkViewModel.backToHome()
                                operationsMoreViewModel.open(authenticated.sessionHandle)
                            }
                        }
                    }
                    androidx.compose.runtime.LaunchedEffect(
                        authenticated.sessionHandle,
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
                                receiveMilkViewModel.open(authenticated.sessionHandle)
                            }
                            destination == NativeOperationsWorkDestination.MakeYogurt -> {
                                receiveMilkViewModel.close()
                                recordSpoilageViewModel.close()
                                makeYogurtViewModel.open(authenticated.sessionHandle)
                            }
                            destination == NativeOperationsWorkDestination.RecordSpoilage -> {
                                receiveMilkViewModel.close()
                                makeYogurtViewModel.close()
                                recordSpoilageViewModel.open(authenticated.sessionHandle)
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
                                    leaveToWelcome()
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
                                    operationsSection = OperationsSection.Work
                                }
                            }
                            else -> operationsSection = OperationsSection.Work
                        }
                    }
                    val navigateOps: (OperationsSection) -> Unit = { section ->
                        when (section) {
                            OperationsSection.Work -> {
                                if (operationsSection == OperationsSection.Work) {
                                    showPreviewNotice("Work is already open.")
                                } else {
                                    operationsSection = OperationsSection.Work
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
                                operationsSection = section
                            }
                        }
                    }
                    val workReady = operationsWorkState as? NativeOperationsWorkState.Ready
                    when {
                        operationsSection == OperationsSection.Deliveries -> {
                            DeliveriesScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
                                state = deliveriesState,
                                onRetry = deliveriesViewModel::refresh,
                                onMarkDelivered = deliveriesViewModel::markDelivered,
                                onNavigate = navigateOps,
                                onLeaveConfirmed = { leaveToWelcome() },
                            )
                        }
                        operationsSection == OperationsSection.Attendance -> {
                            AttendanceScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
                                state = attendanceState,
                                onRetry = attendanceViewModel::refresh,
                                onMarkPresent = attendanceViewModel::markPresent,
                                onNavigate = navigateOps,
                                onLeaveConfirmed = { leaveToWelcome() },
                            )
                        }
                        operationsSection == OperationsSection.More -> {
                            OperationsMoreScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
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
                                onSwitchAccount = { leaveToAccountPicker() },
                                onSignOut = { leaveToWelcome() },
                                onNavigate = navigateOps,
                                onLeaveConfirmed = { leaveToWelcome() },
                            )
                        }
                        operationsSection == OperationsSection.Work &&
                            workReady?.destination == NativeOperationsWorkDestination.ReceiveMilk -> {
                            ReceiveMilkScreen(
                                snackbarHostState = snackbarHostState,
                                identity = authenticated.identity,
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
                                identity = authenticated.identity,
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
                                identity = authenticated.identity,
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
                                identity = authenticated.identity,
                                state = operationsWorkState,
                                section = operationsSection,
                                onRetry = operationsWorkViewModel::refresh,
                                onOpenReceiveMilk = {
                                    operationsWorkViewModel.openReceiveMilk()
                                    receiveMilkViewModel.open(authenticated.sessionHandle)
                                },
                                onOpenMakeYogurt = {
                                    operationsWorkViewModel.openMakeYogurt()
                                    makeYogurtViewModel.open(authenticated.sessionHandle)
                                },
                                onOpenRecordSpoilage = {
                                    operationsWorkViewModel.openRecordSpoilage()
                                    recordSpoilageViewModel.open(authenticated.sessionHandle)
                                },
                                onBackToHome = operationsWorkViewModel::backToHome,
                                onLeaveConfirmed = { leaveToWelcome() },
                                onNavigate = navigateOps,
                            )
                        }
                    }
                }
            } else if (showingCashier) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                // Counter Ledger Cashier surfaces stay on the approved light palette,
                // matching authentication and the Collect blueprint. System dark mode
                // must not invert Sell/Collect into a Material night theme.
                SaafTheme(darkTheme = false) {
                    var sampleReady by remember(authenticated.sessionHandle) { mutableStateOf(false) }
                    var lastCashierSession by rememberSaveable { mutableStateOf<String?>(null) }
                    androidx.compose.runtime.LaunchedEffect(authenticated.sessionHandle) {
                        if (lastCashierSession != null && lastCashierSession != authenticated.sessionHandle) {
                            cashierSection = CashierSection.Sell
                        }
                        lastCashierSession = authenticated.sessionHandle
                        sampleReady = false
                        val sample = runCatching {
                            NativePreviewSampleData.ensureForSession(
                                database = database,
                                security = security,
                                sessionHandle = authenticated.sessionHandle,
                                saleFacade = saleFacade,
                                collectionFacade = collectionFacade,
                                activityFacade = activityFacade,
                            )
                        }.getOrNull()
                        sampleReady = true
                        if (sample?.seededOperator == true) {
                            snackbarHostState.showSnackbar(
                                message = "Operator ready: ${NativePreviewSampleData.PREVIEW_OPERATOR_DISPLAY_NAME} " +
                                    "(${NativePreviewSampleData.PREVIEW_OPERATOR_LOGIN}). " +
                                    "More → Switch account, then full passphrase.",
                                duration = SnackbarDuration.Long,
                            )
                        } else if (sample?.changed == true) {
                            snackbarHostState.showSnackbar(
                                message = "Sample shop data ready for Sell, Collect, and Activity.",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                    androidx.compose.runtime.LaunchedEffect(
                        authenticated.sessionHandle,
                        cashierSection,
                        sampleReady,
                    ) {
                        if (!sampleReady) return@LaunchedEffect
                        when (cashierSection) {
                            CashierSection.Sell -> saleViewModel.open(authenticated.sessionHandle)
                            CashierSection.Collect -> collectionViewModel.open(authenticated.sessionHandle)
                            CashierSection.Activity -> Unit
                            CashierSection.More -> moreViewModel.open(authenticated.sessionHandle)
                        }
                    }
                    BackHandler {
                        when (cashierSection) {
                            CashierSection.Collect -> cashierSection = CashierSection.Sell
                            CashierSection.Activity -> {
                                val ready = activityState as? NativeActivityState.Ready
                                if (ready?.detail != null) {
                                    activityViewModel.clearDetail()
                                } else {
                                    val returnTo = runCatching {
                                        CashierSection.valueOf(activityReturnSection)
                                    }.getOrDefault(CashierSection.Sell)
                                    activityReturnSection = CashierSection.Sell.name
                                    cashierSection = if (returnTo == CashierSection.Collect) {
                                        CashierSection.Collect
                                    } else {
                                        CashierSection.Sell
                                    }
                                }
                            }
                            CashierSection.More -> {
                                val ready = moreState as? NativeMoreState.Ready
                                if (ready != null && ready.destination != NativeMoreDestination.Menu) {
                                    moreViewModel.backToMenu()
                                } else {
                                    moreViewModel.close()
                                    cashierSection = CashierSection.Sell
                                }
                            }
                            else -> leaveToWelcome()
                        }
                    }
                    val navigateCashier: (CashierSection) -> Unit = { section ->
                        when (section) {
                            CashierSection.Sell -> {
                                if (cashierSection == CashierSection.Sell) showPreviewNotice(sellNotice)
                                else cashierSection = CashierSection.Sell
                            }
                            CashierSection.Collect -> {
                                if (cashierSection == CashierSection.Collect) {
                                    showPreviewNotice("Collect is already open.")
                                } else {
                                    cashierSection = CashierSection.Collect
                                }
                            }
                            CashierSection.Activity -> {
                                if (cashierSection == CashierSection.Activity) {
                                    showPreviewNotice("Activity is already open.")
                                } else {
                                    activityReturnSection = CashierSection.Sell.name
                                    activityViewModel.open(authenticated.sessionHandle)
                                    cashierSection = CashierSection.Activity
                                }
                            }
                            CashierSection.More -> {
                                if (cashierSection == CashierSection.More) {
                                    showPreviewNotice("More is already open.")
                                } else {
                                    moreViewModel.open(authenticated.sessionHandle)
                                    cashierSection = CashierSection.More
                                }
                            }
                        }
                    }
                    when (cashierSection) {
                        CashierSection.Sell -> CashierSaleScreen(
                            snackbarHostState = snackbarHostState,
                            identity = authenticated.identity,
                            state = saleState,
                            onRetry = saleViewModel::refresh,
                            onClearFeedback = saleViewModel::clearFeedback,
                            onRecordSale = saleViewModel::record,
                            onLeaveConfirmed = { leaveToWelcome() },
                            onNavigate = navigateCashier,
                        )
                        CashierSection.Collect -> CashierCollectionScreen(
                            snackbarHostState = snackbarHostState,
                            identity = authenticated.identity,
                            state = collectionState,
                            onRetry = collectionViewModel::refresh,
                            onSelectCustomer = collectionViewModel::selectCustomer,
                            onRecordCollection = collectionViewModel::record,
                            onCollectAnother = collectionViewModel::collectAnother,
                            onViewRecord = { eventId ->
                                activityReturnSection = CashierSection.Collect.name
                                activityViewModel.openDetailFromCollect(authenticated.sessionHandle, eventId)
                                cashierSection = CashierSection.Activity
                            },
                            onLeaveConfirmed = { cashierSection = CashierSection.Sell },
                            onNavigate = navigateCashier,
                        )
                        CashierSection.Activity -> CashierActivityScreen(
                            snackbarHostState = snackbarHostState,
                            identity = authenticated.identity,
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
                                    cashierSection = CashierSection.Collect
                                } else {
                                    activityViewModel.clearDetail()
                                }
                            },
                            onLeaveConfirmed = {
                                activityReturnSection = CashierSection.Sell.name
                                cashierSection = CashierSection.Sell
                            },
                            onNavigate = navigateCashier,
                        )
                        CashierSection.More -> CashierMoreScreen(
                            snackbarHostState = snackbarHostState,
                            identity = authenticated.identity,
                            state = moreState,
                            onRetry = moreViewModel::refresh,
                            onOpenCountCash = moreViewModel::openCountCash,
                            onOpenPersonalSecurity = moreViewModel::openPersonalSecurity,
                            onBackToMenu = moreViewModel::backToMenu,
                            onRecordCashCount = moreViewModel::recordCashCount,
                            onRequestRemoveQuickUnlock = moreViewModel::requestRemoveQuickUnlock,
                            onCancelRemoveQuickUnlock = moreViewModel::cancelRemoveQuickUnlock,
                            onRemoveQuickUnlock = moreViewModel::removeQuickUnlock,
                            onEnrollQuickPin = moreViewModel::enrollQuickPin,
                            onEnrollPhoneSecurity = moreViewModel::enrollPhoneSecurity,
                            onSwitchAccount = { leaveToAccountPicker() },
                            onSignOut = { leaveToWelcome() },
                            onNavigate = navigateCashier,
                        )
                    }
                }
            } else {
                when (authenticationState) {
                    is NativeAuthenticationState.AccountPicker -> BackHandler {
                        if (authenticationState.cancelReturnsToSession) {
                            authenticationViewModel.cancelAccountSwitch()
                        } else {
                            destination = PreviewDestination.Welcome
                        }
                    }
                    is NativeAuthenticationState.MethodPicker -> BackHandler {
                        authenticationViewModel.chooseAnotherAccount()
                    }
                    is NativeAuthenticationState.QuickPin -> BackHandler {
                        authenticationViewModel.backFromQuickPin()
                    }
                    is NativeAuthenticationState.Passphrase -> BackHandler {
                        authenticationViewModel.backFromPassphrase()
                    }
                    is NativeAuthenticationState.RecoveryCode -> BackHandler(enabled = true) {}
                    else -> BackHandler {
                        if (authenticationViewModel.isAccountSwitchPending()) {
                            authenticationViewModel.cancelAccountSwitch()
                        } else {
                            destination = PreviewDestination.Welcome
                        }
                    }
                }
                NativeAuthenticationHost(
                    state = authenticationState,
                    onRetry = authenticationViewModel::refreshStartup,
                    onBackToWelcome = {
                        val picker = authenticationState as? NativeAuthenticationState.AccountPicker
                        if (picker?.cancelReturnsToSession == true) {
                            authenticationViewModel.cancelAccountSwitch()
                        } else {
                            destination = PreviewDestination.Welcome
                        }
                    },
                    onSelectAccount = authenticationViewModel::selectAccount,
                    onChooseAnotherAccount = authenticationViewModel::chooseAnotherAccount,
                    onBackFromPassphrase = authenticationViewModel::backFromPassphrase,
                    onUseQuickPin = authenticationViewModel::useQuickPin,
                    onUsePhoneSecurity = authenticationViewModel::signInWithPhoneSecurity,
                    onUseFullPassphrase = authenticationViewModel::useFullPassphrase,
                    onBackFromQuickPin = authenticationViewModel::backFromQuickPin,
                    onQuickPinSignIn = authenticationViewModel::signInWithQuickPin,
                    onSignIn = authenticationViewModel::signIn,
                    onBootstrapOwner = authenticationViewModel::bootstrapOwner,
                    onAcknowledgeRecoveryCode = authenticationViewModel::acknowledgeRecoveryCode,
                )
            }
        }
    }
}

@Composable
private fun SystemBarAppearance(useDarkIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}

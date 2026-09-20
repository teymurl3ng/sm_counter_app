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
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.nativeui.activity.CashierActivityScreen
import com.saaf.milkshop.nativeui.activity.NativeActivityState
import com.saaf.milkshop.nativeui.activity.NativeActivityViewModel
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationHost
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationState
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationViewModel
import com.saaf.milkshop.nativeui.collection.CashierCollectionScreen
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.collection.NativeCollectionViewModel
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
        authenticationViewModel.attachQuickUnlock(quickUnlockFacade)
        setContent {
            SaafTheme {
                val authenticationState by authenticationViewModel.state.collectAsState()
                val saleState by saleViewModel.state.collectAsState()
                val collectionState by collectionViewModel.state.collectAsState()
                val activityState by activityViewModel.state.collectAsState()
                NativeFoundationPreview(
                    authenticationState = authenticationState,
                    authenticationViewModel = authenticationViewModel,
                    saleState = saleState,
                    saleViewModel = saleViewModel,
                    collectionState = collectionState,
                    collectionViewModel = collectionViewModel,
                    activityState = activityState,
                    activityViewModel = activityViewModel,
                )
            }
        }
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
    authenticationState: NativeAuthenticationState,
    authenticationViewModel: NativeAuthenticationViewModel,
    saleState: NativeSaleState,
    saleViewModel: NativeSaleViewModel,
    collectionState: NativeCollectionState,
    collectionViewModel: NativeCollectionViewModel,
    activityState: NativeActivityState,
    activityViewModel: NativeActivityViewModel,
) {
    var destination by rememberSaveable { mutableStateOf(PreviewDestination.Welcome) }
    var cashierSection by rememberSaveable { mutableStateOf(CashierSection.Sell) }
    var activityReturnSection by rememberSaveable { mutableStateOf(CashierSection.Sell.name) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val sellNotice = stringResource(R.string.native_nav_sell_notice)
    val moreNotice = stringResource(R.string.native_nav_more_notice)

    fun showPreviewNotice(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    val showingCashier = destination == PreviewDestination.Access &&
        authenticationState is NativeAuthenticationState.Authenticated

    androidx.compose.runtime.LaunchedEffect(showingCashier) {
        if (!showingCashier) {
            cashierSection = CashierSection.Sell
            saleViewModel.close()
            collectionViewModel.close()
            activityViewModel.close()
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
            if (showingCashier) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                // Counter Ledger Cashier surfaces stay on the approved light palette,
                // matching authentication and the Collect blueprint. System dark mode
                // must not invert Sell/Collect into a Material night theme.
                SaafTheme(darkTheme = false) {
                    androidx.compose.runtime.LaunchedEffect(authenticated.sessionHandle, cashierSection) {
                        when (cashierSection) {
                            CashierSection.Sell -> saleViewModel.open(authenticated.sessionHandle)
                            CashierSection.Collect -> collectionViewModel.open(authenticated.sessionHandle)
                            CashierSection.Activity,
                            CashierSection.More,
                            -> Unit
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
                            else -> {
                                saleViewModel.close()
                                collectionViewModel.close()
                                activityViewModel.close()
                                authenticationViewModel.leaveAuthenticatedArea()
                                destination = PreviewDestination.Welcome
                            }
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
                            CashierSection.More -> showPreviewNotice(moreNotice)
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
                            onLeaveConfirmed = {
                                saleViewModel.close()
                                collectionViewModel.close()
                                activityViewModel.close()
                                authenticationViewModel.leaveAuthenticatedArea()
                                destination = PreviewDestination.Welcome
                            },
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
                        CashierSection.More -> Unit
                    }
                }
            } else {
                when (authenticationState) {
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
                    else -> BackHandler { destination = PreviewDestination.Welcome }
                }
                NativeAuthenticationHost(
                    state = authenticationState,
                    onRetry = authenticationViewModel::refreshStartup,
                    onBackToWelcome = { destination = PreviewDestination.Welcome },
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

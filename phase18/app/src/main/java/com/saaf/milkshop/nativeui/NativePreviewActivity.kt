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
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.saaf.milkshop.application.authentication.NativeAuthenticationFacade
import com.saaf.milkshop.application.authentication.NativeQuickUnlockFacade
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationHost
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationState
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationViewModel
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
        authenticationViewModel.attachQuickUnlock(quickUnlockFacade)
        setContent {
            SaafTheme {
                val authenticationState by authenticationViewModel.state.collectAsState()
                val saleState by saleViewModel.state.collectAsState()
                NativeFoundationPreview(
                    authenticationState = authenticationState,
                    authenticationViewModel = authenticationViewModel,
                    saleState = saleState,
                    saleViewModel = saleViewModel,
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
) {
    var destination by rememberSaveable { mutableStateOf(PreviewDestination.Welcome) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val sellNotice = stringResource(R.string.native_nav_sell_notice)
    val collectNotice = stringResource(R.string.native_nav_collect_notice)
    val activityNotice = stringResource(R.string.native_nav_activity_notice)
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

    SystemBarAppearance(
        useDarkIcons = when {
            destination == PreviewDestination.Welcome -> false
            !showingCashier -> true
            else -> !isSystemInDarkTheme()
        },
    )

    when (destination) {
        PreviewDestination.Welcome -> WelcomeScreen(
            onContinue = { destination = PreviewDestination.Access },
        )
        PreviewDestination.Access -> {
            if (showingCashier) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                androidx.compose.runtime.LaunchedEffect(authenticated.sessionHandle) {
                    saleViewModel.open(authenticated.sessionHandle)
                }
                BackHandler {
                    saleViewModel.close()
                    authenticationViewModel.leaveAuthenticatedArea()
                    destination = PreviewDestination.Welcome
                }
                CashierSaleScreen(
                    snackbarHostState = snackbarHostState,
                    identity = authenticated.identity,
                    state = saleState,
                    onRetry = saleViewModel::refresh,
                    onClearFeedback = saleViewModel::clearFeedback,
                    onRecordSale = saleViewModel::record,
                    onLeaveConfirmed = {
                        saleViewModel.close()
                        authenticationViewModel.leaveAuthenticatedArea()
                        destination = PreviewDestination.Welcome
                    },
                    onNavigate = { section ->
                        showPreviewNotice(
                            when (section) {
                                CashierSection.Sell -> sellNotice
                                CashierSection.Collect -> collectNotice
                                CashierSection.Activity -> activityNotice
                                CashierSection.More -> moreNotice
                            },
                        )
                    },
                )
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

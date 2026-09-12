package com.saaf.milkshop.hybrid

import android.app.AlertDialog
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.insets.GradientProtection
import androidx.core.view.insets.ProtectionLayout
import androidx.webkit.WebViewFeature
import androidx.fragment.app.FragmentActivity
import com.saaf.milkshop.BuildConfig
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.R
import com.saaf.milkshop.application.bridge.BackupDocumentPicker
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.cloud.HybridGoogleDriveAuthorization
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockDeviceGate
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockRecordStore
import com.saaf.milkshop.quickunlock.QuickUnlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class HybridActivity : FragmentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var applicationState: MilkShopApplication
    private lateinit var bridgeDispatcher: HybridBridgeDispatcher
    private lateinit var webViewHost: SecureHybridWebViewHost
    private var ownedBackupDocumentPicker: HybridBackupDocumentPicker? = null
    private var evaluatingBackPress = false
    private var activeArea: String? = null
    private var rendererRecoveryVisible = false
    private var discardDraftDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationState = application as MilkShopApplication
        activeArea = savedInstanceState?.getString(STATE_ACTIVE_AREA)
        configureSystemBars()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            showFatalError("This phone's Android WebView must be updated before Saaf Milk can open safely.")
            return
        }

        val backupDocuments = testBackupDocumentPicker() ?: HybridBackupDocumentPicker(this).also {
            ownedBackupDocumentPicker = it
        }
        val quickUnlock = QuickUnlockService(
            database = applicationState.database,
            security = applicationState.userSecurity,
            store = AndroidQuickUnlockRecordStore(applicationContext),
            deviceGate = AndroidQuickUnlockDeviceGate(this),
        )
        bridgeDispatcher = HybridBridgeDispatcher(
            database = applicationState.database,
            security = applicationState.userSecurity,
            onSessionChanged = { applicationState.currentSessionId = it },
            backupManager = applicationState.backupManager,
            backupDocuments = backupDocuments,
            application = applicationState,
            cloudAuthorization = HybridGoogleDriveAuthorization(this),
            quickUnlockService = quickUnlock,
        )
        webViewHost = SecureHybridWebViewHost(
            activity = this,
            scope = activityScope,
            dispatch = bridgeDispatcher::dispatch,
            onDisplayFailure = ::showDisplayRecovery,
            restorationState = if (savedInstanceState != null) {
                applicationState.currentSessionId?.let { HybridRestorationState(it, activeArea) }
            } else {
                null
            },
            onActiveAreaChanged = { activeArea = it },
        )
        setContentView(createProtectedWebContent())
        webViewHost.load()
        installBackHandler()
    }

    override fun onStart() {
        super.onStart()
        if (::applicationState.isInitialized) applicationState.nativeSessions.onForeground()
    }

    override fun onResume() {
        super.onResume()
        if (::webViewHost.isInitialized) {
            webViewHost.webView.post {
                webViewHost.webView.evaluateJavascript(
                    "window.dispatchEvent(new Event('saafMilkAppResumed'));",
                    null,
                )
            }
        }
    }

    override fun onStop() {
        if (::applicationState.isInitialized) applicationState.nativeSessions.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
        discardDraftDialog?.dismiss()
        ownedBackupDocumentPicker?.close()
        if (::bridgeDispatcher.isInitialized) bridgeDispatcher.close()
        activityScope.cancel()
        if (::webViewHost.isInitialized) webViewHost.close()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        activeArea?.let { outState.putString(STATE_ACTIVE_AREA, it) }
        super.onSaveInstanceState(outState)
    }

    fun webViewForTesting(): WebView = webViewHost.webView
    fun activeAreaForTesting(): String? = activeArea
    fun rendererRecoveryVisibleForTesting(): Boolean = rendererRecoveryVisible
    fun discardDraftDialogVisibleForTesting(): Boolean = discardDraftDialog?.isShowing == true
    fun keepDraftForTesting(): Boolean =
        discardDraftDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.performClick() ?: false
    fun discardDraftForTesting(): Boolean =
        discardDraftDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.performClick() ?: false

    private fun installBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (rendererRecoveryVisible) {
                    moveTaskToBack(true)
                    return
                }
                if (evaluatingBackPress || !::webViewHost.isInitialized) return
                evaluatingBackPress = true
                webViewHost.webView.evaluateJavascript(BACK_HANDLER_SCRIPT) { result ->
                    evaluatingBackPress = false
                    when (result) {
                        "\"root\"" -> moveTaskToBack(true)
                        "\"confirm_discard\"" -> confirmDiscardDraft()
                    }
                }
            }
        })
    }

    private fun confirmDiscardDraft() {
        if (discardDraftDialog?.isShowing == true || isFinishing || isDestroyed) return
        discardDraftDialog = AlertDialog.Builder(this)
            .setTitle("Discard unfinished changes?")
            .setMessage("The entries on this form have not been saved.")
            .setNegativeButton("Keep editing") { _, _ -> clearPendingDiscard() }
            .setPositiveButton("Discard") { _, _ ->
                webViewHost.webView.evaluateJavascript(
                    "window.__saafMilkPendingDiscard?.(); delete window.__saafMilkPendingDiscard;",
                    null,
                )
            }
            .setOnCancelListener { clearPendingDiscard() }
            .create()
            .also { it.show() }
    }

    private fun clearPendingDiscard() {
        if (::webViewHost.isInitialized) {
            webViewHost.webView.evaluateJavascript("delete window.__saafMilkPendingDiscard;", null)
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
    }

    private fun createProtectedWebContent(): ProtectionLayout =
        ProtectionLayout(this).apply {
            setProtections(
                listOf(
                    GradientProtection(
                        WindowInsetsCompat.Side.TOP,
                        ContextCompat.getColor(this@HybridActivity, R.color.hybrid_brand_header),
                    ),
                ),
            )
            addView(webViewHost.webView)
        }

    private fun testBackupDocumentPicker(): BackupDocumentPicker? =
        if (BuildConfig.DEBUG) backupDocumentPickerForTesting else null

    private fun showFatalError(message: String) {
        rendererRecoveryVisible = false
        configureFailureSystemBars()
        setContentView(createHybridFailureView(
            context = this,
            title = "Saaf Milk cannot open",
            message = message,
        ))
    }

    private fun showDisplayRecovery(message: String) {
        if (isFinishing || isDestroyed || rendererRecoveryVisible) return
        rendererRecoveryVisible = true
        evaluatingBackPress = false
        configureFailureSystemBars()
        setContentView(
            createHybridFailureView(
                context = this,
                title = "The app display needs to restart",
                message = "$message Your saved shop records were not erased. Reloading will securely check your login again before showing protected information.",
                recoveryAction = {
                    rendererRecoveryVisible = false
                    recreate()
                },
            ),
        )
    }

    private fun configureFailureSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        const val BRIDGE_CHANNEL = SecureHybridWebViewHost.BRIDGE_CHANNEL
        const val APP_HOST = SecureHybridWebViewHost.APP_HOST
        const val APP_ORIGIN = SecureHybridWebViewHost.APP_ORIGIN
        const val HYBRID_ENTRY_URL = SecureHybridWebViewHost.HYBRID_ENTRY_URL
        @Volatile var backupDocumentPickerForTesting: BackupDocumentPicker? = null
        private const val STATE_ACTIVE_AREA = "active_management_area"

        private const val BACK_HANDLER_SCRIPT = """
            (() => {
              const visible = (element) => {
                const style = window.getComputedStyle(element);
                return style.display !== 'none' && style.visibility !== 'hidden' && element.getClientRects().length > 0;
              };
              const visibleClosers = Array.from(document.querySelectorAll('[data-native-back-close]')).filter(visible);
              const closer = visibleClosers.at(-1);
              const scope = closer?.closest('[role="dialog"]') ?? document;
              const dirty = Array.from(scope.querySelectorAll('form[data-saaf-dirty="true"]')).some(visible);
              const state = window.history.state;
              const canReturnToPriorArea = typeof state?.saafAreaDepth === 'number' && state.saafAreaDepth > 0;
              const performBack = () => {
                if (closer) {
                  closer.click();
                  return true;
                }
                if (canReturnToPriorArea) {
                  window.history.back();
                  return true;
                }
                return false;
              };
              if (dirty && (closer || canReturnToPriorArea)) {
                window.__saafMilkPendingDiscard = performBack;
                return 'confirm_discard';
              }
              return performBack() ? 'handled' : 'root';
            })()
        """
    }
}

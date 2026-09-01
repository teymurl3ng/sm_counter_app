package com.saaf.milkshop.hybrid

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewFeature
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.BuildConfig
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.application.bridge.BackupDocumentPicker
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class HybridActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var applicationState: MilkShopApplication
    private lateinit var bridgeDispatcher: HybridBridgeDispatcher
    private lateinit var webViewHost: SecureHybridWebViewHost
    private var ownedBackupDocumentPicker: HybridBackupDocumentPicker? = null
    private var evaluatingBackPress = false
    private var activeArea: String? = null

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
        bridgeDispatcher = HybridBridgeDispatcher(
            database = applicationState.database,
            security = applicationState.userSecurity,
            onSessionChanged = { applicationState.currentSessionId = it },
            backupManager = BackupManager(this),
            backupDocuments = backupDocuments,
        )
        webViewHost = SecureHybridWebViewHost(
            activity = this,
            scope = activityScope,
            dispatch = bridgeDispatcher::dispatch,
            onFatalError = ::showFatalError,
            restorationState = if (savedInstanceState != null) {
                applicationState.currentSessionId?.let { HybridRestorationState(it, activeArea) }
            } else {
                null
            },
            onActiveAreaChanged = { activeArea = it },
        )
        setContentView(webViewHost.webView)
        webViewHost.load()
        installBackHandler()
    }

    override fun onStart() {
        super.onStart()
        if (::applicationState.isInitialized) applicationState.nativeSessions.onForeground()
    }

    override fun onStop() {
        if (::applicationState.isInitialized) applicationState.nativeSessions.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
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

    private fun installBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (evaluatingBackPress || !::webViewHost.isInitialized) return
                evaluatingBackPress = true
                webViewHost.webView.evaluateJavascript(BACK_HANDLER_SCRIPT) { result ->
                    evaluatingBackPress = false
                    if (result == "\"root\"") moveTaskToBack(true)
                }
            }
        })
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
    }

    private fun testBackupDocumentPicker(): BackupDocumentPicker? =
        if (BuildConfig.DEBUG) backupDocumentPickerForTesting else null

    private fun showFatalError(message: String) {
        setContentView(
            TextView(this).apply {
                text = message
                textSize = 18f
                setTextColor(Color.rgb(30, 41, 59))
                setBackgroundColor(Color.rgb(247, 246, 240))
                setPadding(48, 96, 48, 48)
            },
        )
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
              const visibleClosers = Array.from(document.querySelectorAll('[data-native-back-close]')).filter((element) => {
                const style = window.getComputedStyle(element);
                return style.display !== 'none' && style.visibility !== 'hidden' && element.getClientRects().length > 0;
              });
              const closer = visibleClosers.at(-1);
              if (closer) {
                closer.click();
                return 'handled';
              }
              const activeArea = document.querySelector('[data-app-area-active="true"]');
              const firstPrimaryArea = document.querySelector('#primary-navigation [data-app-area]');
              if (activeArea && firstPrimaryArea && activeArea !== firstPrimaryArea) {
                firstPrimaryArea.click();
                return 'handled';
              }
              return 'root';
            })()
        """
    }
}

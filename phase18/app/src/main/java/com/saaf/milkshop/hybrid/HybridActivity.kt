package com.saaf.milkshop.hybrid

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.saaf.milkshop.BuildConfig
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.application.bridge.BackupDocumentPicker
import com.saaf.milkshop.application.bridge.BridgeCommandConflict
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HybridActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var webView: WebView
    private lateinit var applicationState: MilkShopApplication
    private lateinit var bridgeDispatcher: HybridBridgeDispatcher
    private var pendingCreateDocument: CompletableDeferred<Uri?>? = null
    private var pendingOpenDocument: CompletableDeferred<Uri?>? = null
    private val createBackupDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        pendingCreateDocument?.complete(uri)
    }
    private val openBackupDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingOpenDocument?.complete(uri)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationState = application as MilkShopApplication
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            showFatalError("This phone's Android WebView must be updated before Saaf Milk can open safely.")
            return
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        bridgeDispatcher = HybridBridgeDispatcher(
            database = applicationState.database,
            security = applicationState.userSecurity,
            onSessionChanged = { applicationState.currentSessionId = it },
            backupManager = BackupManager(this),
            backupDocuments = if (BuildConfig.DEBUG && backupDocumentPickerForTesting != null) requireNotNull(backupDocumentPickerForTesting) else object : BackupDocumentPicker {
                override suspend fun createDocument(suggestedFileName: String?): Uri? = pickBackupDestination(suggestedFileName)
                override suspend fun openDocument(): Uri? = pickBackupSource()
            },
        )

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.rgb(247, 246, 240))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mediaPlaybackRequiresUserGesture = true
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    return !isPackagedOrigin(uri)
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    view.destroy()
                    showFatalError("The app display stopped unexpectedly. Close and reopen Saaf Milk; your saved records are safe.")
                    return true
                }
            }
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        WebViewCompat.addWebMessageListener(
            webView,
            BRIDGE_CHANNEL,
            setOf(APP_ORIGIN),
        ) { _, message, sourceOrigin, isMainFrame, replyProxy ->
            if (!isMainFrame || sourceOrigin.toString() != APP_ORIGIN) return@addWebMessageListener
            val raw = message.data ?: return@addWebMessageListener
            activityScope.launch {
                val response = withContext(Dispatchers.IO) { bridgeDispatcher.dispatch(raw) }
                replyProxy.postMessage(response)
            }
        }
        setContentView(webView)
        webView.loadUrl(HYBRID_ENTRY_URL)
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
        pendingCreateDocument?.complete(null)
        pendingOpenDocument?.complete(null)
        if (::bridgeDispatcher.isInitialized) bridgeDispatcher.close()
        activityScope.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
        super.onDestroy()
    }

    fun webViewForTesting(): WebView = webView

    private suspend fun pickBackupDestination(suggestedFileName: String?): Uri? = withContext(Dispatchers.Main.immediate) {
        if (pendingCreateDocument != null) throw BridgeCommandConflict("A backup file picker is already open")
        val result = CompletableDeferred<Uri?>()
        pendingCreateDocument = result
        try {
            createBackupDocument.launch(suggestedFileName ?: BackupManager.displayName(this@HybridActivity))
            result.await()
        } finally {
            pendingCreateDocument = null
        }
    }

    private suspend fun pickBackupSource(): Uri? = withContext(Dispatchers.Main.immediate) {
        if (pendingOpenDocument != null) throw BridgeCommandConflict("A backup file picker is already open")
        val result = CompletableDeferred<Uri?>()
        pendingOpenDocument = result
        try {
            openBackupDocument.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
            result.await()
        } finally {
            pendingOpenDocument = null
        }
    }

    private fun showFatalError(message: String) {
        setContentView(
            TextView(this).apply {
                text = message
                textSize = 18f
                setTextColor(Color.rgb(30, 41, 59))
                setBackgroundColor(Color.rgb(247, 246, 240))
                setPadding(48, 96, 48, 48)
            }
        )
    }

    private fun isPackagedOrigin(uri: Uri): Boolean =
        uri.scheme == "https" &&
            uri.host == APP_HOST &&
            uri.port == -1 &&
            uri.path?.startsWith("/assets/hybrid/") == true

    companion object {
        const val BRIDGE_CHANNEL = "saafMilkNative"
        const val APP_HOST = "appassets.androidplatform.net"
        const val APP_ORIGIN = "https://$APP_HOST"
        const val HYBRID_ENTRY_URL = "$APP_ORIGIN/assets/hybrid/index.html"
        @Volatile var backupDocumentPickerForTesting: BackupDocumentPicker? = null
    }
}

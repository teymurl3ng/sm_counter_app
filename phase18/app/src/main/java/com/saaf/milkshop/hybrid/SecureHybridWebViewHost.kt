package com.saaf.milkshop.hybrid

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.saaf.milkshop.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class HybridRestorationState(
    val sessionHandle: String,
    val activeArea: String?,
)

internal class SecureHybridWebViewHost(
    activity: ComponentActivity,
    private val scope: CoroutineScope,
    private val dispatch: suspend (String) -> String,
    private val onFatalError: (String) -> Unit,
    private val restorationState: HybridRestorationState?,
    private val onActiveAreaChanged: (String) -> Unit,
) : AutoCloseable {
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(activity))
        .build()
    private val restorationScript = restorationState?.let(::restorationScript)
    private var documentStartScript: androidx.webkit.ScriptHandler? = null
    private lateinit var windowInsetsPublisher: HybridWindowInsetsPublisher

    @SuppressLint("SetJavaScriptEnabled")
    val webView = WebView(activity).apply {
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

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !isPackagedOrigin(request.url)

            override fun onPageFinished(view: WebView, url: String) {
                if (documentStartScript == null && url == HYBRID_ENTRY_URL) {
                    restorationScript?.let { view.evaluateJavascript(it, null) }
                }
                if (url == HYBRID_ENTRY_URL) windowInsetsPublisher.onPageReady()
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                view.destroy()
                onFatalError("The app display stopped unexpectedly. Close and reopen Saaf Milk; your saved records are safe.")
                return true
            }
        }
    }

    init {
        windowInsetsPublisher = HybridWindowInsetsPublisher(webView)
        windowInsetsPublisher.attach()
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        if (restorationScript != null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            documentStartScript = WebViewCompat.addDocumentStartJavaScript(webView, restorationScript, setOf(APP_ORIGIN))
        }
        WebViewCompat.addWebMessageListener(
            webView,
            BRIDGE_CHANNEL,
            setOf(APP_ORIGIN),
        ) { _, message, sourceOrigin, isMainFrame, replyProxy ->
            if (!isMainFrame || sourceOrigin.toString() != APP_ORIGIN) return@addWebMessageListener
            val raw = message.data ?: return@addWebMessageListener
            scope.launch {
                val response = withContext(Dispatchers.IO) { dispatch(raw) }
                replyProxy.postMessage(response)
            }
        }
        WebViewCompat.addWebMessageListener(
            webView,
            LIFECYCLE_CHANNEL,
            setOf(APP_ORIGIN),
        ) { _, message, sourceOrigin, isMainFrame, _ ->
            if (!isMainFrame || sourceOrigin.toString() != APP_ORIGIN) return@addWebMessageListener
            val activeArea = runCatching {
                JSONObject(message.data ?: return@addWebMessageListener).getString("activeArea")
            }.getOrNull()
            if (activeArea in RESTORABLE_AREAS) onActiveAreaChanged(requireNotNull(activeArea))
        }
    }

    fun load() = webView.loadUrl(HYBRID_ENTRY_URL)

    override fun close() {
        windowInsetsPublisher.close()
        documentStartScript?.remove()
        documentStartScript = null
        webView.stopLoading()
        webView.webViewClient = WebViewClient()
        webView.destroy()
    }

    private fun isPackagedOrigin(uri: Uri): Boolean =
        uri.scheme == "https" &&
            uri.host == APP_HOST &&
            uri.port == -1 &&
            uri.path?.startsWith("/assets/hybrid/") == true

    private fun restorationScript(state: HybridRestorationState): String {
        val area = state.activeArea?.let(JSONObject::quote) ?: "null"
        return """
            window.__saafMilkRestoration = Object.freeze({
              sessionHandle: ${JSONObject.quote(state.sessionHandle)},
              activeArea: $area
            });
            window.dispatchEvent(new Event('saafMilkRestorationReady'));
        """.trimIndent()
    }

    companion object {
        const val BRIDGE_CHANNEL = "saafMilkNative"
        const val LIFECYCLE_CHANNEL = "saafMilkLifecycle"
        const val APP_HOST = "appassets.androidplatform.net"
        const val APP_ORIGIN = "https://$APP_HOST"
        const val HYBRID_ENTRY_URL = "$APP_ORIGIN/assets/hybrid/index.html"
        private val RESTORABLE_AREAS = setOf(
            "dashboard", "operations", "deliveries", "accounts", "history", "cash-close",
            "products", "customers", "suppliers", "employees", "costing-planning",
            "settings", "access", "backup",
        )
    }
}

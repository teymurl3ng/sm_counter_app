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
import com.saaf.milkshop.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SecureHybridWebViewHost(
    activity: ComponentActivity,
    private val scope: CoroutineScope,
    private val dispatch: suspend (String) -> String,
    private val onFatalError: (String) -> Unit,
) : AutoCloseable {
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(activity))
        .build()

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

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                view.destroy()
                onFatalError("The app display stopped unexpectedly. Close and reopen Saaf Milk; your saved records are safe.")
                return true
            }
        }
    }

    init {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
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
    }

    fun load() = webView.loadUrl(HYBRID_ENTRY_URL)

    override fun close() {
        webView.stopLoading()
        webView.webViewClient = WebViewClient()
        webView.destroy()
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
    }
}

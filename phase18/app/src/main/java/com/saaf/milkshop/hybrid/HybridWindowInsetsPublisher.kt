package com.saaf.milkshop.hybrid

import android.webkit.WebView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

internal class HybridWindowInsetsPublisher(
    private val webView: WebView,
) : AutoCloseable {
    private val density = webView.resources.displayMetrics.density
    private var state = State()

    fun attach() {
        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            state = State(
                systemTopCssPx = systemBars.top.toCssPx(),
                systemBottomCssPx = systemBars.bottom.toCssPx(),
                imeBottomCssPx = ime.bottom.toCssPx(),
                imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime()) && ime.bottom > systemBars.bottom,
            )
            publish()
            insets
        }
        webView.post { ViewCompat.requestApplyInsets(webView) }
    }

    fun onPageReady() {
        publish()
        ViewCompat.requestApplyInsets(webView)
    }

    override fun close() {
        ViewCompat.setOnApplyWindowInsetsListener(webView, null)
    }

    private fun publish() {
        val snapshot = state
        webView.evaluateJavascript(
            """
                (() => {
                  const root = document.documentElement;
                  if (!root) return false;
                  root.style.setProperty('--saaf-system-top', '${snapshot.systemTopCssPx}px');
                  root.style.setProperty('--saaf-system-bottom', '${snapshot.systemBottomCssPx}px');
                  root.style.setProperty('--saaf-ime-bottom', '${snapshot.imeBottomCssPx}px');
                  root.dataset.imeVisible = '${snapshot.imeVisible}';
                  window.dispatchEvent(new CustomEvent('saafMilkInsetsChanged'));
                  return true;
                })()
            """.trimIndent(),
            null,
        )
    }

    private fun Int.toCssPx(): Int = (this / density).roundToInt()

    private data class State(
        val systemTopCssPx: Int = 0,
        val systemBottomCssPx: Int = 0,
        val imeBottomCssPx: Int = 0,
        val imeVisible: Boolean = false,
    )
}

package com.saaf.milkshop.nativeui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.saaf.milkshop.R
import com.saaf.milkshop.nativeui.sale.CashierSaleScreen
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

class NativePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SaafTheme(darkTheme = false) {
                NativeFoundationPreview()
            }
        }
    }
}

private enum class PreviewDestination { Welcome, CashierSale }

@Composable
private fun NativeFoundationPreview() {
    var destination by rememberSaveable { mutableStateOf(PreviewDestination.Welcome) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val previewNotice = stringResource(R.string.native_preview_notice)
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

    SystemBarAppearance(useDarkIcons = destination != PreviewDestination.Welcome)

    when (destination) {
        PreviewDestination.Welcome -> WelcomeScreen(
            onContinue = { destination = PreviewDestination.CashierSale },
        )
        PreviewDestination.CashierSale -> {
            BackHandler { destination = PreviewDestination.Welcome }
            CashierSaleScreen(
                snackbarHostState = snackbarHostState,
                onRecordSale = { showPreviewNotice(previewNotice) },
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

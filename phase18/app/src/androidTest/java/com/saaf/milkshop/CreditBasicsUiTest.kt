package com.saaf.milkshop

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class CreditBasicsUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context = NativeSaleContext("2026-10-15", true,
        listOf(NativeSaleProduct(1, "Milk", "liters", 19000, "100 L")),
        listOf(NativeSaleAccount(7, "Ahmed Store", phone = "+92 300 1234567", areaRoute = "North"),
            NativeSaleAccount(8, "Ahmed Store", phone = "+92 311 7654321", areaRoute = "South")),
        listOf(NativeSaleAccount(10, "Cash")), 10, 0, 0, 50.0)
    private val request = NativeSaleRequest(1, NativeSaleEntryMode.AMOUNT, 180.0,
        NativeSalePaymentKind.CUSTOMER_ACCOUNT, null, 7, surchargeEnabled = true, surchargeAmountRs = 50.0)
    private val summary = NativeSaleCreditSummary(7, "Ahmed Store", 80000, 100000, 103000, 23000,
        CreditAge(true, 5), false, "one-reviewed-snapshot")
    private val identity = NativeSignedInIdentity(1, "synthetic.owner", "Synthetic Owner", AppRole.OWNER,
        RolePermissions.permissions(AppRole.OWNER))
    private var state by mutableStateOf(NativeSaleState.Ready(context))
    private var recorded: NativeSaleRequest? = null
    private fun content(scale: Float = 1f, dark: Boolean = false) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                SaafTheme(darkTheme = dark) {
                    Surface {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.testTag("credit_header")) {
                            CashierScreenHeader("Sale", "Synthetic shop", "Owner", "2026-10-15", true)
                        }
                        CashierSaleScreen(remember { SnackbarHostState() }, identity, state,
                            onRetry = {}, onClearFeedback = { state = state.copy(creditWarning = null) },
                            onRecordSale = { recorded = it }, onSendAsDelivery = {}, onStartNextSale = {},
                            onNavigate = {}, modifier = Modifier.weight(1f))
                        Box(Modifier.testTag("credit_nav")) {
                            CashierNavigation(CashierSection.Sell) {}
                        }
                    }
                    }
                }
            }
        }
    }
    @Test fun searchNamePhoneNoMatchClearAndCancelPreserveSelection() {
        content()
        rule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        val search = rule.onNodeWithTag(NativeUiTags.SaleCreditSearch)
        search.performTextInput("  aHmEd  ")
        rule.onNodeWithTag(NativeUiTags.cashierCustomer(7)).assertExists()
        rule.onNodeWithTag(NativeUiTags.cashierCustomer(8)).assertExists()
        rule.onNodeWithText("Customer #8", substring = true).assertExists()
        search.performTextReplacement("0300-does-not-match")
        rule.onNodeWithText("No customers match that search.").assertExists()
        search.performTextReplacement("+92 (311) 765-4321")
        rule.onNodeWithTag(NativeUiTags.cashierCustomer(7)).assertDoesNotExist()
        search.performImeAction()
        rule.onNodeWithTag(NativeUiTags.cashierCustomer(8)).performClick()
        rule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).performScrollTo().performClick()
        rule.onNodeWithTag(NativeUiTags.SaleCreditSearch).performTextInput("no-match")
        rule.onNodeWithTag(NativeUiTags.SaleCreditSearchClear).performClick()
        rule.onNodeWithTag(NativeUiTags.cashierCustomer(8)).assertIsSelected()
        rule.onNodeWithTag(NativeUiTags.SaleCreditSearch).performImeAction()
        rule.onNodeWithTag(NativeUiTags.CashierCustomerCancel).performClick()
        rule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).assertExists()
        assertNull(recorded)
        capture("search-selected")
    }
    @Test fun creditLightDefaultSummaryAndWarningKeepChromeStable() = summaryAndWarning(1f, false)
    @Test fun creditDarkDoubleTextSummaryAndWarningKeepChromeStable() = summaryAndWarning(2f, true)
    private fun summaryAndWarning(scale: Float, dark: Boolean) {
        content(scale, dark)
        val header = rule.onNodeWithTag("credit_header").fetchSemanticsNode().boundsInRoot
        val nav = rule.onNodeWithTag("credit_nav").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        rule.onNodeWithTag(NativeUiTags.cashierCustomer(7)).performClick()
        rule.runOnIdle { state = state.copy(creditLoading = true) }
        rule.onNodeWithText("Checking credit…").assertExists()
        rule.runOnIdle { state = state.copy(creditLoading = false, creditSummary = summary) }
        rule.onNodeWithText("Oldest unpaid sale: 5 days.").performScrollTo().assertIsDisplayed()
        assertEquals(header, rule.onNodeWithTag("credit_header").fetchSemanticsNode().boundsInRoot)
        assertEquals(nav, rule.onNodeWithTag("credit_nav").fetchSemanticsNode().boundsInRoot)
        capture("summary-$scale-$dark")
        rule.runOnIdle { state = state.copy(creditWarning = NativeSaleCreditWarning(request, summary)) }
        rule.onNodeWithTag(NativeUiTags.SaleCreditWarningContinue).assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithText("This sale: PKR 230").assertExists()
        capture("warning-$scale-$dark")
        rule.onNodeWithTag(NativeUiTags.SaleCreditWarningBack).performClick()
        assertNull(recorded)
        rule.onNodeWithTag(NativeUiTags.SaleCreditWarning).assertDoesNotExist()
        rule.runOnIdle { state = state.copy(creditWarning = NativeSaleCreditWarning(request, summary)) }
        rule.onNodeWithTag(NativeUiTags.SaleCreditWarningContinue).performClick()
        assertEquals(request.copy(creditAcknowledgment = summary.warningKey), recorded)
    }
    @Test fun reviewTotalIncludesSurchargeForPaidNow() {
        content()
        rule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextInput("2")
        com.saaf.milkshop.qa.NativeLayoutImeEvidence.dismissImeAndWait(rule.activity, rule)
        rule.onNodeWithTag(NativeUiTags.CashierSurchargeToggle).performScrollTo().performClick()
        rule.onAllNodesWithText("PKR 430").assertCountEquals(2)
        rule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().performClick()
        assertEquals(2.0, recorded!!.enteredValue, 0.0)
        assertEquals(50.0, recorded!!.surchargeAmountRs!!, 0.0)
    }
    private fun capture(name: String) {
        // Compose idleness does not wait for Android's separate dialog-window animation.
        androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
        android.os.SystemClock.sleep(350)
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "credit-basics").apply { mkdirs() }
        assertTrue(androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .takeScreenshot(File(dir, "$name.png")))
    }
}

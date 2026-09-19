package com.saaf.milkshop

import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.domain.SettingKeys
import com.saaf.milkshop.domain.AnalyticsService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class NativeFoundationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<NativePreviewActivity>()

    @Test
    fun welcomeAccountPickerAndFullPassphraseOpenCashierSale() {
        openCashier()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertIsSelected()
    }

    @Test
    fun accessBackAccountChangeAndAuthenticationErrorFollowTheirVisibleContracts() {
        openAccountPicker()
        composeRule.onNodeWithText("Owner · Quick PIN, full passphrase").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthBackToWelcome).performClick()
        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
        openAccountPicker()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
        openAccountPicker()

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthPhoneSecurityMethod).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthMethodBack).performClick()
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthChooseAnotherAccount).performClick()
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()
        composeRule.onNodeWithText("Enter Quick PIN").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinBack).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()
        composeRule.onNodeWithText("Enter Quick PIN").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinUsePassphrase).performClick()
        composeRule.onNodeWithText("Welcome back, Instrumentation Owner").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseBack).performClick()
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthChooseAnotherAccount).performClick()
        waitForText("Who is using Saaf?")

        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithText("Welcome back, Instrumentation Owner").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Unlock Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement("incorrect full passphrase")
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()

        waitForText("The login name or passphrase is incorrect, or this account is unavailable.")
        composeRule.onNodeWithText("Welcome back, Instrumentation Owner").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithText(
            "The login name or passphrase is incorrect, or this account is unavailable.",
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        waitForText("New sale")
    }

    @Test
    fun quickPinUnlocksTheSelectedAccount() {
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PIN)
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).performClick()
        waitForText("New sale")
    }

    @Test
    fun quickPinRejectsAnotherAccountsPinAndUnlocksTheSelectedAccount() {
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_CASHIER_PIN)
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).performClick()
        waitForText(
            "That Quick PIN did not unlock Instrumentation Owner. " +
                "Check the selected account or use the full passphrase.",
        )
        composeRule.onNodeWithText("Enter Quick PIN").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PIN)
        composeRule.onNodeWithText(
            "That Quick PIN did not unlock Instrumentation Owner. " +
                "Check the selected account or use the full passphrase.",
        ).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).performClick()
        try {
            waitForText("New sale")
        } catch (timeout: Throwable) {
            val visibleError = runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthError).printToString()
            }.getOrElse { "No tagged authentication error was visible." }
            throw AssertionError(
                "Correct selected-account PIN did not open Cashier. $visibleError",
                timeout,
            )
        }
    }

    @Test
    fun cashierSystemBackEndsSessionBeforeReturningToAccountPicker() {
        openCashier()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            assertNull((activity.application as MilkShopApplication).currentSessionId)
        }

        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Who is using Saaf?")
    }

    @Test
    fun productButtonsSwitchProductRateUnitAndCalculatedEffects() {
        openCashier()
        val milkRate = retailRate(SettingKeys.RETAIL_RATE_MILK)
        val yogurtRate = retailRate(SettingKeys.RETAIL_RATE_YOGURT)

        composeRule.onNodeWithTag(NativeUiTags.CashierProductMilk).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierProductYogurt).performClick().assertIsSelected()
        composeRule.onNodeWithText("At PKR ${formatAmount(yogurtRate)} / kg").assertIsDisplayed()
        composeRule.onNodeWithText("Cash +PKR ${formatAmount(yogurtRate * 2)}").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierProductMilk).performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithText("At PKR ${formatAmount(milkRate)} / L").assertIsDisplayed()
        composeRule.onNodeWithText("Cash +PKR ${formatAmount(milkRate * 2)}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun entryModesInputAndSteppersConvertAndRecalculate() {
        openCashier()
        val milkRate = retailRate(SettingKeys.RETAIL_RATE_MILK)
        val threeUnits = formatInput(milkRate * 3)

        composeRule.onNodeWithTag(NativeUiTags.CashierModeAmount).performClick().assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals(formatInput(milkRate * 2))
        composeRule.onNodeWithText("PKR").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement(threeUnits)
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals(threeUnits)
        composeRule.onNodeWithTag(NativeUiTags.CashierIncrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals(formatInput(milkRate * 3 + 50))
        composeRule.onNodeWithTag(NativeUiTags.CashierDecrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals(threeUnits)

        composeRule.onNodeWithTag(NativeUiTags.CashierModeQuantity).performClick().assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("3.0")
        composeRule.onNodeWithTag(NativeUiTags.CashierDecrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("2.5")
        composeRule.onNodeWithTag(NativeUiTags.CashierIncrease).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("3.0")
    }

    @Test
    fun saleEntryDoneActionClearsKeyboardFocus() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput)
            .performClick()
            .performImeAction()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertIsNotFocused()
    }

    @Test
    fun editedSaleDraftSurvivesActivityRecreation() {
        openCashier()
        val ahmedId = customerId("Ahmed Store")

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        selectCustomer(ahmedId)
        composeRule.onNodeWithTag(NativeUiTags.CashierModeAmount).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement("725")
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performImeAction()
        composeRule.onNodeWithTag(NativeUiTags.CashierSecondaryDetails).performScrollTo().performClick()

        composeRule.activityRule.scenario.recreate()
        waitForText("New sale")

        composeRule.onNodeWithTag(NativeUiTags.CashierModeAmount).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("725")
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().assertIsSelected()
        composeRule.onNodeWithText("Ahmed Store").assertIsDisplayed()
        composeRule.onNodeWithText("Counter pickup").assertIsDisplayed()
    }

    @Test
    fun foregroundValidationRemovesAnInvalidatedSessionBeforeSaleReturns() {
        openCashier()
        val application = composeRule.activity.application as MilkShopApplication

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        application.nativeSessions.invalidateAll()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        waitForText("Who is using Saaf?")
        composeRule.onNodeWithText("Your session expired. Sign in again.").assertIsDisplayed()
        composeRule.onNodeWithText("New sale").assertDoesNotExist()
        assertNull(application.currentSessionId)
    }

    @Test
    fun nativeSaleRemainsUsableInLandscape() {
        openCashier()
        try {
            composeRule.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            SystemClock.sleep(1_000)
            waitForText("New sale")
            composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput)
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale)
                .performScrollTo()
                .assertIsDisplayed()
        } finally {
            composeRule.activityRule.scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            SystemClock.sleep(500)
        }
    }

    @Test
    fun recordSaleRequiresAPositiveEntry() {
        openCashier()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement("")
        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement("1")
        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().assertIsEnabled()
    }

    @Test
    fun customerPaymentOpensPickerAndPaidNowRestoresCashEffect() {
        openCashier()
        val ahmedId = customerId("Ahmed Store")
        val total = formatAmount(retailRate(SettingKeys.RETAIL_RATE_MILK) * 2)

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        selectCustomer(ahmedId)

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).assertIsDisplayed()
        composeRule.onNodeWithText("Customer due +PKR $total").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentPaidNow).performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithText("Cash +PKR $total").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).assertDoesNotExist()
    }

    @Test
    fun customerPickerCancelPreservesPaidNowAndBothAccountsCanBeSelected() {
        openCashier()
        val ahmedId = customerId("Ahmed Store")
        val bilalId = customerId("Bilal Dairy Point")

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerCancel).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentPaidNow).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).assertIsNotSelected()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performClick()
        selectCustomer(bilalId)
        composeRule.onNodeWithText("Bilal Dairy Point").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerSelector).performClick()
        selectCustomer(ahmedId)
        composeRule.onNodeWithText("Ahmed Store").assertIsDisplayed()
    }

    @Test
    fun secondaryDetailsToggleShowsAndHidesItsDefinedContent() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierSecondaryDetails).performScrollTo().performClick()
        composeRule.onNodeWithText("Counter pickup").assertIsDisplayed()
        composeRule.onNodeWithText("None").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierSecondaryDetails).performClick()
        composeRule.onNodeWithText("Counter pickup").assertDoesNotExist()
        composeRule.onNodeWithText("None").assertDoesNotExist()
    }

    @Test
    fun everyBottomDestinationProvidesItsDefinedPreviewResponse() {
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).performClick()
        composeRule.onNodeWithText("Sell is already open.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).performClick()
        composeRule.onNodeWithText("Collect is not connected in this native preview yet.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).performClick()
        composeRule.onNodeWithText("Activity is not connected in this native preview yet.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).performClick()
        composeRule.onNodeWithText("More is not connected in this native preview yet.").assertIsDisplayed()
    }

    @Test
    fun recordSaleWritesRoomAndShowsRefreshedReceipt() = runBlocking {
        val app = app()
        val vendorId = VendorManagementService(app.database).create("Native UI stock ${System.nanoTime()}")
        VendorPurchaseService(app.database).record(
            VendorPurchaseInput(vendorId, LocalDate.now().toString(), 10.0, 4.0, 28.0),
        )
        val priorEventId = app.database.eventDao().findAllDescending().maxOfOrNull { it.eventId } ?: 0L
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("Record sale").fetchSemanticsNodes().isNotEmpty() &&
                runCatching {
                    composeRule.onNodeWithTag(NativeUiTags.CashierSaleSuccess).fetchSemanticsNode()
                }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierSaleSuccess).assertIsDisplayed()

        val sale = app.database.eventDao().findAllDescending().single {
            it.eventId > priorEventId && it.eventType == "SALE"
        }
        val product = app.database.productDao().findByName("Milk")!!
        val cash = app.database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
        assertEquals(-2.0, app.database.inventoryLogDao().findByEventId(sale.eventId).single().quantity, 0.000001)
        assertEquals(product.productId, app.database.inventoryLogDao().findByEventId(sale.eventId).single().productId)
        assertEquals(cash.accountId, app.database.financialLogDao().findByEventId(sale.eventId).single().accountId)
        assertEquals(retailRate(SettingKeys.RETAIL_RATE_MILK) * 2, app.database.financialLogDao().findByEventId(sale.eventId).single().amount, 0.000001)
        val actor = app.database.eventActorLinkDao().find(sale.eventId)
        assertNotNull(actor)
        assertEquals("Instrumentation Owner", actor!!.actorNameSnapshot)
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("2.0")
        Unit
    }

    @Test
    fun customerAccountSaleWritesTheSelectedCustomerBalance() = runBlocking {
        val app = app()
        val vendorId = VendorManagementService(app.database).create("Native credit stock ${System.nanoTime()}")
        VendorPurchaseService(app.database).record(
            VendorPurchaseInput(vendorId, LocalDate.now().toString(), 5.0, 4.0, 28.0),
        )
        val customerId = customerId("Ahmed Store")
        val priorEventId = app.database.eventDao().findAllDescending().maxOfOrNull { it.eventId } ?: 0L
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierPaymentCustomer).performScrollTo().performClick()
        selectCustomer(customerId)
        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().performClick()
        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onNodeWithTag(NativeUiTags.CashierSaleSuccess).fetchSemanticsNode() }.isSuccess
        }

        val sale = app.database.eventDao().findAllDescending().single {
            it.eventId > priorEventId && it.eventType == "SALE"
        }
        val movement = app.database.financialLogDao().findByEventId(sale.eventId).single()
        assertEquals(customerId, movement.accountId)
        assertEquals(retailRate(SettingKeys.RETAIL_RATE_MILK) * 2, movement.amount, 0.000001)
    }

    @Test
    fun insufficientStockKeepsDraftVisibleAndWritesNoSale() = runBlocking {
        val app = app()
        val stock = AnalyticsService(app.database).currentPosition().stock
            .single { it.name == "Milk" }.quantityOnHand
        require(stock < 999_999_999.0) { "Sale test fixture unexpectedly exceeds the maximum UI entry" }
        val impossibleQuantity = "999999999"
        val saleIdsBefore = app.database.eventDao().findAllDescending()
            .filter { it.eventType == "SALE" }.map { it.eventId }.toSet()
        openCashier()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement(impossibleQuantity)
        composeRule.onNodeWithTag(NativeUiTags.CashierRecordSale).performScrollTo().performClick()
        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onNodeWithTag(NativeUiTags.CashierSaleError).fetchSemanticsNode() }.isSuccess
        }

        composeRule.onNodeWithText("There is not enough Milk in stock for this sale.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals(impossibleQuantity)
        val newSaleIds = app.database.eventDao().findAllDescending()
            .filter { it.eventType == "SALE" && it.eventId !in saleIdsBefore }
        assertEquals(emptyList<com.saaf.milkshop.data.EventEntity>(), newSaleIds)
    }

    @Test
    fun editedDraftRequiresExplicitBackChoice() {
        openCashier()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement("3.5")
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Discard this sale draft?").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierKeepDraft).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("3.5")

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag(NativeUiTags.CashierDiscardDraft).performClick()
        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
    }

    private fun openCashier() {
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        waitForText("New sale")
    }

    private fun openAccountPicker() {
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Who is using Saaf?")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun app(): MilkShopApplication =
        composeRule.activity.application as MilkShopApplication

    private fun retailRate(key: String): Double = runBlocking {
        requireNotNull(app().database.settingsDao().getValue(key))
    }

    private fun customerId(name: String): Long = runBlocking {
        app().database.accountDao().findByCategory("Customer").single {
            it.name.removePrefix("Customer: ").trim() == name
        }.accountId
    }

    private fun selectCustomer(accountId: Long) {
        val tag = NativeUiTags.cashierCustomer(accountId)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(NativeUiTags.CashierCustomerList)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierCustomerList)
            .performScrollToNode(hasTestTag(tag))
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun formatAmount(value: Double): String = when {
        value % 1.0 == 0.0 -> "%,.0f".format(Locale.US, value)
        else -> "%,.2f".format(Locale.US, value)
    }

    private fun formatInput(value: Double): String = when {
        value % 1.0 == 0.0 -> "%.0f".format(Locale.US, value)
        else -> "%.2f".format(Locale.US, value)
    }
}

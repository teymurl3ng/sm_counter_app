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
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.domain.SettingKeys
import com.saaf.milkshop.domain.AnalyticsService
import com.saaf.milkshop.domain.CustomerManagementService
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
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).assertIsDisplayed()
    }

    @Test
    fun quickPinUnlocksTheSelectedAccount() {
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinMethod).performClick()

        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PIN)
        composeRule.onNodeWithTag(NativeUiTags.AuthQuickPinSubmit).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).assertIsDisplayed()
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
            composeRule.waitUntil(timeoutMillis = 15_000) {
                runCatching {
                    composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).fetchSemanticsNode()
                }.isSuccess
            }
        } catch (timeout: Throwable) {
            val visibleError = runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthError).printToString()
            }.getOrElse { "No tagged authentication error was visible." }
            throw AssertionError(
                "Correct selected-account PIN did not open Owner Overview. $visibleError",
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
    fun operationsWorkHomeShowsPhysicalContext() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkContext).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertIsSelected()
        composeRule.onNodeWithText("no money", substring = true).assertIsDisplayed()
    }

    @Test
    fun operationsNavPreviewDestinationsShowExplicitNotices() {
        openOperationsWork()

        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMoreRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertIsSelected()
    }

    @Test
    fun operationsMoreOpensConnectedEntry() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMoreRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreTodayWork).assertIsDisplayed()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertIsSelected()
    }

    @Test
    fun operationsMoreSignsOutToWelcome() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMoreSignOut).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsMoreSignOut).performClick()
        waitForText("Continue")
    }

    @Test
    fun ownerOverviewOpensConnectedEntry() {
        openOwnerOverview()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewSales).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertIsSelected()
    }

    @Test
    fun ownerOverviewPreviewNavShowsExplicitNotices() {
        openOwnerOverview()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertIsSelected()
    }

    @Test
    fun ownerWorkOpensConnectedEntry() {
        openOwnerOverview()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordSale).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkPrimaryActions).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
    }

    @Test
    fun ownerWorkExpenseShowsExplicitPreview() {
        openOwnerOverview()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordExpense).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerWorkExpensePreview).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("not connected yet", substring = true).assertIsDisplayed()
    }

    @Test
    fun ownerRecordsOpensUnderOwnerChrome() {
        openOwnerRecords()

        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsSearch).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavRecords).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShellRoot).assertIsDisplayed()
        assertOwnerShellWithoutForeignNav()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).assertDoesNotExist()
    }

    @Test
    fun ownerMoreOpensUnderOwnerChrome() {
        openOwnerMore()

        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreProducts).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMorePersonalSecurity)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
    }

    @Test
    fun ownerMoreSignsOutToWelcome() {
        openOwnerMore()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSignOut)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).assertIsDisplayed()
    }

    @Test
    fun ownerProductsOpensUnderOwnerChrome() {
        openOwnerProducts()

        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun ownerProductsHardwareBackReturnsToMoreMenu() {
        openOwnerProducts()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
        }
    }

    @Test
    fun ownerCustomersOpensUnderOwnerChrome() {
        openOwnerCustomers()

        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun ownerCustomersHardwareBackReturnsToMoreMenu() {
        openOwnerCustomers()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
        }
    }

    @Test
    fun ownerSuppliersOpensUnderOwnerChrome() {
        openOwnerSuppliers()

        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun ownerSuppliersHardwareBackReturnsToMoreMenu() {
        openOwnerSuppliers()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
        }
    }

    @Test
    fun ownerEmployeesOpensUnderOwnerChrome() {
        openOwnerEmployees()

        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun ownerEmployeesHardwareBackReturnsToMoreMenu() {
        openOwnerEmployees()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
        }
    }

    @Test
    fun ownerWorkRecordSaleOpensUnderOwnerChrome() {
        openOwnerSale()

        composeRule.onNodeWithTag(NativeUiTags.OwnerSaleEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
    }

    @Test
    fun ownerWorkRecordSaleHardwareBackReturnsToWork() {
        openOwnerSale()

        pressHardwareBack()
        assertReturnedToOwnerWorkHome()
    }

    @Test
    fun ownerWorkRecordSaleDirtyBackRequiresDiscard() {
        openOwnerSale()
        composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput)
            .performScrollTo()
            .performTextReplacement("3.0")

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("Discard this sale draft?").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSaleEntryRoot).assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierDiscardDraft).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).assertIsDisplayed()
    }

    @Test
    fun ownerWorkRecordPaymentOpensUnderOwnerChrome() {
        openOwnerCollection()

        composeRule.onNodeWithTag(NativeUiTags.OwnerCollectionEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
    }

    @Test
    fun ownerWorkRecordPaymentHardwareBackReturnsToWork() {
        openOwnerCollection()
        pressHardwareBack()
        assertReturnedToOwnerWorkHome()
    }

    @Test
    fun ownerWorkReceiveMilkOpensUnderOwnerChrome() {
        openOwnerReceiveMilk()

        composeRule.onNodeWithTag(NativeUiTags.OwnerReceiveMilkEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
    }

    @Test
    fun ownerWorkReceiveMilkHardwareBackReturnsToWork() {
        openOwnerReceiveMilk()
        pressHardwareBack()
        assertReturnedToOwnerWorkHome()
    }

    @Test
    fun ownerWorkMakeYogurtOpensUnderOwnerChrome() {
        openOwnerMakeYogurt()

        composeRule.onNodeWithTag(NativeUiTags.OwnerMakeYogurtEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
    }

    @Test
    fun ownerWorkMakeYogurtHardwareBackReturnsToWork() {
        openOwnerMakeYogurt()
        pressHardwareBack()
        assertReturnedToOwnerWorkHome()
    }

    @Test
    fun ownerWorkRecordSpoilageOpensUnderOwnerChrome() {
        openOwnerRecordSpoilage()

        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordSpoilageEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
    }

    @Test
    fun ownerWorkRecordSpoilageHardwareBackReturnsToWork() {
        openOwnerRecordSpoilage()
        pressHardwareBack()
        assertReturnedToOwnerWorkHome()
    }

    @Test
    fun ownerWorkDeliveriesOpensUnderOwnerChrome() {
        openOwnerDeliveries()

        composeRule.onNodeWithTag(NativeUiTags.OwnerDeliveriesEntryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
        assertOwnerShellWithoutForeignNav()
        // Regression: Owner Deliveries must not host Ops manager chrome / sign-out path.
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
    }

    @Test
    fun ownerWorkDeliveriesHardwareBackReturnsToWork() {
        openOwnerDeliveries()
        pressHardwareBack()
        assertReturnedToOwnerWorkHome()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).assertDoesNotExist()
    }

    @Test
    fun operationsAttendanceOpensConnectedEntry() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).assertIsSelected()
    }

    @Test
    fun operationsAttendanceMarksPresent() {
        openOperationsWork()
        composeRule.activityRule.scenario.onActivity { activity ->
            val app = activity.application as MilkShopApplication
            kotlinx.coroutines.runBlocking {
                com.saaf.milkshop.application.preview.NativePreviewSampleData
                    .ensurePreviewOperatorEmployeeLink(
                        app.database,
                        loginName = SaafMilkTestRunner.TEST_OPERATOR_LOGIN,
                    )
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarkPresent).fetchSemanticsNode()
            }.isSuccess ||
                runCatching {
                    composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarked).fetchSemanticsNode()
                }.isSuccess
        }
        if (runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarkPresent).fetchSemanticsNode()
            }.isSuccess
        ) {
            composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarkPresent).performClick()
        }
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsAttendanceMarked).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Today:", substring = true).assertIsDisplayed()
    }

    @Test
    fun operationsDeliveriesOpensConnectedEntry() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).assertIsSelected()
    }

    @Test
    fun operationsDeliveriesMarksPhysicalDelivery() {
        openOperationsWork()
        composeRule.activityRule.scenario.onActivity { activity ->
            val app = activity.application as MilkShopApplication
            kotlinx.coroutines.runBlocking {
                com.saaf.milkshop.application.preview.NativePreviewSampleData
                    .ensureSamplePendingDeliveries(app.database)
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onAllNodesWithText("Delivered", substring = false)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onAllNodesWithText("Delivered", substring = false)[0].performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesSuccess).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Marked delivered", substring = true).assertIsDisplayed()
    }

    @Test
    fun operationsReceiveMilkOpensConnectedEntry() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilk).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkForm).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkReview).assertIsDisplayed()
        composeRule.onNodeWithText("No money", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkBack).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
    }

    @Test
    fun operationsReceiveMilkRecordsPhysicalReceipt() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilk).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplier).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplier).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplierPicker).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Dawn", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Dawn", substring = true)[0].performClick()
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkLiters, "25")
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkFat, "4")
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkLr, "28")
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).performScrollTo()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSuccess).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Milk received").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkAnother).assertIsDisplayed()
    }

    @Test
    fun operationsMakeYogurtOpensConnectedEntry() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurt).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtForm).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtReview).assertIsDisplayed()
        composeRule.onNodeWithText("No money", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkBack).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
    }

    @Test
    fun operationsMakeYogurtRecordsPhysicalBatch() {
        openOperationsWork()
        // Ensure milk stock exists for the conversion.
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilk).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplier).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplier).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplierPicker).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Dawn", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Dawn", substring = true)[0].performClick()
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkLiters, "40")
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkFat, "4")
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkLr, "28")
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).performScrollTo()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSuccess).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkBack).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).fetchSemanticsNode()
            }.isSuccess
        }

        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurt).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtMilk).fetchSemanticsNode()
            }.isSuccess
        }
        enterReceiveMilkMeasurement(NativeUiTags.OpsMakeYogurtMilk, "20")
        enterReceiveMilkMeasurement(NativeUiTags.OpsMakeYogurtYogurt, "15")
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtRecord).performScrollTo()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtRecord).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtSuccess).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Yogurt made").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtAnother).assertIsDisplayed()
    }

    @Test
    fun operationsRecordSpoilageOpensConnectedEntry() {
        openOperationsWork()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilage).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageForm).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageReview).assertIsDisplayed()
        composeRule.onNodeWithText("No money", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkBack).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
    }

    @Test
    fun operationsRecordSpoilageRecordsPhysicalLoss() {
        openOperationsWork()
        // Seed milk stock via receive milk first.
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilk).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplier).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplier).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSupplierPicker).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Dawn", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Dawn", substring = true)[0].performClick()
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkLiters, "30")
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkFat, "4")
        enterReceiveMilkMeasurement(NativeUiTags.OpsReceiveMilkLr, "28")
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).performScrollTo()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkRecord).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkSuccess).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkBack).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).fetchSemanticsNode()
            }.isSuccess
        }

        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilage).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageProduct).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageProduct).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageProductPicker).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Milk", substring = false).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Milk", substring = false)[0].performClick()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageReason).performClick()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageReasonPicker).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Spill", substring = false).performClick()
        enterReceiveMilkMeasurement(NativeUiTags.OpsRecordSpoilageQuantity, "2")
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageRecord).performScrollTo()
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageRecord).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageRecord).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageSuccess).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithText("Spoilage recorded").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageAnother).assertIsDisplayed()
    }

    @Test
    fun operationsWorkHardwareBackEndsSessionLikeSell() {
        openOperationsWork()

        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
            activity.onBackPressedDispatcher.onBackPressed()
            assertNull((activity.application as MilkShopApplication).currentSessionId)
        }

        composeRule.onNodeWithText("Saaf Milk").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.WelcomeContinue).performClick()
        waitForText("Who is using Saaf?")
    }

    @Test
    fun moreMenuHardwareBackReturnsToSellAndKeepsSession() {
        openCashier()
        openMoreMenu()

        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
            activity.onBackPressedDispatcher.onBackPressed()
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
        }

        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.MoreMenu).assertDoesNotExist()
    }

    @Test
    fun switchAccountHardwareBackKeepsSessionAndReturnsToMore() {
        openCashier()
        openMoreMenu()
        openSwitchAccountPicker()

        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
            activity.onBackPressedDispatcher.onBackPressed()
            assertNotNull(
                (activity.application as MilkShopApplication).currentSessionId,
                "Hardware Back on Switch account must cancel the switch, not sign out.",
            )
        }

        waitForMoreMenu()
        composeRule.onNodeWithTag(NativeUiTags.MoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertIsSelected()
        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
        }
    }

    @Test
    fun switchAccountUiBackKeepsSessionAndReturnsToMore() {
        openCashier()
        openMoreMenu()
        openSwitchAccountPicker()

        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull((activity.application as MilkShopApplication).currentSessionId)
        }
        composeRule.onNodeWithTag(NativeUiTags.AuthBackToWelcome).performClick()

        waitForMoreMenu()
        composeRule.onNodeWithTag(NativeUiTags.MoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertIsSelected()
        composeRule.activityRule.scenario.onActivity { activity ->
            assertNotNull(
                (activity.application as MilkShopApplication).currentSessionId,
                "UI Back on Switch account must cancel the switch, not sign out.",
            )
        }
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
        waitForText("Collect payment")
        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).performClick()
        composeRule.onNodeWithText("Collect is already open.").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).performClick()
        waitForText("Today’s activity")
        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).assertIsSelected()
        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onNodeWithTag(NativeUiTags.ActivityEmpty).fetchSemanticsNode() }.isSuccess ||
                runCatching { composeRule.onNodeWithTag(NativeUiTags.ActivityList).fetchSemanticsNode() }.isSuccess
        }

        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).performClick()
        composeRule.waitUntil(15_000) {
            runCatching { composeRule.onNodeWithTag(NativeUiTags.MoreMenu).fetchSemanticsNode() }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.MoreMenu).assertIsDisplayed()
        composeRule.onNodeWithText("More is not connected in this native preview yet.").assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).performClick()
        waitForText("New sale")
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertIsSelected()
    }

    @Test
    fun collectPaymentWritesRoomShowsReceiptAndOffersTheNextVisibleActions() = runBlocking {
        val app = app()
        val customerName = "Native Collect ${System.nanoTime()}"
        val customerId = CustomerManagementService(app.database).create(customerName)
        val cash = app.database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
        val cashBefore = app.database.readModelDao().accountBalance(cash.accountId)!!.balance
        val eventIdBefore = app.database.eventDao().findAllDescending().maxOfOrNull { it.eventId } ?: 0L
        openCashier()

        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).performClick()
        waitForText("Collect payment")
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSearch).performTextReplacement(customerName)
        composeRule.onNodeWithTag(NativeUiTags.collectCustomer(customerId)).performClick()
        waitForText(customerName)
        composeRule.onNodeWithTag(NativeUiTags.CollectAmountInput).performTextReplacement("125")
        composeRule.onNodeWithTag(NativeUiTags.CollectRecord).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectConfirmAdvance).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectSuccess).fetchSemanticsNode()
            }.isSuccess
        }

        val collection = app.database.eventDao().findAllDescending().single {
            it.eventId > eventIdBefore && it.eventType == "CREDIT_COLLECTION"
        }
        val movements = app.database.financialLogDao().findByEventId(collection.eventId)
        assertEquals(-125.0, movements.single { it.accountId == customerId }.amount, 0.000001)
        assertEquals(125.0, movements.single { it.accountId == cash.accountId }.amount, 0.000001)
        assertEquals(cashBefore + 125.0, app.database.readModelDao().accountBalance(cash.accountId)!!.balance, 0.000001)
        composeRule.onNodeWithText("Has PKR 125.00 advance").assertIsDisplayed()

        composeRule.onNodeWithTag(NativeUiTags.CollectViewRecord).performClick()
        composeRule.waitUntil(15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.ActivityDetail).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.ActivityDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ActivityDetailEventId).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ActivityDetailBack).performClick()
        waitForText("Collect payment")
        composeRule.onNodeWithTag(NativeUiTags.CollectAnother).performClick()
        composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).assertIsDisplayed()
        composeRule.onNodeWithText("Choose customer").assertIsDisplayed()
        Unit
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
        assertEquals("Instrumentation Cashier", actor!!.actorNameSnapshot)
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
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_CASHIER_LOGIN)).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).fetchSemanticsNode()
            }.isSuccess ||
                runCatching {
                    composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).fetchSemanticsNode()
                }.isSuccess
        }
        if (runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).fetchSemanticsNode()
            }.isSuccess
        ) {
            composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        }
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_CASHIER_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        waitForText("New sale")
    }

    private fun openOwnerOverview() {
        openAccountPicker()
        composeRule.onNodeWithTag(NativeUiTags.authAccount(SaafMilkTestRunner.TEST_LOGIN)).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).fetchSemanticsNode()
            }.isSuccess ||
                runCatching {
                    composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).fetchSemanticsNode()
                }.isSuccess
        }
        if (runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).fetchSemanticsNode()
            }.isSuccess
        ) {
            composeRule.onNodeWithTag(NativeUiTags.AuthFullPassphraseMethod).performClick()
        }
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerOverviewRoot).assertIsDisplayed()
    }

    private fun openOwnerWork() {
        openOwnerOverview()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).assertIsDisplayed()
    }

    private fun openOwnerRecords() {
        openOwnerOverview()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavRecords).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordsRoot).assertIsDisplayed()
    }

    private fun openOwnerMore() {
        openOwnerOverview()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerMoreRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreRoot).assertIsDisplayed()
    }

    private fun openOwnerProducts() {
        openOwnerMore()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreProducts).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerProductsRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsRoot).assertIsDisplayed()
    }

    private fun openOwnerCustomers() {
        openOwnerMore()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreCustomers)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersRoot).assertIsDisplayed()
    }

    private fun openOwnerSuppliers() {
        openOwnerMore()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSuppliers)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersRoot).assertIsDisplayed()
    }

    private fun openOwnerEmployees() {
        openOwnerMore()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreEmployees)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesRoot).assertIsDisplayed()
    }

    private fun openOwnerSale() {
        openOwnerWork()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordSale).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CashierEntryInput).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerSaleEntryRoot).assertIsDisplayed()
    }

    private fun openOwnerCollection() {
        openOwnerWork()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordPayment).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.CollectCustomerSelector).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerCollectionEntryRoot).assertIsDisplayed()
    }

    private fun openOwnerReceiveMilk() {
        openOwnerWork()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkReceiveMilk).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsReceiveMilkForm).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerReceiveMilkEntryRoot).assertIsDisplayed()
    }

    private fun openOwnerMakeYogurt() {
        openOwnerWork()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkMakeYogurt).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsMakeYogurtForm).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerMakeYogurtEntryRoot).assertIsDisplayed()
    }

    private fun openOwnerRecordSpoilage() {
        openOwnerWork()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkRecordSpoilage).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsRecordSpoilageForm).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerRecordSpoilageEntryRoot).assertIsDisplayed()
    }

    private fun openOwnerDeliveries() {
        openOwnerWork()
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkDeliveries).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsDeliveriesRoot).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerDeliveriesEntryRoot).assertIsDisplayed()
    }

    private fun pressHardwareBack() {
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun assertReturnedToOwnerWorkHome() {
        composeRule.waitUntil(10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerWorkHome).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavWork).assertIsSelected()
    }

    private fun assertOwnerShellWithoutForeignNav() {
        composeRule.onNodeWithTag(NativeUiTags.OwnerShellRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavCollect).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavActivity).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavDeliveries).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavAttendance).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
    }

    private fun openOperationsWork() {
        openAccountPicker()
        composeRule.onNodeWithTag(
            NativeUiTags.authAccount(SaafMilkTestRunner.TEST_OPERATOR_LOGIN),
        ).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.AuthPassphraseInput)
            .performTextReplacement(SaafMilkTestRunner.TEST_OPERATOR_PASSPHRASE)
        composeRule.onNodeWithTag(NativeUiTags.AuthSignIn).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).fetchSemanticsNode()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OpsWorkHome).assertIsDisplayed()
    }

    private fun enterReceiveMilkMeasurement(tag: String, value: String) {
        val node = composeRule.onNode(
            hasTestTag(tag) and hasSetTextAction(),
            useUnmergedTree = true,
        )
        node.performScrollTo()
        node.performTextClearance()
        node.performTextInput(value)
    }

    private fun openMoreMenu() {
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).performClick()
        waitForMoreMenu()
    }

    private fun waitForMoreMenu() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching { composeRule.onNodeWithTag(NativeUiTags.MoreMenu).fetchSemanticsNode() }.isSuccess
        }
    }

    private fun openSwitchAccountPicker() {
        composeRule.onNodeWithTag(NativeUiTags.MoreSwitchAccount).performClick()
        waitForText("Switch account")
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

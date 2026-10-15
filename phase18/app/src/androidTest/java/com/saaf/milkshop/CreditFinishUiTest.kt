package com.saaf.milkshop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.*
import org.junit.Assert.*
import java.io.File

class CreditFinishUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val view=CustomerCreditView(CustomerCreditStatus(CreditStatusKind.LATE,125000,3,ownerFlagged=true),CreditPaymentTerm.Monthly,"monthly",true,setOf("a"),listOf("2026-10-15 · SET · Reviewed · Accepted"),listOf(CustomerCreditPending("held","TERM_REVISION","Payment terms proposal",true)))
    private val row=NativeOwnerCustomerRow(7,"Long customer name with a market and duplicate identity reference","PKR 1,250.00","Active",null,null,view)
    private val detail=NativeOwnerCustomerDetail(row,null,null,2000.0,null,true,"PKR 2,000",null)
    @Test fun lightTermsReviewAndHistoryKeepChromeStable()=panel(1f,false)
    @Test fun darkLargeTextTermsReviewAndHistoryKeepChromeStable()=panel(2f,true)
    private fun panel(scale: Float,dark: Boolean) {
        var action: CustomerCreditCommand?=null
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
                SaafTheme(darkTheme=dark) { Surface { Column(Modifier.fillMaxSize()) {
                    Box(Modifier.testTag("fixed-header")) { CashierScreenHeader("Customers","Synthetic shop","Owner","2026-10-15",true) }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) { CustomerCreditPanel(detail,true,false) { action=it } }
                    Box(Modifier.testTag("fixed-nav")) { OwnerNavigation(OwnerSection.More) {} }
                } } }
            }
        }
        val header=rule.onNodeWithTag("fixed-header").fetchSemanticsNode().boundsInRoot
        val nav=rule.onNodeWithTag("fixed-nav").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("credit-edit-terms").performScrollTo().performClick()
        rule.onNodeWithText("30 days",useUnmergedTree=true).performClick()
        rule.onNodeWithTag("credit-confirm").performClick()
        assertEquals(CustomerCreditCommand.Terms("monthly",CreditPaymentTerm(CreditTermKind.NET_DAYS,30)),action)
        rule.onNodeWithTag("credit-set-flag").performScrollTo().performClick()
        rule.onNodeWithTag("credit-confirm").assertIsNotEnabled()
        rule.onNodeWithTag("credit-review-reason").performTextInput("Owner reviewed balance")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        rule.onNodeWithTag("credit-confirm").performClick()
        assertTrue(action is CustomerCreditCommand.Flag)
        rule.onNodeWithTag("credit-history").performScrollTo().performClick()
        rule.onNodeWithTag("credit-ledger").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("credit-audit").performScrollTo().performClick()
        rule.onNodeWithTag("credit-audit-history").performScrollTo().assertIsDisplayed()
        assertEquals(header,rule.onNodeWithTag("fixed-header").fetchSemanticsNode().boundsInRoot)
        assertEquals(nav,rule.onNodeWithTag("fixed-nav").fetchSemanticsNode().boundsInRoot)
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"credit-finish").apply { mkdirs() }
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir,"panel-$scale-$dark.png")))
    }
    @Test fun managerCanReadStatusWithoutOwnerActions() {
        rule.setContent { SaafTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { CustomerCreditPanel(detail,false,false) {} } } }
        rule.onNodeWithTag("credit-status").assertExists()
        rule.onNodeWithTag("credit-edit-terms").assertDoesNotExist();rule.onNodeWithTag("credit-set-flag").assertDoesNotExist()
    }
    @Test fun customerFiltersIncludeSettledOwnerFlagsAndSearchWithoutChangingRows() {
        val flagged=row.copy(credit=view.copy(status=CustomerCreditStatus(CreditStatusKind.SETTLED,ownerFlagged=true)))
        val good=row.copy(accountId=8,name="Good customer",credit=view.copy(status=CustomerCreditStatus(CreditStatusKind.GOOD)))
        val identity=com.saaf.milkshop.application.authentication.NativeSignedInIdentity(1,"owner","Owner",AppRole.OWNER,RolePermissions.permissions(AppRole.OWNER))
        rule.setContent { SaafTheme { OwnerCustomersScreen(SnackbarHostState(),identity,NativeOwnerMoreContext("2026-10-15",true,"Shop"),
            listOf(flagged,good),false,null,false,false,false,false,null,
            onBackToMenu={},onRetry={},onOpenCustomer={},onBackFromDetail={},onOpenCreate={},onBackFromCreate={},onCreateCustomer={},
            onOpenEdit={},onBackFromEdit={},onUpdateCustomer={_,_->},hostedScaffold={_,_,_,content->content(PaddingValues(0.dp))}) } }
        rule.onNodeWithTag("credit-filter-Bad").performClick()
        rule.onNodeWithText(flagged.name).assertExists();rule.onNodeWithText("Good customer").assertDoesNotExist()
        rule.onNodeWithTag("credit-customer-search").performTextInput("no matching customer")
        rule.onNodeWithText("No customers match this filter.").assertExists()
    }
}

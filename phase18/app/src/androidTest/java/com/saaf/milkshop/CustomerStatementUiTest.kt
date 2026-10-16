package com.saaf.milkshop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.activity.ComponentActivity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.sync.BusinessClock
import org.junit.*
import org.junit.Assert.*
import java.io.File
import kotlinx.coroutines.delay

class CustomerStatementUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    private val row=NativeOwnerCustomerRow(7,"Northern market community cafe and delivery counter","PKR 300.00","Active",null,"North",
        CustomerCreditView(CustomerCreditStatus(CreditStatusKind.LATE,30000,3,ownerFlagged=true),CreditPaymentTerm.Weekly,"weekly-default-v1",true,setOf("a"),
            listOf("Accepted Owner review"),listOf(CustomerCreditPending("held","TERM_REVISION","Payment terms proposal",true))))
    private val detail=NativeOwnerCustomerDetail(row,"Long address by the community market","Private owner note",500.0,null,true,"PKR 500.00",null)
    private fun statement(period: CustomerStatementPeriod)=CustomerStatement(7,"synthetic-customer",row.name,"Configured shop",null,"Market Road","PKR",period,
        "2026-10-16T00:00:00Z",10000,30000,listOf(
            CustomerStatementLine(period.start.toString(),"ref-sale","Sale","Milk 2 L",20000,30000,false)))
    private fun show(scale: Float=1f,dark: Boolean=false,load: CustomerStatementLoader={_,p->statement(p)}) {
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
                SaafTheme(darkTheme=dark) { OwnerCustomersScreen(SnackbarHostState(),
                    NativeSignedInIdentity(1,"owner","Owner",AppRole.OWNER,RolePermissions.permissions(AppRole.OWNER)),
                    NativeOwnerMoreContext(BusinessClock.today().toString(),true,"Configured shop"),listOf(row),false,detail,false,false,false,false,null,
                    onBackToMenu={},onRetry={},onOpenCustomer={},onBackFromDetail={},onOpenCreate={},onBackFromCreate={},onCreateCustomer={},
                    onOpenEdit={},onBackFromEdit={},onUpdateCustomer={_,_->},loadStatement=load,
                    hostedScaffold={snackbar,mod,wrap,content->OwnerScaffold(snackbar,OwnerSection.More,{next->wrap{}},mod,content=content)}) }
            }
        }
    }
    @Test fun detailDisclosuresRetainPendingAttentionAndExistingOwnerActions() {
        show()
        rule.onNodeWithTag("customer-statement-entry").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("customer-credit-attention").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("credit-edit-terms").assertDoesNotExist()
        rule.onNodeWithTag("customer-disclosure-profile").performScrollTo().performClick()
        rule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailEdit).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("customer-disclosure-credit").performScrollTo().performClick()
        rule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailEdit).assertDoesNotExist()
        rule.onNodeWithTag("credit-edit-terms").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Discard held terms proposal").performScrollTo().assertIsDisplayed()
    }
    @Test fun statementLightPreviewAndReturnKeepChromeAndDisclosure()=preview(1f,false)
    @Test fun statementDarkLargeTextPreviewAndReturnKeepChromeAndDisclosure()=preview(2f,true)
    private fun preview(scale: Float,dark: Boolean) {
        show(scale,dark)
        val nav=rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        val title=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("customer-disclosure-profile").performScrollTo().performClick()
        rule.onNodeWithTag("customer-statement-entry").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("statement-paper").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Back to customer").performScrollTo()
        rule.waitUntil(10000) {rule.onAllNodesWithContentDescription("Statement page 1 of 1").fetchSemanticsNodes().isNotEmpty()}
        capture("statement-top-$scale-$dark")
        rule.onNodeWithTag("statement-read-text").performScrollTo().performClick()
        rule.onNodeWithText("Customer statement").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("statement-refresh").performScrollTo().assertIsDisplayed()
        assertEquals(nav,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
        assertEquals(title,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        capture("statement-$scale-$dark")
        rule.onNodeWithText("Back to customer").performScrollTo().performClick()
        rule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailEdit).performScrollTo().assertIsDisplayed()
        assertEquals(nav,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
        assertEquals(title,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
    }
    @Test fun periodFailureRetainsPreviousSnapshotAndAllowsRevert() {
        var calls=0
        show(load={_,p->calls++;if(p.start.dayOfMonth==1) {delay(30);error("Synthetic period error")};statement(p)})
        rule.onNodeWithTag("customer-statement-entry").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("statement-paper").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("statement-period").performScrollTo().performClick()
        rule.onNodeWithText("This month",useUnmergedTree=true).performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("statement-error").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("The faded preview is the previous statement, not the selected period.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Revert period").performScrollTo().performClick()
        rule.onNodeWithTag("statement-refresh").performScrollTo().assertIsDisplayed()
        assertTrue(calls>=3)
    }
    @Test fun multiplePagesAndRefreshClampToNewContent() {
        var calls=0
        show(load={_,p->
            calls++
            if(calls==1) statement(p).copy(openingPaisa=0,closingPaisa=900,
                lines=(1..9).map { CustomerStatementLine(p.start.toString(),"ref-$it","Sale $it",null,100,it*100L,false) })
            else statement(p)
        })
        rule.onNodeWithTag("customer-statement-entry").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("statement-paper").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("statement-next").performScrollTo().performClick()
        rule.onNodeWithText("Page 2 of",substring=true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("statement-read-text").performScrollTo().performClick()
        rule.onNodeWithText("Sale 9",substring=true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("statement-refresh").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithText("Page 1 of 1").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Page 1 of 1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Sale 9",substring=true).assertDoesNotExist()
    }
    private fun capture(name: String) {
        val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"statement-preview").apply {mkdirs()}
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir,"$name.png")))
    }
}

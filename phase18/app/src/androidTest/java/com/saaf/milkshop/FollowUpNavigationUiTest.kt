package com.saaf.milkshop

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.admin.*
import com.saaf.milkshop.nativeui.manager.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.sync.FollowUpAttention
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

internal open class FollowUpNavigationUiTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()
    protected var role by mutableStateOf(AppRole.OWNER)
    protected val gates=AtomicInteger()
    protected val date=LocalDate.parse("2026-10-18")
    protected val row=NativeOwnerCustomerRow(7,"Northern community cafe امینہ","PKR 300.00","Active",null,null)
    private val detail=NativeOwnerCustomerDetail(row,null,null,null,null,true,null,null)
    private val reminder=CustomerReminder(7,"fixture",row.name,"Configured shop",null,30000,date.toString())
    private val status=CustomerCreditStatus(CreditStatusKind.LATE,30000,3)
    private val shared=CustomerSharedHandoffAccess(load={CustomerSharedHandoffState()},open={_,_,_,_,_->
        gates.incrementAndGet();CustomerSharedHandoffState("fixture-attempt","HANDOFF_STARTED",canResolve=true,blockedReason="Review the attempt")
    },recover={_,_->error("Unexpected")},release={_,_->error("Unexpected")},resolve={_,_,_,_->error("Unexpected")})
    private val workspace=FollowUpWorkspaceAccess(load={FollowUpWorkspaceSnapshot(FollowUpPolicy("fixture",date.minusDays(2),true),date,
        listOf(FollowUpCustomer(7,"fixture",row.name,null,true,30000,status)),online=true,owner=role==AppRole.OWNER,saveToken="fixture")},
        command={_,_->error("No saves in navigation fixture")})
    protected fun show(scale: Float=1f,dark: Boolean=false,initialRole: AppRole=AppRole.OWNER,workspaceAccess: FollowUpWorkspaceAccess=workspace,
        balancePaisa: Long=30000,handoff: CustomerSharedHandoffAccess=shared) {
        role=initialRole
        rule.activityRule.scenario.onActivity {it.enableEdgeToEdge();it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.getInsetsController(it.window,it.window.decorView).apply {isAppearanceLightStatusBars=false;isAppearanceLightNavigationBars=!dark}}
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {SaafTheme(darkTheme=dark) {key(role) {
                var selected by remember {mutableStateOf<NativeOwnerCustomerDetail?>(null)}
                var loading by remember {mutableStateOf(false)}
                var request by remember {mutableStateOf<Long?>(null)}
                var guard by remember {mutableStateOf<EntryLeaveGuard?>(null)}
                LaunchedEffect(request) {if(request!=null) {loading=true;delay(100);selected=detail.copy(row=row.copy(balanceLabel=statementMoney(balancePaisa)));loading=false}}
                val bundle=remember(workspaceAccess,handoff) {CustomerFollowUpAccess(workspaceAccess,flowOf(FollowUpAttention(1,false)),handoff)}
                OwnerCustomersScreen(SnackbarHostState(),NativeSignedInIdentity(role.ordinal.toLong(),role.wireValue,"Staff",role,RolePermissions.permissions(role)),
                    NativeOwnerMoreContext(date.toString(),true,"Configured shop"),listOf(row.copy(balanceLabel=statementMoney(balancePaisa))),loading,selected,false,false,false,false,null,
                    onBackToMenu={},onRetry={},onOpenCustomer={request=it},onBackFromDetail={selected=null;request=null},onOpenCreate={},onBackFromCreate={},onCreateCustomer={},
                    onOpenEdit={},onBackFromEdit={},onUpdateCustomer={_,_->},followUps=bundle,registerGuard={guard=it},
                    communication=CustomerCommunicationAccess(reminder={reminder.copy(balancePaisa=balancePaisa)},sharedHandoff=handoff),loadStatement={_,p->
                        CustomerStatement(7,"fixture",row.name,"Configured shop",null,null,"PKR",p,"2026-10-18T00:00:00Z",0,balancePaisa,
                            if(balancePaisa==0L) emptyList() else listOf(CustomerStatementLine(p.start.toString(),"fixture-sale","Sale","Milk",balancePaisa,balancePaisa,false)))},
                    hostedScaffold={s,m,wrap,content->when(role) {
                        AppRole.ADMIN->AdminScaffold(s,AdminSection.People,{guard?.requestLeave {}},m,content)
                        AppRole.MANAGER->ManagerScaffold(s,ManagerSection.More,{guard?.requestLeave {}},m,content)
                        else->OwnerScaffold(s,OwnerSection.More,{wrap {}},m,content=content)
                    }})
            }}}
        }
        rule.waitForIdle()
    }
    private fun review() {
        rule.onNodeWithTag("followup-attention-entry").performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("followup-row-7").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("followup-row-7").performScrollTo().performClick()
    }
    private fun prepareReminder() {
        rule.onNodeWithText("Prepare reminder").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("reminder-message").fetchSemanticsNodes().isNotEmpty()}
    }
    protected fun capture(name: String) {
        rule.waitForIdle();val context=InstrumentationRegistry.getInstrumentation().targetContext
        val dir=File(context.getExternalFilesDir(null),"followup-navigation").apply {mkdirs()}
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(dir,"$name.png")))
    }
    @Test fun ownerReturnPathsRetainReviewAndChrome() {
        show();val header=rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val nav=rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        review();prepareReminder()
        rule.onNodeWithText("Back to follow-up").performScrollTo().performClick()
        rule.onNodeWithText("Review follow-up").assertIsDisplayed()
        assertEquals(header,rule.onNodeWithText("Customers",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot)
        assertEquals(nav,rule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
        capture("owner-review-return")
    }
    @Test fun managerAndAdminUseTheirOwnChromeAndGuards() {
        show(initialRole=AppRole.ADMIN)
        for(current in listOf(AppRole.ADMIN,AppRole.MANAGER)) {
            if(current==AppRole.MANAGER) {rule.runOnIdle {role=current};rule.waitForIdle()}
            review();prepareReminder()
            rule.onNodeWithTag("reminder-phone").performScrollTo().performTextInput("+923001234567")
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();rule.waitForIdle()
            rule.onNodeWithTag(if(current==AppRole.ADMIN) NativeUiTags.AdminNavWork else NativeUiTags.ManagerNavWork).performClick()
            rule.onNodeWithText("Keep editing").assertIsDisplayed().performClick()
            rule.onNodeWithText("Back to follow-up").performScrollTo().performClick()
            rule.onNodeWithTag("reminder-discard").performClick()
            rule.onNodeWithText("Review follow-up").assertIsDisplayed()
            rule.onNodeWithTag(if(current==AppRole.ADMIN) NativeUiTags.AdminShellRoot else NativeUiTags.ManagerShellRoot).assertExists()
            rule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertDoesNotExist()
        }
    }
    @Test fun darkLargeReviewAndStatementReturn() {
        show(2f,true);review()
        rule.onNodeWithTag("followup-prepare").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("statement-paper").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("statement-prepare-reminder").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("reminder-message").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Back to statement").performScrollTo().performClick()
        rule.onNodeWithText("Back to follow-up").performScrollTo().performClick()
        rule.onNodeWithText("Review follow-up").assertIsDisplayed();capture("dark-large-review-return")
    }
    @Test fun directCustomerReminderUsesSharedHook() {
        show();rule.onNodeWithTag(NativeUiTags.OwnerCustomersList).performScrollToNode(hasText(row.name))
        rule.onNodeWithText(row.name,useUnmergedTree=true).performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("customer-statement-entry").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("customer-statement-entry").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("statement-paper").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("statement-prepare-reminder").performScrollTo().performClick()
        rule.waitUntil(10000) {rule.onAllNodesWithTag("reminder-phone").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("reminder-phone").performScrollTo().performTextInput("+923001234567")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();rule.waitForIdle()
        rule.onNodeWithTag("reminder-confirm-recipient").performScrollTo().performClick()
        rule.onNodeWithTag("reminder-open").performScrollTo().performClick()
        rule.waitUntil(10000) {gates.get()==1};rule.onNodeWithTag("reminder-open").assertIsNotEnabled()
    }
}

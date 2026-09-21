package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreDestination
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.OwnerMoreScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerMoreScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun menuShowsJourneyGroupsWithoutCashierChrome() {
        setScreen(NativeOwnerMoreState.Ready(context = openContext()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreProducts).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreCustomers)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSuppliers)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreEmployees)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreCosting)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreShopSettings)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMorePersonalSecurity)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreUsersRoles)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSecurityHistory)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreBackup)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSwitchAccount)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSignOut)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
    }

    @Test
    fun productsOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenProducts = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreProducts).performClick()
        assertTrue(opened)
    }

    @Test
    fun customersOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenCustomers = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreCustomers)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun suppliersOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenSuppliers = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSuppliers)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun employeesOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenEmployees = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreEmployees)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun costingPlansOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenCostingPlans = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreCosting)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun shopSettingsOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenShopSettings = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreShopSettings)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun usersRolesOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenUsersRoles = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreUsersRoles)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun securityHistoryOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenSecurityHistory = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSecurityHistory)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun backupOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenBackup = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreBackup)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun personalSecurityOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenPersonalSecurity = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMorePersonalSecurity)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun personalSecurityScreenShowsStatusAndBack() {
        var back = false
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.PersonalSecurity,
                security = NativeMoreSecurityStatus(
                    quickPinEnabled = false,
                    phoneSecurityEnabled = false,
                    phoneSecurityAvailable = false,
                    phoneSecurityLabel = "Phone security",
                ),
            ),
            onBackToMenu = { back = true },
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSecurityScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSecurityStatus)
            .assertExists()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreBack).performClick()
        assertTrue(back)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            NativeOwnerMoreState.LoadFailure("More could not be loaded."),
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreRetryLoad).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun overviewNavFromMoreRequestsNavigation() {
        var navigated: OwnerSection? = null
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onNavigate = { navigated = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).performClick()
        assertEquals(OwnerSection.Overview, navigated)
    }

    private fun setScreen(
        state: NativeOwnerMoreState,
        onRetry: () -> Unit = {},
        onOpenPersonalSecurity: () -> Unit = {},
        onOpenProducts: () -> Unit = {},
        onOpenCustomers: () -> Unit = {},
        onOpenSuppliers: () -> Unit = {},
        onOpenEmployees: () -> Unit = {},
        onOpenCostingPlans: () -> Unit = {},
        onOpenShopSettings: () -> Unit = {},
        onOpenUsersRoles: () -> Unit = {},
        onOpenSecurityHistory: () -> Unit = {},
        onOpenBackup: () -> Unit = {},
        onBackToMenu: () -> Unit = {},
        onSwitchAccount: () -> Unit = {},
        onSignOut: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerMoreScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = NativeSignedInIdentity(
                        userId = 1L,
                        loginName = "owner",
                        displayName = "Owner",
                        role = AppRole.OWNER,
                        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
                    ),
                    state = state,
                    onRetry = onRetry,
                    onOpenPersonalSecurity = onOpenPersonalSecurity,
                    onOpenProducts = onOpenProducts,
                    onOpenCustomers = onOpenCustomers,
                    onOpenSuppliers = onOpenSuppliers,
                    onOpenEmployees = onOpenEmployees,
                    onOpenCostingPlans = onOpenCostingPlans,
                    onOpenShopSettings = onOpenShopSettings,
                    onOpenUsersRoles = onOpenUsersRoles,
                    onOpenSecurityHistory = onOpenSecurityHistory,
                    onOpenBackup = onOpenBackup,
                    onBackToMenu = onBackToMenu,
                    onRetryProducts = {},
                    onOpenProduct = {},
                    onBackFromProductDetail = {},
                    onCreateProductPreview = {},
                    onRetryCustomers = {},
                    onOpenCustomer = {},
                    onBackFromCustomerDetail = {},
                    onCreateCustomerPreview = {},
                    onRetrySuppliers = {},
                    onOpenSupplier = {},
                    onBackFromSupplierDetail = {},
                    onCreateSupplierPreview = {},
                    onRetryEmployees = {},
                    onOpenEmployee = {},
                    onBackFromEmployeeDetail = {},
                    onCreateEmployeePreview = {},
                    onRetryCostingPlans = {},
                    onOpenCostingPolicy = {},
                    onBackFromCostingPolicyDetail = {},
                    onApproveCostingPolicyPreview = {},
                    onRetryShopSettings = {},
                    onEditShopSettingsPreview = {},
                    onRetryUsersRoles = {},
                    onOpenUser = {},
                    onBackFromUserDetail = {},
                    onCreateUserPreview = {},
                    onRetrySecurityHistory = {},
                    onOpenSecurityAudit = {},
                    onBackFromSecurityAuditDetail = {},
                    onRetryBackup = {},
                    onExportBackupPreview = {},
                    onRestoreBackupPreview = {},
                    onConnectBackupPreview = {},
                    onRequestRemoveQuickUnlock = {},
                    onCancelRemoveQuickUnlock = {},
                    onRemoveQuickUnlock = {},
                    onEnrollQuickPin = { _, _ -> },
                    onEnrollPhoneSecurity = { _, _ -> },
                    onSwitchAccount = onSwitchAccount,
                    onSignOut = onSignOut,
                    onNavigate = onNavigate,
                    onLeaveConfirmed = {},
                )
            }
        }
    }

    private fun openContext() = NativeOwnerMoreContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        shopName = "Saaf Milk",
    )
}

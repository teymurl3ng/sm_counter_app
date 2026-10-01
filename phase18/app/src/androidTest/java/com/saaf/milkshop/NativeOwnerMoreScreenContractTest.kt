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
import androidx.compose.ui.test.performTextInput
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
import com.saaf.milkshop.nativeui.owner.OwnerSyncState
import com.saaf.milkshop.sync.NativeSyncStatus
import com.saaf.milkshop.sync.NativeSyncShopChoice
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
    fun personalSecurityScreenShowsStatus() {
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
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSecurityScreen).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSecurityStatus)
            .assertExists()
            .assertIsDisplayed()
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
        onOpenSync: () -> Unit = {},
        onRefreshSync: () -> Unit = {},
        onEnrollSync: (String, CharArray, String) -> Unit = { _, _, _ -> },
        onEnrollSyncInShop: (String, String, CharArray, String) -> Unit = { _, _, _, _ -> },
        onSyncNow: () -> Unit = {},
        onUnenrollSync: () -> Unit = {},
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
                    onOpenSync = onOpenSync,
                    onRefreshSync = onRefreshSync,
                    onEnrollSync = onEnrollSync,
                    onEnrollSyncInShop = onEnrollSyncInShop,
                    onSyncNow = onSyncNow,
                    onUnenrollSync = onUnenrollSync,
                    onBackToMenu = onBackToMenu,
                    onRetryProducts = {},
                    onOpenProduct = {},
                    onBackFromProductDetail = {},
                    onOpenProductCreate = {},
                    onBackFromProductCreate = {},
                    onCreateProduct = {},
                    onOpenProductEdit = {},
                    onBackFromProductEdit = {},
                    onUpdateProduct = {},
                    onArchiveProduct = {},
                    onUnarchiveProduct = {},
                    onProductsShowArchivedChange = {},
                    onRetryCustomers = {},
                    onOpenCustomer = {},
                    onBackFromCustomerDetail = {},
                    onOpenCustomerCreate = {},
                    onBackFromCustomerCreate = {},
                    onCreateCustomer = {},
                    onOpenCustomerEdit = {},
                    onBackFromCustomerEdit = {},
                    onUpdateCustomer = { _, _ -> },
                    onRetrySuppliers = {},
                    onOpenSupplier = {},
                    onBackFromSupplierDetail = {},
                    onOpenSupplierCreate = {},
                    onBackFromSupplierCreate = {},
                    onCreateSupplier = {},
                    onOpenSupplierEdit = {},
                    onBackFromSupplierEdit = {},
                    onUpdateSupplier = { _, _ -> },
                    onRetryEmployees = {},
                    onOpenEmployee = {},
                    onBackFromEmployeeDetail = {},
                    onOpenEmployeeCreate = {},
                    onBackFromEmployeeCreate = {},
                    onCreateEmployee = {},
                    onOpenEmployeeEdit = {},
                    onBackFromEmployeeEdit = {},
                    onUpdateEmployee = { _, _ -> },
                    onRetryShopSettings = {},
                    onOpenShopIdentityEdit = {},
                    onBackFromShopIdentityEdit = {},
                    onSaveShopIdentity = {},
                    onOpenShopRatesEdit = {},
                    onBackFromShopRatesEdit = {},
                    onUpdateShopRates = {},
                    onOpenShopCategoryCreate = {},
                    onBackFromShopCategoryCreate = {},
                    onCreateShopExpenseCategory = {},
                    onOpenShopCategory = {},
                    onBackFromShopCategoryDetail = {},
                    onSetShopExpenseCategoryArchived = {},
                    onRetryUsersRoles = {},
                    onOpenUser = {},
                    onBackFromUserDetail = {},
                    onOpenUserEnroll = {},
                    onBackFromUserEnroll = {},
                    onBeginUserEnroll = { _, _, _ -> },
                    onCompleteUserEnroll = { a, b ->
                        a.fill('\u0000')
                        b.fill('\u0000')
                    },
                    onOpenUserRoleChange = {},
                    onBackFromUserRoleChange = {},
                    onChangeUserRole = {},
                    onRequestSetUserEnabled = {},
                    onConfirmSetUserEnabled = {},
                    onCancelSetUserEnabled = {},
                    onSubmitUsersRolesReauth = { it.fill('\u0000') },
                    onCancelUsersRolesReauth = {},
                    onRetrySecurityHistory = {},
                    onOpenSecurityAudit = {},
                    onBackFromSecurityAuditDetail = {},
                    onRetryBackup = {},
                    onOpenBackupExport = {},
                    onBackFromBackupExport = {},
                    onExportBackup = { _, passphrase -> passphrase.fill('\u0000') },
                    onOpenBackupRestore = {},
                    onBackFromBackupRestore = {},
                    onInspectBackupRestore = { _, passphrase -> passphrase.fill('\u0000') },
                    onConfirmBackupRestore = {},
                    onOpenBackupConnect = {},
                    onBackFromBackupConnect = {},
                    onConnectDriveBackup = { passphrase, _ -> passphrase.fill('\u0000') },
                    onSetDriveBackupEnabled = {},
                    onRunDriveBackupNow = {},
                    onRequestBackupDisconnect = {},
                    onConfirmBackupDisconnect = {},
                    onCancelBackupDisconnect = {},
                    onSubmitBackupReauth = { it.fill('\u0000') },
                    onCancelBackupReauth = {},
                    onRequestRemoveQuickUnlock = {},
                    onCancelRemoveQuickUnlock = {},
                    onRemoveQuickUnlock = {},
                    onEnrollQuickPin = { _, _ -> },
                    onEnrollPhoneSecurity = { _, _ -> },
                    onSwitchAccount = onSwitchAccount,
                    onSignOut = onSignOut,
                    hostedScaffold = { s, m, wrapNav, content ->
                        com.saaf.milkshop.nativeui.owner.OwnerScaffold(
                            snackbarHostState = s,
                            section = OwnerSection.More,
                            onNavigate = { section -> wrapNav { onNavigate(section) } },
                            modifier = m,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun openContext() = NativeOwnerMoreContext(
        businessDate = "2026-09-21",
        postingOpen = true,
        shopName = "Saaf Milk",
    )

    // ---- Sync screen (14.8.6 cutover) ----

    @Test
    fun syncRowOpensFromMenu() {
        var opened = false
        setScreen(
            NativeOwnerMoreState.Ready(context = openContext()),
            onOpenSync = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerMoreSync)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun syncScreenShowsStatusOrEnroll() {
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncEnroll)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun enrolledShowsServerShopSession() {
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(status = syncStatus()),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncStatus)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Shop 1").assertIsDisplayed()
    }

    @Test
    fun loadingShowsWhileStatusLoads() {
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(busy = true),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncLoading).assertIsDisplayed()
    }

    @Test
    fun syncNowPullsAndPushes() {
        var pressed = false
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(status = syncStatus()),
            ),
            onSyncNow = { pressed = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncNow)
            .performScrollTo()
            .performClick()
        assertTrue(pressed)
    }

    @Test
    fun refreshReloadsStatus() {
        var refreshed = false
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(status = syncStatus()),
            ),
            onRefreshSync = { refreshed = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncRefresh)
            .performScrollTo()
            .performClick()
        assertTrue(refreshed)
    }

    @Test
    fun unenrollOpensConfirm() {
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(status = syncStatus()),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncUnenroll)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Stop syncing this phone?").assertIsDisplayed()
    }

    @Test
    fun confirmStopsSyncing() {
        var stopped = false
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(status = syncStatus()),
            ),
            onUnenrollSync = { stopped = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncUnenroll)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncUnenrollConfirm).performClick()
        assertTrue(stopped)
    }

    @Test
    fun enrollConnectsPhone() {
        var enrolled: String? = null
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
            ),
            onEnrollSync = { username, _, _ -> enrolled = username },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncUsername)
            .performScrollTo()
            .performTextInput("teimur")
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncCredential)
            .performScrollTo()
            .performTextInput("224511180177980")
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncEnroll)
            .performScrollTo()
            .performClick()
        assertEquals("teimur", enrolled)
    }

    @Test
    fun shopChoicesListedWhenAmbiguous() {
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(
                    shopChoices = listOf(
                        NativeSyncShopChoice("shop-a", "Shop A"),
                        NativeSyncShopChoice("shop-b", "Shop B"),
                    ),
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncShopPicker)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Shop A").assertIsDisplayed()
        composeRule.onNodeWithText("Shop B").assertIsDisplayed()
    }

    @Test
    fun shopPickCompletesEnrollment() {
        var picked: String? = null
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(
                    shopChoices = listOf(NativeSyncShopChoice("shop-a", "Shop A")),
                ),
            ),
            onEnrollSyncInShop = { shopId, _, _, _ -> picked = shopId },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncShopPick)
            .performScrollTo()
            .performClick()
        assertEquals("shop-a", picked)
    }

    @Test
    fun errorShowsFailureReason() {
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(
                    error = "The server login or credential is not correct.",
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncError)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun noticeShowsResultSummary() {
        setScreen(
            NativeOwnerMoreState.Ready(
                context = openContext(),
                destination = NativeOwnerMoreDestination.Sync,
                syncState = OwnerSyncState(
                    notice = "Enrolled as owner for 'Shop 1'.",
                ),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSyncNotice)
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun syncStatus() = NativeSyncStatus(
        environment = "prod",
        shopName = "Shop 1",
        role = "owner",
        sessionPresent = true,
        trustedTimeAnchored = true,
        outboxCount = 0,
        pullCursor = 12,
    )
}


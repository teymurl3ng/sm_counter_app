package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerPendingEnrollment
import com.saaf.milkshop.application.owner.NativeOwnerUserDetail
import com.saaf.milkshop.application.owner.NativeOwnerUserRow
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.owner.OwnerUsersRolesScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerUsersRolesScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsUsersWithoutCashierChrome() {
        setScreen(users = listOf(sampleUser()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerUser(3L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(users = emptyList())
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No staff users yet.").assertIsDisplayed()
    }

    @Test
    fun userRowOpensDetail() {
        var opened: Long? = null
        setScreen(users = listOf(sampleUser()), onOpenUser = { opened = it })
        composeRule.onNodeWithTag(NativeUiTags.ownerUser(3L)).performClick()
        assertEquals(3L, opened)
    }

    @Test
    fun detailShowsFactsActionsAndBack() {
        var back = false
        var roleChange = false
        var disableRequested: Boolean? = null
        setScreen(
            users = listOf(sampleUser()),
            userDetail = NativeOwnerUserDetail(
                row = sampleUser(),
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-02T00:00:00Z",
                disabledAt = null,
                employeeAccountIdLabel = "Not linked",
            ),
            onBackFromDetail = { back = true },
            onOpenRoleChange = { roleChange = true },
            onRequestSetEnabled = { disableRequested = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetailChangeRole)
            .performScrollTo()
            .performClick()
        assertTrue(roleChange)
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetailDisable)
            .performScrollTo()
            .performClick()
        assertEquals(false, disableRequested)
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesCredentialResetDeferred)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetailBack)
            .performScrollTo()
            .performClick()
        assertTrue(back)
    }

    @Test
    fun backToMenuFromList() {
        var back = false
        setScreen(users = listOf(sampleUser()), onBackToMenu = { back = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesBack).performClick()
        assertTrue(back)
    }

    @Test
    fun enrollOpensIdentityForm() {
        var opened = false
        setScreen(users = listOf(sampleUser()), onOpenEnroll = { opened = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnroll).performClick()
        assertTrue(opened)
    }

    @Test
    fun enrollIdentityFormSubmitsBegin() {
        var savedLogin: String? = null
        var savedDisplay: String? = null
        var savedRole: String? = null
        setScreen(
            users = listOf(sampleUser()),
            userEnrollOpen = true,
            onBeginEnroll = { login, display, role ->
                savedLogin = login
                savedDisplay = display
                savedRole = role
            },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollLogin)
            .performScrollTo()
            .performTextReplacement("new.cashier")
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollDisplayName)
            .performScrollTo()
            .performTextReplacement("New Cashier")
        composeRule.onNodeWithTag(NativeUiTags.ownerUsersRolesEnrollRole(AppRole.OPERATOR.wireValue))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals("new.cashier", savedLogin)
        assertEquals("New Cashier", savedDisplay)
        assertEquals(AppRole.OPERATOR.wireValue, savedRole)
    }

    @Test
    fun enrollPassphraseStepCompletes() {
        var completed = false
        setScreen(
            users = listOf(sampleUser()),
            userEnrollOpen = true,
            userEnrollPending = NativeOwnerPendingEnrollment(
                userId = 9L,
                displayName = "New Cashier",
                loginName = "new.cashier",
                enrollmentToken = "token-9",
            ),
            onCompleteEnroll = { passphrase, confirmation ->
                completed = passphrase.contentEquals(confirmation) &&
                    String(passphrase).startsWith("new cashier")
                passphrase.fill('\u0000')
                confirmation.fill('\u0000')
            },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollPassphraseForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollPassphrase)
            .performScrollTo()
            .performTextReplacement("new cashier enroll passphrase 2026!!")
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollPassphraseConfirm)
            .performScrollTo()
            .performTextReplacement("new cashier enroll passphrase 2026!!")
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesEnrollPassphraseSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertTrue(completed)
    }

    @Test
    fun roleChangeFormSavesSelectedRole() {
        var savedRole: String? = null
        setScreen(
            users = listOf(sampleUser()),
            userDetail = NativeOwnerUserDetail(
                row = sampleUser(),
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-02T00:00:00Z",
                disabledAt = null,
                employeeAccountIdLabel = "Not linked",
            ),
            userRoleChangeOpen = true,
            onChangeRole = { savedRole = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesRoleChangeForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerUsersRolesChangeRole(AppRole.MANAGER.wireValue))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesRoleChangeSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(AppRole.MANAGER.wireValue, savedRole)
    }

    @Test
    fun disableConfirmInvokesCallback() {
        var confirmed = false
        setScreen(
            users = listOf(sampleUser()),
            userDetail = NativeOwnerUserDetail(
                row = sampleUser(),
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-02T00:00:00Z",
                disabledAt = null,
                employeeAccountIdLabel = "Not linked",
            ),
            userEnableConfirmOpen = true,
            onConfirmSetEnabled = { confirmed = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDisableConfirm).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            users = emptyList(),
            usersError = "Users could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesRetry).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        users: List<NativeOwnerUserRow>,
        usersLoading: Boolean = false,
        userDetail: NativeOwnerUserDetail? = null,
        userEnrollOpen: Boolean = false,
        userEnrollPending: NativeOwnerPendingEnrollment? = null,
        userEnrollBusy: Boolean = false,
        userRoleChangeOpen: Boolean = false,
        userRoleChanging: Boolean = false,
        userEnableConfirmOpen: Boolean = false,
        userEnableBusy: Boolean = false,
        userReauthOpen: Boolean = false,
        userReauthBusy: Boolean = false,
        usersError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenUser: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenEnroll: () -> Unit = {},
        onBackFromEnroll: () -> Unit = {},
        onBeginEnroll: (String, String, String) -> Unit = { _, _, _ -> },
        onCompleteEnroll: (CharArray, CharArray) -> Unit = { a, b ->
            a.fill('\u0000')
            b.fill('\u0000')
        },
        onOpenRoleChange: () -> Unit = {},
        onBackFromRoleChange: () -> Unit = {},
        onChangeRole: (String) -> Unit = {},
        onRequestSetEnabled: (Boolean) -> Unit = {},
        onConfirmSetEnabled: () -> Unit = {},
        onCancelSetEnabled: () -> Unit = {},
        onSubmitReauth: (CharArray) -> Unit = { it.fill('\u0000') },
        onCancelReauth: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerUsersRolesScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = NativeSignedInIdentity(
                        userId = 1L,
                        loginName = "owner",
                        displayName = "Owner",
                        role = AppRole.OWNER,
                        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
                    ),
                    context = NativeOwnerMoreContext(
                        businessDate = "2026-09-21",
                        postingOpen = true,
                        shopName = "Saaf Milk",
                    ),
                    users = users,
                    usersLoading = usersLoading,
                    userDetail = userDetail,
                    userEnrollOpen = userEnrollOpen,
                    userEnrollPending = userEnrollPending,
                    userEnrollBusy = userEnrollBusy,
                    userRoleChangeOpen = userRoleChangeOpen,
                    userRoleChanging = userRoleChanging,
                    userEnableConfirmOpen = userEnableConfirmOpen,
                    userEnableBusy = userEnableBusy,
                    userReauthOpen = userReauthOpen,
                    userReauthBusy = userReauthBusy,
                    usersError = usersError,
                    enrollableRoles = AppRole.entries.toList(),
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenUser = onOpenUser,
                    onBackFromDetail = onBackFromDetail,
                    onOpenEnroll = onOpenEnroll,
                    onBackFromEnroll = onBackFromEnroll,
                    onBeginEnroll = onBeginEnroll,
                    onCompleteEnroll = onCompleteEnroll,
                    onOpenRoleChange = onOpenRoleChange,
                    onBackFromRoleChange = onBackFromRoleChange,
                    onChangeRole = onChangeRole,
                    onRequestSetEnabled = onRequestSetEnabled,
                    onConfirmSetEnabled = onConfirmSetEnabled,
                    onCancelSetEnabled = onCancelSetEnabled,
                    onSubmitReauth = onSubmitReauth,
                    onCancelReauth = onCancelReauth,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleUser() = NativeOwnerUserRow(
        userId = 3L,
        loginName = "cashier.one",
        displayName = "Cashier One",
        roleWire = AppRole.CASHIER.wireValue,
        roleLabel = "Cashier",
        statusLabel = "Active",
        enabled = true,
        setupRequired = false,
    )
}

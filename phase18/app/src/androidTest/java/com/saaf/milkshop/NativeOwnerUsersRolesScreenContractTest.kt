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
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
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
    fun detailShowsFactsAndBack() {
        var back = false
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
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetailReadOnly)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesDetailBack).performClick()
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
    fun createPreviewSurfacesNotice() {
        var preview = false
        setScreen(users = listOf(sampleUser()), onCreatePreview = { preview = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerUsersRolesCreatePreview).performClick()
        assertTrue(preview)
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
        usersError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenUser: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onCreatePreview: () -> Unit = {},
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
                    usersError = usersError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenUser = onOpenUser,
                    onBackFromDetail = onBackFromDetail,
                    onCreatePreview = onCreatePreview,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleUser() = NativeOwnerUserRow(
        userId = 3L,
        loginName = "cashier.one",
        displayName = "Cashier One",
        roleLabel = "Cashier",
        statusLabel = "Active",
        setupRequired = false,
    )
}

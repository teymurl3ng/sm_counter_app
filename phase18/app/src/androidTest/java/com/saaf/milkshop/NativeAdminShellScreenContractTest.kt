package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.admin.AdminSection
import com.saaf.milkshop.nativeui.admin.AdminShellChromePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeAdminShellScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun workStubShowsAdminChromeAndKeepsWorkNavSelected() {
        setHost(AdminSection.Work)
        composeRule.onNodeWithTag(NativeUiTags.AdminShellRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminNavWork).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.ManagerNavWork).assertDoesNotExist()
    }

    @Test
    fun bottomNavSwitchesAllFourSections() {
        setHost(AdminSection.Work)

        composeRule.onNodeWithTag(NativeUiTags.AdminNavPeople).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AdminPeopleRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminNavPeople).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.AdminNavRecords).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AdminRecordsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminNavRecords).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.AdminNavMore).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AdminMoreRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminNavMore).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.AdminNavWork).performClick()
        composeRule.onNodeWithTag(NativeUiTags.AdminWorkRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.AdminNavWork).assertIsSelected()
    }

    private fun setHost(initial: AdminSection) {
        composeRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            var section by remember { mutableStateOf(initial) }
            AdminShellChromePreview(
                identity = adminIdentity(),
                snackbarHostState = snackbarHostState,
                adminSection = section,
                onAdminSectionChange = { section = it },
            )
        }
    }

    private fun adminIdentity() = NativeSignedInIdentity(
        userId = 43L,
        loginName = "contract.admin",
        displayName = "Contract Admin",
        role = AppRole.ADMIN,
        effectivePermissions = RolePermissions.permissions(AppRole.ADMIN),
    )
}

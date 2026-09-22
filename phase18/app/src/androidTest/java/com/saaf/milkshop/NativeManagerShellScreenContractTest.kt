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
import com.saaf.milkshop.nativeui.manager.ManagerSection
import com.saaf.milkshop.nativeui.manager.ManagerShellChromePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeManagerShellScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun workStubShowsManagerChromeAndKeepsWorkNavSelected() {
        setHost(ManagerSection.Work)
        composeRule.onNodeWithTag(NativeUiTags.ManagerShellRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerNavWork).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavSell).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavOverview).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavWork).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.AdminNavWork).assertDoesNotExist()
    }

    @Test
    fun bottomNavSwitchesAllFourSections() {
        setHost(ManagerSection.Work)

        composeRule.onNodeWithTag(NativeUiTags.ManagerNavClose).performClick()
        composeRule.onNodeWithTag(NativeUiTags.ManagerCloseRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerNavClose).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.ManagerNavRecords).performClick()
        composeRule.onNodeWithTag(NativeUiTags.ManagerRecordsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerNavRecords).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.ManagerNavMore).performClick()
        composeRule.onNodeWithTag(NativeUiTags.ManagerMoreRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerNavMore).assertIsSelected()

        composeRule.onNodeWithTag(NativeUiTags.ManagerNavWork).performClick()
        composeRule.onNodeWithTag(NativeUiTags.ManagerWorkRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ManagerNavWork).assertIsSelected()
    }

    private fun setHost(initial: ManagerSection) {
        composeRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            var section by remember { mutableStateOf(initial) }
            ManagerShellChromePreview(
                identity = managerIdentity(),
                snackbarHostState = snackbarHostState,
                managerSection = section,
                onManagerSectionChange = { section = it },
            )
        }
    }

    private fun managerIdentity() = NativeSignedInIdentity(
        userId = 42L,
        loginName = "contract.manager",
        displayName = "Contract Manager",
        role = AppRole.MANAGER,
        effectivePermissions = RolePermissions.permissions(AppRole.MANAGER),
    )
}

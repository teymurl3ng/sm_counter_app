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
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeDetail
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerEmployeesScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerEmployeesScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsEmployeesWithoutCashierChrome() {
        setScreen(employees = listOf(sampleEmployee()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerEmployee(41L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(employees = emptyList())
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No employees yet.").assertIsDisplayed()
    }

    @Test
    fun employeeRowOpensDetail() {
        var opened: Long? = null
        setScreen(
            employees = listOf(sampleEmployee()),
            onOpenEmployee = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerEmployee(41L)).performClick()
        assertEquals(41L, opened)
    }

    @Test
    fun detailShowsFactsAndBack() {
        var back = false
        setScreen(
            employees = listOf(sampleEmployee()),
            employeeDetail = NativeOwnerEmployeeDetail(
                row = sampleEmployee(),
                referenceSalaryLabel = "PKR 35,000.00",
                salaryPeriod = "monthly",
                joinDate = "2026-01-15",
            ),
            onBackFromDetail = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesDetailReadOnly)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesDetailBack).performClick()
        assertTrue(back)
    }

    @Test
    fun backToMenuFromList() {
        var back = false
        setScreen(
            employees = listOf(sampleEmployee()),
            onBackToMenu = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesBack).performClick()
        assertTrue(back)
    }

    @Test
    fun createPreviewSurfacesNotice() {
        var preview = false
        setScreen(
            employees = listOf(sampleEmployee()),
            onCreatePreview = { preview = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesCreatePreview).performClick()
        assertTrue(preview)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            employees = emptyList(),
            employeesError = "Employees could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesRetry).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        employees: List<NativeOwnerEmployeeRow>,
        employeesLoading: Boolean = false,
        employeeDetail: NativeOwnerEmployeeDetail? = null,
        employeesError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenEmployee: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onCreatePreview: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerEmployeesScreen(
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
                    employees = employees,
                    employeesLoading = employeesLoading,
                    employeeDetail = employeeDetail,
                    employeesError = employeesError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenEmployee = onOpenEmployee,
                    onBackFromDetail = onBackFromDetail,
                    onCreatePreview = onCreatePreview,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleEmployee() = NativeOwnerEmployeeRow(
        accountId = 41L,
        name = "Ayesha Khan",
        balanceLabel = "PKR 0.00",
        statusLabel = "Active",
        role = "Delivery rider",
        phone = "0300",
    )
}

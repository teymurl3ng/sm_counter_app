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
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeDetail
import com.saaf.milkshop.application.owner.NativeOwnerEmployeeRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.EmployeeProfileInput
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
    fun detailShowsFactsAndEdit() {
        var back = false
        var editOpened = false
        setScreen(
            employees = listOf(sampleEmployee()),
            employeeDetail = sampleDetail(),
            onBackFromDetail = { back = true },
            onOpenEdit = { editOpened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesDetailEdit)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(editOpened)
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
    fun addEmployeeOpensCreateForm() {
        var opened = false
        setScreen(
            employees = listOf(sampleEmployee()),
            onOpenCreate = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesCreateOpen).performClick()
        assertTrue(opened)
    }

    @Test
    fun createFormShowsFieldsAndSave() {
        var saved: String? = null
        setScreen(
            employees = listOf(sampleEmployee()),
            employeeCreateOpen = true,
            onCreateEmployee = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesCreateForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesCreateName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesCreateName)
            .performScrollTo()
            .performTextReplacement("Fresh Hire")
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesCreateSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Fresh Hire", saved)
    }

    @Test
    fun editFormPrefillsAndSaves() {
        var savedName: String? = null
        var savedProfile: EmployeeProfileInput? = null
        setScreen(
            employees = listOf(sampleEmployee()),
            employeeDetail = sampleDetail(),
            employeeEditOpen = true,
            onUpdateEmployee = { name, profile ->
                savedName = name
                savedProfile = profile
            },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditRole)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditPhone)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditReferenceSalary)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditSalaryPeriod)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditJoinDate)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditActive)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditName)
            .performScrollTo()
            .performTextReplacement("Ayesha Khan Updated")
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditRole)
            .performScrollTo()
            .performTextReplacement("Shop helper")
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditPhone)
            .performScrollTo()
            .performTextReplacement("0311")
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditReferenceSalary)
            .performScrollTo()
            .performTextReplacement("42000")
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditSalaryPeriod)
            .performScrollTo()
            .performTextReplacement("weekly")
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditJoinDate)
            .performScrollTo()
            .performTextReplacement("2026-02-01")
        composeRule.onNodeWithTag(NativeUiTags.OwnerEmployeesEditSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Ayesha Khan Updated", savedName)
        assertEquals("Shop helper", savedProfile!!.role)
        assertEquals("0311", savedProfile!!.phone)
        assertEquals(42_000.0, savedProfile!!.referenceSalary!!, 0.001)
        assertEquals("weekly", savedProfile!!.salaryPeriod)
        assertEquals("2026-02-01", savedProfile!!.joinDate)
        assertTrue(savedProfile!!.active)
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
        employeeCreateOpen: Boolean = false,
        employeeCreating: Boolean = false,
        employeeEditOpen: Boolean = false,
        employeeUpdating: Boolean = false,
        employeesError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenEmployee: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenCreate: () -> Unit = {},
        onBackFromCreate: () -> Unit = {},
        onCreateEmployee: (String) -> Unit = {},
        onOpenEdit: () -> Unit = {},
        onBackFromEdit: () -> Unit = {},
        onUpdateEmployee: (String, EmployeeProfileInput) -> Unit = { _, _ -> },
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
                    employeeCreateOpen = employeeCreateOpen,
                    employeeCreating = employeeCreating,
                    employeeEditOpen = employeeEditOpen,
                    employeeUpdating = employeeUpdating,
                    employeesError = employeesError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenEmployee = onOpenEmployee,
                    onBackFromDetail = onBackFromDetail,
                    onOpenCreate = onOpenCreate,
                    onBackFromCreate = onBackFromCreate,
                    onCreateEmployee = onCreateEmployee,
                    onOpenEdit = onOpenEdit,
                    onBackFromEdit = onBackFromEdit,
                    onUpdateEmployee = onUpdateEmployee,
                    hostedScaffold = { snackbar, mod, _, content ->
                    androidx.compose.material3.Scaffold(
                        modifier = mod,
                        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
                        content = content,
                    )
                },
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

    private fun sampleDetail() = NativeOwnerEmployeeDetail(
        row = sampleEmployee(),
        referenceSalary = 35_000.0,
        referenceSalaryLabel = "PKR 35,000.00",
        salaryPeriod = "monthly",
        joinDate = "2026-01-15",
        active = true,
    )
}

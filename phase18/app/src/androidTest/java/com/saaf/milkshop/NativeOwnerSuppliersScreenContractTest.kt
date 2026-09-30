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
import com.saaf.milkshop.application.owner.NativeOwnerSupplierDetail
import com.saaf.milkshop.application.owner.NativeOwnerSupplierRow
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.owner.OwnerSuppliersScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerSuppliersScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsSuppliersWithoutCashierChrome() {
        setScreen(suppliers = listOf(sampleSupplier()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerSupplier(31L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(suppliers = emptyList())
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No suppliers yet.").assertIsDisplayed()
    }

    @Test
    fun supplierRowOpensDetail() {
        var opened: Long? = null
        setScreen(
            suppliers = listOf(sampleSupplier()),
            onOpenSupplier = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerSupplier(31L)).performClick()
        assertEquals(31L, opened)
    }

    @Test
    fun detailShowsFactsAndEdit() {
        var editOpened = false
        setScreen(
            suppliers = listOf(sampleSupplier()),
            supplierDetail = sampleDetail(),
            onOpenEdit = { editOpened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersDetailEdit)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(editOpened)
    }

    @Test
    fun addSupplierOpensCreateForm() {
        var opened = false
        setScreen(
            suppliers = listOf(sampleSupplier()),
            onOpenCreate = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersCreateOpen).performClick()
        assertTrue(opened)
    }

    @Test
    fun createFormShowsFieldsAndSave() {
        var saved: String? = null
        setScreen(
            suppliers = listOf(sampleSupplier()),
            supplierCreateOpen = true,
            onCreateSupplier = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersCreateForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersCreateName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersCreateName)
            .performScrollTo()
            .performTextReplacement("Fresh Canal Farm")
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersCreateSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Fresh Canal Farm", saved)
    }

    @Test
    fun editFormPrefillsAndSaves() {
        var savedName: String? = null
        var savedProfile: VendorProfileInput? = null
        setScreen(
            suppliers = listOf(sampleSupplier()),
            supplierDetail = sampleDetail(),
            supplierEditOpen = true,
            onUpdateSupplier = { name, profile ->
                savedName = name
                savedProfile = profile
            },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditPhone)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditLocation)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditNotes)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditActive)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditName)
            .performScrollTo()
            .performTextReplacement("Dawn Dairy Updated")
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditPhone)
            .performScrollTo()
            .performTextReplacement("0311")
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditLocation)
            .performScrollTo()
            .performTextReplacement("New Canal")
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersEditSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Dawn Dairy Updated", savedName)
        assertEquals("0311", savedProfile!!.phone)
        assertEquals("New Canal", savedProfile!!.location)
        assertEquals("Morning pickups", savedProfile!!.notes)
        assertTrue(savedProfile!!.active)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            suppliers = emptyList(),
            suppliersError = "Suppliers could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersRetry).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        suppliers: List<NativeOwnerSupplierRow>,
        suppliersLoading: Boolean = false,
        supplierDetail: NativeOwnerSupplierDetail? = null,
        supplierCreateOpen: Boolean = false,
        supplierCreating: Boolean = false,
        supplierEditOpen: Boolean = false,
        supplierUpdating: Boolean = false,
        suppliersError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenSupplier: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenCreate: () -> Unit = {},
        onBackFromCreate: () -> Unit = {},
        onCreateSupplier: (String) -> Unit = {},
        onOpenEdit: () -> Unit = {},
        onBackFromEdit: () -> Unit = {},
        onUpdateSupplier: (String, VendorProfileInput) -> Unit = { _, _ -> },
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerSuppliersScreen(
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
                    suppliers = suppliers,
                    suppliersLoading = suppliersLoading,
                    supplierDetail = supplierDetail,
                    supplierCreateOpen = supplierCreateOpen,
                    supplierCreating = supplierCreating,
                    supplierEditOpen = supplierEditOpen,
                    supplierUpdating = supplierUpdating,
                    suppliersError = suppliersError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenSupplier = onOpenSupplier,
                    onBackFromDetail = onBackFromDetail,
                    onOpenCreate = onOpenCreate,
                    onBackFromCreate = onBackFromCreate,
                    onCreateSupplier = onCreateSupplier,
                    onOpenEdit = onOpenEdit,
                    onBackFromEdit = onBackFromEdit,
                    onUpdateSupplier = onUpdateSupplier,
                    hostedScaffold = { snackbar, mod, wrapNav, content ->
                        OwnerScaffold(
                            snackbarHostState = snackbar,
                            section = OwnerSection.More,
                            onNavigate = { section -> wrapNav { onNavigate(section) } },
                            modifier = mod,
                            content = content,
                        )
                    },
                )
            }
        }
    }

    private fun sampleSupplier() = NativeOwnerSupplierRow(
        accountId = 31L,
        name = "Dawn Dairy",
        balanceLabel = "PKR 0.00",
        statusLabel = "Active",
        phone = "0300",
        location = "Canal Road",
    )

    private fun sampleDetail() = NativeOwnerSupplierDetail(
        row = sampleSupplier(),
        notes = "Morning pickups",
        active = true,
    )
}

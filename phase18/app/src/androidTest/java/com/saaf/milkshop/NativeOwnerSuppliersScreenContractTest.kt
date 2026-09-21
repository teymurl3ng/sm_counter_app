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
import com.saaf.milkshop.application.owner.NativeOwnerSupplierDetail
import com.saaf.milkshop.application.owner.NativeOwnerSupplierRow
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
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
    fun detailShowsFactsAndBack() {
        var back = false
        setScreen(
            suppliers = listOf(sampleSupplier()),
            supplierDetail = NativeOwnerSupplierDetail(
                row = sampleSupplier(),
                notes = "Morning pickups",
            ),
            onBackFromDetail = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersDetailReadOnly)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersDetailBack).performClick()
        assertTrue(back)
    }

    @Test
    fun backToMenuFromList() {
        var back = false
        setScreen(
            suppliers = listOf(sampleSupplier()),
            onBackToMenu = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersBack).performClick()
        assertTrue(back)
    }

    @Test
    fun createPreviewSurfacesNotice() {
        var preview = false
        setScreen(
            suppliers = listOf(sampleSupplier()),
            onCreatePreview = { preview = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSuppliersCreatePreview).performClick()
        assertTrue(preview)
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
        suppliersError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenSupplier: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onCreatePreview: () -> Unit = {},
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
                    suppliersError = suppliersError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenSupplier = onOpenSupplier,
                    onBackFromDetail = onBackFromDetail,
                    onCreatePreview = onCreatePreview,
                    onNavigate = onNavigate,
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
}

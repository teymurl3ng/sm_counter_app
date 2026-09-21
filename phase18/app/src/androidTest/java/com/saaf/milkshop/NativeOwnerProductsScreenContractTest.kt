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
import com.saaf.milkshop.application.owner.NativeOwnerProductDetail
import com.saaf.milkshop.application.owner.NativeOwnerProductRow
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerProductsScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerProductsScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsActiveProductsWithoutCashierChrome() {
        setScreen(products = listOf(sampleProduct()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerProduct(11L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(products = emptyList())
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No active products yet.").assertIsDisplayed()
    }

    @Test
    fun productRowOpensDetail() {
        var opened: Long? = null
        setScreen(
            products = listOf(sampleProduct()),
            onOpenProduct = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerProduct(11L)).performClick()
        assertEquals(11L, opened)
    }

    @Test
    fun detailShowsFactsAndBack() {
        var back = false
        setScreen(
            products = listOf(sampleProduct()),
            productDetail = NativeOwnerProductDetail(sampleProduct()),
            onBackFromDetail = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailReadOnly).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailBack).performClick()
        assertTrue(back)
    }

    @Test
    fun backToMenuFromList() {
        var back = false
        setScreen(
            products = listOf(sampleProduct()),
            onBackToMenu = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsBack).performClick()
        assertTrue(back)
    }

    @Test
    fun addProductOpensCreateForm() {
        var opened = false
        setScreen(
            products = listOf(sampleProduct()),
            onOpenCreate = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateOpen).performClick()
        assertTrue(opened)
    }

    @Test
    fun createFormShowsFieldsAndSave() {
        var saved: ProductCatalogInput? = null
        setScreen(
            products = listOf(sampleProduct()),
            productCreateOpen = true,
            onCreateProduct = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateUnitLiters).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateUnitKg).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateRate)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateTracking)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateNotes)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateName)
            .performScrollTo()
            .performTextReplacement("Fresh Cream")
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateUnitKg).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateRate)
            .performScrollTo()
            .performTextReplacement("450")
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Fresh Cream", saved!!.name)
        assertEquals("Kg", saved!!.unit)
        assertEquals(450.0, saved!!.defaultSaleRate!!, 0.001)
        assertTrue(saved!!.inventoryTracking)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            products = emptyList(),
            productsError = "Products could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsRetry).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        products: List<NativeOwnerProductRow>,
        productsLoading: Boolean = false,
        productDetail: NativeOwnerProductDetail? = null,
        productCreateOpen: Boolean = false,
        productCreating: Boolean = false,
        productsError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenProduct: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenCreate: () -> Unit = {},
        onBackFromCreate: () -> Unit = {},
        onCreateProduct: (ProductCatalogInput) -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerProductsScreen(
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
                    products = products,
                    productsLoading = productsLoading,
                    productDetail = productDetail,
                    productCreateOpen = productCreateOpen,
                    productCreating = productCreating,
                    productsError = productsError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenProduct = onOpenProduct,
                    onBackFromDetail = onBackFromDetail,
                    onOpenCreate = onOpenCreate,
                    onBackFromCreate = onBackFromCreate,
                    onCreateProduct = onCreateProduct,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleProduct() = NativeOwnerProductRow(
        productId = 11L,
        name = "Cream",
        unit = "Kg",
        rateLabel = "PKR 500.00 / Kg",
        statusLabel = "Active",
        trackingLabel = "Stock tracked",
        notes = "Fresh",
    )
}

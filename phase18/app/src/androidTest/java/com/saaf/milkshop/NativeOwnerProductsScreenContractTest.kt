package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
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
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
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
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsFilterActive).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsFilterArchived).assertIsDisplayed()
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
    fun archivedEmptyStateIsHonest() {
        setScreen(products = emptyList(), productsShowArchived = true)
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No archived products.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsCreateOpen).assertDoesNotExist()
    }

    @Test
    fun filterChipsRequestActiveAndArchived() {
        var showArchived: Boolean? = null
        setScreen(
            products = listOf(sampleProduct()),
            onShowArchivedChange = { showArchived = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsFilterArchived).performClick()
        assertEquals(true, showArchived)
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsFilterActive).performClick()
        assertEquals(false, showArchived)
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
    fun detailShowsFactsEditArchive() {
        var editOpened = false
        setScreen(
            products = listOf(sampleProduct()),
            productDetail = NativeOwnerProductDetail(sampleProduct()),
            onOpenEdit = { editOpened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailEdit).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailArchive).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailUnarchive).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailEdit).performClick()
        assertTrue(editOpened)
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailArchive).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsArchiveConfirm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsArchiveCancel).performClick()
    }

    @Test
    fun archivedDetailShowsUnarchiveOnly() {
        var unarchived = false
        setScreen(
            products = listOf(sampleArchivedProduct()),
            productsShowArchived = true,
            productDetail = NativeOwnerProductDetail(sampleArchivedProduct()),
            onUnarchiveProduct = { unarchived = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailUnarchive).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailEdit).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailArchive).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailUnarchive).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsUnarchiveConfirm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsUnarchiveCancel).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailUnarchive).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsUnarchiveConfirm).performClick()
        assertTrue(unarchived)
    }

    @Test
    fun longProductDetailReflowsInDarkExtraLargeText() {
        val product = sampleProduct().copy(
            name = "Extra thick full cream yoghurt for the northern market route",
            rateLabel = "PKR 123,456,789.00 / Kg",
            notes = "Keep chilled until the customer receives the complete order.",
        )
        setScreen(products = listOf(product), productDetail = NativeOwnerProductDetail(product),
            darkTheme = true, fontScale = 2f)
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailName).assertIsDisplayed()
        composeRule.onNodeWithText(product.rateLabel!!, useUnmergedTree = true).performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(product.rateLabel!!, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Full price and unit must reflow", layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "batch5b-product-detail-dark-200.png")
        output.outputStream().use { composeRule.onRoot().captureToImage().asAndroidBitmap()
            .compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun archiveConfirmCallsArchive() {
        var archived = false
        setScreen(
            products = listOf(sampleProduct()),
            productDetail = NativeOwnerProductDetail(sampleProduct()),
            onArchiveProduct = { archived = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsDetailArchive).performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsArchiveConfirm).performClick()
        assertTrue(archived)
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
    fun editFormPrefillsAndSaves() {
        var saved: ProductCatalogInput? = null
        setScreen(
            products = listOf(sampleProduct()),
            productDetail = NativeOwnerProductDetail(sampleProduct()),
            productEditOpen = true,
            onUpdateProduct = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditUnitLiters).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditUnitKg).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditRate)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditTracking)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditNotes)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditName)
            .performScrollTo()
            .performTextReplacement("Cream Updated")
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditUnitKg).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditRate)
            .performScrollTo()
            .performTextReplacement("525")
        composeRule.onNodeWithTag(NativeUiTags.OwnerProductsEditSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Cream Updated", saved!!.name)
        assertEquals("Kg", saved!!.unit)
        assertEquals(525.0, saved!!.defaultSaleRate!!, 0.001)
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
        productsShowArchived: Boolean = false,
        productDetail: NativeOwnerProductDetail? = null,
        productCreateOpen: Boolean = false,
        productCreating: Boolean = false,
        productEditOpen: Boolean = false,
        productUpdating: Boolean = false,
        productArchiving: Boolean = false,
        productUnarchiving: Boolean = false,
        productsError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onShowArchivedChange: (Boolean) -> Unit = {},
        onOpenProduct: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenCreate: () -> Unit = {},
        onBackFromCreate: () -> Unit = {},
        onCreateProduct: (ProductCatalogInput) -> Unit = {},
        onOpenEdit: () -> Unit = {},
        onBackFromEdit: () -> Unit = {},
        onUpdateProduct: (ProductCatalogInput) -> Unit = {},
        onArchiveProduct: () -> Unit = {},
        onUnarchiveProduct: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
        darkTheme: Boolean = false,
        fontScale: Float? = null,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale ?: density.fontScale)) {
            SaafTheme(darkTheme = darkTheme) {
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
                    productsShowArchived = productsShowArchived,
                    productDetail = productDetail,
                    productCreateOpen = productCreateOpen,
                    productCreating = productCreating,
                    productEditOpen = productEditOpen,
                    productUpdating = productUpdating,
                    productArchiving = productArchiving,
                    productUnarchiving = productUnarchiving,
                    productsError = productsError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onShowArchivedChange = onShowArchivedChange,
                    onOpenProduct = onOpenProduct,
                    onBackFromDetail = onBackFromDetail,
                    onOpenCreate = onOpenCreate,
                    onBackFromCreate = onBackFromCreate,
                    onCreateProduct = onCreateProduct,
                    onOpenEdit = onOpenEdit,
                    onBackFromEdit = onBackFromEdit,
                    onUpdateProduct = onUpdateProduct,
                    onArchiveProduct = onArchiveProduct,
                    onUnarchiveProduct = onUnarchiveProduct,
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
    }

    private fun sampleProduct() = NativeOwnerProductRow(
        productId = 11L,
        name = "Cream",
        unit = "Kg",
        defaultSaleRate = 500.0,
        inventoryTracking = true,
        archived = false,
        rateLabel = "PKR 500.00 / Kg",
        statusLabel = "Active",
        trackingLabel = "Stock tracked",
        notes = "Fresh",
    )

    private fun sampleArchivedProduct() = NativeOwnerProductRow(
        productId = 12L,
        name = "Old Cream",
        unit = "Kg",
        defaultSaleRate = 400.0,
        inventoryTracking = true,
        archived = true,
        rateLabel = "PKR 400.00 / Kg",
        statusLabel = "Archived",
        trackingLabel = "Stock tracked",
        notes = null,
    )
}

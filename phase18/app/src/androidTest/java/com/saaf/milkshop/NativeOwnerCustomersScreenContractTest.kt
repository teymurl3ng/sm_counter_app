package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
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
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerCustomersScreen
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerCustomersScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsCustomersWithoutCashierChrome() {
        setScreen(customers = listOf(sampleCustomer()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCustomer(21L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(customers = emptyList())
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No customers yet.").assertIsDisplayed()
    }

    @Test
    fun customerRowOpensDetail() {
        var opened: Long? = null
        setScreen(
            customers = listOf(sampleCustomer()),
            onOpenCustomer = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerCustomer(21L)).performClick()
        assertEquals(21L, opened)
    }

    @Test
    fun detailShowsFactsAndEdit() {
        var editOpened = false
        setScreen(
            customers = listOf(sampleCustomer()),
            customerDetail = sampleDetail(),
            onOpenEdit = { editOpened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailName).assertIsDisplayed()
        saveScreenshot("customer-detail-light")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailEdit)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(editOpened)
    }

    @Test
    fun longCustomerRowKeepsSignedBalanceAndCallbackAtLargeText() {
        val customer = sampleCustomer().copy(name = "Northern market community cafe and delivery counter",
            balanceLabel = "PKR -123,456,789.99", statusLabel = "Archived",
            areaRoute = "North district — community market route")
        var opened: Long? = null
        setScreen(customers = listOf(customer), fontScale = 1.5f, onOpenCustomer = { opened = it })
        assertCompleteText(customer.name)
        assertCompleteText(customer.balanceLabel)
        assertCompleteText(customer.statusLabel)
        saveScreenshot("customer-row-light-150")
        composeRule.onNodeWithTag(NativeUiTags.ownerCustomer(customer.accountId)).performClick()
        assertEquals(customer.accountId, opened)
    }

    @Test
    fun longCustomerDetailReflowsInDarkExtraLargeText() {
        val customer = sampleCustomer().copy(name = "Northern market community cafe and delivery counter",
            balanceLabel = "PKR -123,456,789.99", statusLabel = "Archived")
        val detail = sampleDetail().copy(row = customer, active = false,
            address = "12 Market Road, beside the community centre, North District")
        var edited = false
        setScreen(customers = listOf(customer), customerDetail = detail, darkTheme = true,
            fontScale = 2f, onOpenEdit = { edited = true })
        assertCompleteText(customer.name)
        assertCompleteText(customer.balanceLabel)
        saveScreenshot("customer-detail-dark-200")
        assertCompleteText(detail.address!!)
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailEdit).performScrollTo().performClick()
        assertTrue(edited)
    }

    private fun assertCompleteText(text: String) {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        // Android rounds the text container to whole pixels; a fractional last
        // glyph edge can set didOverflowWidth even on a short word. Allow that
        // rounding only, while still checking every glyph and complete lines.
        assertTrue("Text must reflow completely: $text", layouts.isNotEmpty() && layouts.all { layout ->
            !layout.didOverflowHeight && layout.getLineEnd(layout.lineCount - 1) == text.length &&
                (0 until layout.lineCount).all { line ->
                    !layout.isLineEllipsized(line) && layout.getLineRight(line) <= layout.size.width + 1f
                }
        })
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "batch6-$name.png").outputStream().use {
            composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun addCustomerOpensCreateForm() {
        var opened = false
        setScreen(
            customers = listOf(sampleCustomer()),
            onOpenCreate = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersCreateOpen).performClick()
        assertTrue(opened)
    }

    @Test
    fun createFormShowsFieldsAndSave() {
        var saved: String? = null
        setScreen(
            customers = listOf(sampleCustomer()),
            customerCreateOpen = true,
            onCreateCustomer = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersCreateForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersCreateName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersCreateName)
            .performScrollTo()
            .performTextReplacement("Fresh Route Cafe")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersCreateSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Fresh Route Cafe", saved)
    }

    @Test
    fun editFormPrefillsAndSaves() {
        var savedName: String? = null
        var savedProfile: CustomerProfileInput? = null
        setScreen(
            customers = listOf(sampleCustomer()),
            customerDetail = sampleDetail(),
            customerEditOpen = true,
            onUpdateCustomer = { name, profile ->
                savedName = name
                savedProfile = profile
            },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditPhone)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditAddress)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditAreaRoute)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditNotes)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditCreditLimit)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditDailyQuota)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditActive)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()

        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditName)
            .performScrollTo()
            .performTextReplacement("Corner Cafe Updated")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditPhone)
            .performScrollTo()
            .performTextReplacement("0311")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditCreditLimit)
            .performScrollTo()
            .performTextReplacement("15000")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersEditSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("Corner Cafe Updated", savedName)
        assertEquals("0311", savedProfile!!.phone)
        assertEquals("Main Market", savedProfile!!.address)
        assertEquals("Route A", savedProfile!!.areaRoute)
        assertEquals("Friday collect", savedProfile!!.notes)
        assertEquals(15_000.0, savedProfile!!.creditLimit!!, 0.001)
        assertEquals(25.0, savedProfile!!.dailyQuota!!, 0.001)
        assertTrue(savedProfile!!.active)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            customers = emptyList(),
            customersError = "Customers could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersRetry).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        customers: List<NativeOwnerCustomerRow>,
        customersLoading: Boolean = false,
        customerDetail: NativeOwnerCustomerDetail? = null,
        customerCreateOpen: Boolean = false,
        customerCreating: Boolean = false,
        customerEditOpen: Boolean = false,
        customerUpdating: Boolean = false,
        customersError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenCustomer: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenCreate: () -> Unit = {},
        onBackFromCreate: () -> Unit = {},
        onCreateCustomer: (String) -> Unit = {},
        onOpenEdit: () -> Unit = {},
        onBackFromEdit: () -> Unit = {},
        onUpdateCustomer: (String, CustomerProfileInput) -> Unit = { _, _ -> },
        onNavigate: (OwnerSection) -> Unit = {},
        darkTheme: Boolean = false,
        fontScale: Float? = null,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale ?: density.fontScale)) {
            SaafTheme(darkTheme = darkTheme) {
                OwnerCustomersScreen(
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
                    customers = customers,
                    customersLoading = customersLoading,
                    customerDetail = customerDetail,
                    customerCreateOpen = customerCreateOpen,
                    customerCreating = customerCreating,
                    customerEditOpen = customerEditOpen,
                    customerUpdating = customerUpdating,
                    customersError = customersError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenCustomer = onOpenCustomer,
                    onBackFromDetail = onBackFromDetail,
                    onOpenCreate = onOpenCreate,
                    onBackFromCreate = onBackFromCreate,
                    onCreateCustomer = onCreateCustomer,
                    onOpenEdit = onOpenEdit,
                    onBackFromEdit = onBackFromEdit,
                    onUpdateCustomer = onUpdateCustomer,
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

    private fun sampleCustomer() = NativeOwnerCustomerRow(
        accountId = 21L,
        name = "Corner Cafe",
        balanceLabel = "PKR 0.00",
        statusLabel = "Active",
        phone = "0300",
        areaRoute = "Route A",
    )

    private fun sampleDetail() = NativeOwnerCustomerDetail(
        row = sampleCustomer(),
        address = "Main Market",
        notes = "Friday collect",
        creditLimit = 12_000.0,
        dailyQuota = 25.0,
        active = true,
        creditLimitLabel = "PKR 12,000.00",
        dailyQuotaLabel = "25.00 L / day",
    )
}

package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.saaf.milkshop.qa.NativeEvidenceCapture
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerExpenseCategoryRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsSnapshot
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.owner.OwnerShopSettingsScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerShopSettingsScreenContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun contentShowsSettingsWithoutCashierChrome() {
        setScreen(snapshot = sampleSnapshot())
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsContent).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRatesSection).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
        composeRule.onNodeWithText("Read only in this preview. Editing shop settings comes next.")
            .assertDoesNotExist()
    }

    @Test
    fun editIdentityOpensForm() {
        var opened = false
        setScreen(
            snapshot = sampleSnapshot(),
            onOpenIdentityEdit = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsEditIdentity).performClick()
        assertTrue(opened)
    }

    @Test
    fun identityFormShowsFieldsAndSave() {
        var saved: StoreIdentityInput? = null
        setScreen(
            snapshot = sampleSnapshot(),
            identityEditOpen = true,
            onSaveIdentity = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsIdentityForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsIdentityName)
            .performScrollTo()
            .performTextReplacement("Updated Shop")
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsIdentityPhone)
            .performScrollTo()
            .performTextReplacement("0311")
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsIdentitySave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals("Updated Shop", saved?.storeName)
        assertEquals("0311", saved?.phone)
    }

    @Test
    fun editRatesOpensForm() {
        var opened = false
        setScreen(
            snapshot = sampleSnapshot(),
            onOpenRatesEdit = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsEditRates)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun ratesFormShowsFieldsAndSave() {
        var saved: BusinessSettings? = null
        setScreen(
            snapshot = sampleSnapshot(),
            ratesEditOpen = true,
            onUpdateRates = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRatesForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRatesMilkVendor)
            .performScrollTo()
            .performTextReplacement("190")
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRatesSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(190.0, saved?.milkRateVendor ?: 0.0, 1e-9)
        assertEquals(5.0, saved?.deliveryFeeVendor ?: 0.0, 1e-9)
    }

    @Test
    fun addCategoryOpensCreateForm() {
        var opened = false
        setScreen(
            snapshot = sampleSnapshot(),
            onOpenCategoryCreate = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsAddCategory)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun categoryCreateFormShowsFieldsAndSave() {
        var saved: String? = null
        setScreen(
            snapshot = sampleSnapshot(),
            categoryCreateOpen = true,
            onCreateCategory = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryCreateForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryCreateName)
            .performScrollTo()
            .performTextReplacement("Generator Fuel")
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryCreateSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals("Generator Fuel", saved)
    }

    @Test
    fun categoryDetailArchivesWithConfirm() {
        var archived: Boolean? = null
        setScreen(
            snapshot = sampleSnapshot(),
            categoryDetail = sampleCategory(active = true),
            onSetCategoryArchived = { archived = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryArchive)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryArchiveConfirm).performClick()
        assertEquals(true, archived)
    }

    @Test
    fun openingSectionShowsRecordActionWhenAllowed() {
        setScreen(snapshot = sampleSnapshot(canRecordOpening = true))
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningSection)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningRecord)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun openingSectionShowsNoticeWhenIdentityMissing() {
        setScreen(snapshot = sampleSnapshot(hasStoreIdentity = false, canRecordOpening = false))
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningNotice)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            snapshot = null,
            error = "Shop settings could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRetry).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun openingAndCategoriesSectionsArePresent() {
        setScreen(snapshot = sampleSnapshot())
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningSection)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoriesSection)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerShopExpenseCategory(9L))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun categoryRowOpensDetail() {
        var opened: Long? = null
        setScreen(
            snapshot = sampleSnapshot(),
            onOpenCategory = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerShopExpenseCategory(9L))
            .performScrollTo()
            .performClick()
        assertEquals(9L, opened)
    }

    @Test
    fun recordedOpeningKeepsCorrectionAction() {
        var correction: Boolean? = null
        setScreen(snapshot = sampleSnapshot(openingRecorded = true, canRecordOpening = false),
            onOpenOpeningEdit = { correction = it })
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningRecord).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningCorrect).performScrollTo().performClick()
        assertEquals(true, correction)
    }

    @Test
    fun businessActivityBlocksInitialOpening() {
        setScreen(snapshot = sampleSnapshot(canRecordOpening = false))
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningRecord).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningCorrect).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsOpeningNotice).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun emptyCategoriesKeepAddAction() {
        var opened = false
        setScreen(snapshot = sampleSnapshot().copy(expenseCategories = emptyList()), onOpenCategoryCreate = { opened = true })
        composeRule.onNodeWithText("No expense categories yet.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsAddCategory).performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun categoryDetailRestoresWithConfirm() {
        var archived: Boolean? = null
        setScreen(snapshot = sampleSnapshot(), categoryDetail = sampleCategory(active = false),
            onSetCategoryArchived = { archived = it })
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryUnarchive).performScrollTo().performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryUnarchiveConfirm).performClick()
        assertEquals(false, archived)
    }

    @Test fun overviewGeometryDefault() = overviewGeometry(1f, false)
    @Test fun overviewGeometryLarge() = overviewGeometry(1.5f, false)
    @Test fun overviewGeometryExtraLargeDark() = overviewGeometry(2f, true)

    private fun overviewGeometry(scale: Float, dark: Boolean) {
        val address = "12 Market Road, beside the community centre, North District"
        val money = "PKR 123,456,789,012.99"
        val snapshot = sampleSnapshot(openingRecorded = true, canRecordOpening = false).copy(
            shopName = "North District community dairy and grocery market",
            address = address, vendorMilkRateLabel = money,
            openingNotes = "Verified starting balances recorded before trading. Original figures remain in history.",
            openingInventoryCount = 5, openingAccountCount = 12,
            expenseCategories = listOf(sampleCategory().copy(name = "Building maintenance and shop supplies")),
        )
        setScreen(snapshot = snapshot, fontScale = scale, dark = dark)
        val top = composeRule.onNodeWithText("Shop settings").fetchSemanticsNode().boundsInRoot
        val bottom = composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot
        val prefix = "overview-$scale-${if (dark) "dark" else "light"}"
        fun capture(part: String) = NativeEvidenceCapture.captureWholeWindow(composeRule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "settings-grouping", scenario = "$prefix-$part", workflowId = "NativeOwnerShopSettingsScreenContractTest",
            fontScale = scale.toString(), theme = if (dark) "dark" else "light",
            note = "Long-content Shop settings fixture; no business data written. User review pending."))
        for (heading in listOf("Shop details", "Rates", "Opening balances", "Expense categories")) {
            assertTextRole(heading, 16f, androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        for (subgroup in listOf("Supplier", "Retail & delivery")) {
            assertTextRole(subgroup, 14f, androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        composeRule.onNodeWithText(snapshot.shopName, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        val nameLayout = textLayout(snapshot.shopName)
        assertTrue("Dynamic shop name remains complete", !nameLayout.hasVisualOverflow)
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsEditIdentity).performScrollTo()
        capture("top")
        for (value in listOf(address, money)) {
            composeRule.onNodeWithText(value, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(value, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Essential value is complete", layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
        }
        composeRule.onNodeWithText("Supplier", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        capture("rates")
        composeRule.onNodeWithText("Opening balances", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        capture("opening")
        composeRule.onNodeWithTag(NativeUiTags.ownerShopExpenseCategory(9L)).performScrollTo().assertIsDisplayed()
        capture("categories")
        assertEquals("Body scrolling keeps header stable", top, composeRule.onNodeWithText("Shop settings").fetchSemanticsNode().boundsInRoot)
        assertEquals("Body scrolling keeps navigation stable", bottom, composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).fetchSemanticsNode().boundsInRoot)
    }

    @Test fun categoryGeometryDefault() = categoryGeometry(1f, false)
    @Test fun categoryGeometryLargeDark() = categoryGeometry(1.5f, true)

    private fun categoryGeometry(scale: Float, dark: Boolean) {
        setScreen(snapshot = sampleSnapshot(), categoryDetail = sampleCategory().copy(name = "Building maintenance and shop supplies"),
            fontScale = scale, dark = dark)
        assertTextRole("Category details", 16f, androidx.compose.ui.text.font.FontWeight.SemiBold)
        composeRule.onNodeWithText("Building maintenance and shop supplies", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTrue(!textLayout("Building maintenance and shop supplies").hasVisualOverflow)
        if (scale == 1f) {
            for ((label, value) in listOf("Account" to "#${sampleCategory().accountId}", "Status" to sampleCategory().statusLabel)) {
                composeRule.onNodeWithText(value, useUnmergedTree = true).performScrollTo()
                val l = composeRule.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val v = composeRule.onNodeWithText(value, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertEquals(l.top + textLayout(label).firstBaseline, v.top + textLayout(value).firstBaseline, 1f)
            }
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsCategoryArchive).performScrollTo().assertIsDisplayed()
        NativeEvidenceCapture.captureWholeWindow(composeRule.activity, NativeEvidenceCapture.CaptureSpec(
            screen = "settings-grouping", scenario = "category-$scale-${if (dark) "dark" else "light"}", workflowId = "NativeOwnerShopSettingsScreenContractTest",
            fontScale = scale.toString(), theme = if (dark) "dark" else "light", note = "Long category detail fixture; user review pending."))
    }

    private fun textLayout(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single()
    }

    private fun assertTextRole(text: String, size: Float, weight: androidx.compose.ui.text.font.FontWeight) {
        composeRule.onNodeWithText(text, useUnmergedTree = true).performScrollTo()
        val style = textLayout(text).layoutInput.style
        assertEquals("Same-level role size: $text", size, style.fontSize.value, .01f)
        assertEquals("Same-level role weight: $text", weight, style.fontWeight)
    }

    private fun setScreen(
        snapshot: NativeOwnerShopSettingsSnapshot?,
        fontScale: Float = 1f,
        dark: Boolean = false,
        loading: Boolean = false,
        error: String? = null,
        identityEditOpen: Boolean = false,
        identitySaving: Boolean = false,
        ratesEditOpen: Boolean = false,
        ratesSaving: Boolean = false,
        categoryCreateOpen: Boolean = false,
        categoryCreating: Boolean = false,
        categoryDetail: NativeOwnerExpenseCategoryRow? = null,
        categoryArchiving: Boolean = false,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenIdentityEdit: () -> Unit = {},
        onBackFromIdentityEdit: () -> Unit = {},
        onSaveIdentity: (StoreIdentityInput) -> Unit = {},
        onOpenRatesEdit: () -> Unit = {},
        onBackFromRatesEdit: () -> Unit = {},
        onUpdateRates: (BusinessSettings) -> Unit = {},
        onOpenCategoryCreate: () -> Unit = {},
        onBackFromCategoryCreate: () -> Unit = {},
        onCreateCategory: (String) -> Unit = {},
        onOpenCategory: (Long) -> Unit = {},
        onBackFromCategoryDetail: () -> Unit = {},
        onSetCategoryArchived: (Boolean) -> Unit = {},
        onOpenOpeningEdit: (Boolean) -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            SaafTheme(darkTheme = dark) {
                OwnerShopSettingsScreen(
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
                    snapshot = snapshot,
                    loading = loading,
                    error = error,
                    identityEditOpen = identityEditOpen,
                    identitySaving = identitySaving,
                    ratesEditOpen = ratesEditOpen,
                    ratesSaving = ratesSaving,
                    categoryCreateOpen = categoryCreateOpen,
                    categoryCreating = categoryCreating,
                    categoryDetail = categoryDetail,
                    categoryArchiving = categoryArchiving,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenIdentityEdit = onOpenIdentityEdit,
                    onBackFromIdentityEdit = onBackFromIdentityEdit,
                    onSaveIdentity = onSaveIdentity,
                    onOpenRatesEdit = onOpenRatesEdit,
                    onBackFromRatesEdit = onBackFromRatesEdit,
                    onUpdateRates = onUpdateRates,
                    onOpenCategoryCreate = onOpenCategoryCreate,
                    onBackFromCategoryCreate = onBackFromCategoryCreate,
                    onCreateCategory = onCreateCategory,
                    onOpenCategory = onOpenCategory,
                    onBackFromCategoryDetail = onBackFromCategoryDetail,
                    onSetCategoryArchived = onSetCategoryArchived,
                    onOpenOpeningEdit = onOpenOpeningEdit,
                    hostedScaffold = { snackbar, mod, wrapNav, content ->
                        OwnerScaffold(
                            snackbarHostState = snackbar,
                            section = OwnerSection.More,
                            onNavigate = { section -> wrapNav { } },
                            modifier = mod,
                            content = content,
                        )
                    },
                )
            }
            }
        }
    }

    private fun sampleCategory(active: Boolean = true) = NativeOwnerExpenseCategoryRow(
        accountId = 9L,
        name = "Utilities",
        statusLabel = if (active) "Active" else "Inactive",
        active = active,
    )

    private fun sampleSnapshot(
        hasStoreIdentity: Boolean = true,
        openingRecorded: Boolean = false,
        canRecordOpening: Boolean = true,
    ) = NativeOwnerShopSettingsSnapshot(
        shopName = "Saaf Milk",
        phone = "0300",
        address = "Shop Street",
        currencyCode = "PKR",
        hasStoreIdentity = hasStoreIdentity,
        vendorMilkRateLabel = "PKR 180.00",
        vendorDeliveryFeeLabel = "PKR 5.00",
        tsStandardLabel = "3.5%",
        retailMilkRateLabel = "PKR 220.00",
        retailYogurtRateLabel = "PKR 250.00",
        defaultSurchargeLabel = "PKR 10.00",
        milkRateVendor = 180.0,
        deliveryFeeVendor = 5.0,
        tsStandard = 3.5,
        retailRateMilk = 220.0,
        retailRateYogurt = 250.0,
        defaultDeliverySurcharge = 10.0,
        openingRecorded = openingRecorded,
        canRecordOpening = canRecordOpening,
        openingDate = if (openingRecorded) "2026-01-01" else null,
        openingDateLabel = if (openingRecorded) "2026-01-01" else "Not recorded",
        openingNotes = null,
        openingInventoryCount = 0,
        openingAccountCount = 0,
        openingProducts = emptyList(),
        openingAccounts = emptyList(),
        expenseCategories = listOf(sampleCategory()),
    )
}

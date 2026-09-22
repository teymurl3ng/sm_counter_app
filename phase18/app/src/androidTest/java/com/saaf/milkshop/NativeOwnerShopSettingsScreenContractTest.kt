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
    val composeRule = createComposeRule()

    @Test
    fun contentShowsSettingsWithoutCashierChrome() {
        setScreen(snapshot = sampleSnapshot())
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsContent).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsRatesSection).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
        composeRule.onNodeWithText("Read only in this preview. Editing shop settings comes next.")
            .assertDoesNotExist()
    }

    @Test
    fun backToMenuFromContent() {
        var back = false
        setScreen(snapshot = sampleSnapshot(), onBackToMenu = { back = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsBack).performClick()
        assertTrue(back)
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

    private fun setScreen(
        snapshot: NativeOwnerShopSettingsSnapshot?,
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
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
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

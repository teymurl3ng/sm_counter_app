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
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsSnapshot
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
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
    }

    @Test
    fun backToMenuFromContent() {
        var back = false
        setScreen(snapshot = sampleSnapshot(), onBackToMenu = { back = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsBack).performClick()
        assertTrue(back)
    }

    @Test
    fun editPreviewSurfacesNotice() {
        var preview = false
        setScreen(snapshot = sampleSnapshot(), onEditPreview = { preview = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerShopSettingsEditPreview).performClick()
        assertTrue(preview)
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
    }

    private fun setScreen(
        snapshot: NativeOwnerShopSettingsSnapshot?,
        loading: Boolean = false,
        error: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onEditPreview: () -> Unit = {},
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
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onEditPreview = onEditPreview,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleSnapshot() = NativeOwnerShopSettingsSnapshot(
        shopName = "Saaf Milk",
        phone = "0300",
        address = "Shop Street",
        currencyCode = "PKR",
        vendorMilkRateLabel = "PKR 180.00",
        vendorDeliveryFeeLabel = "PKR 5.00",
        tsStandardLabel = "3.5%",
        retailMilkRateLabel = "PKR 220.00",
        retailYogurtRateLabel = "PKR 250.00",
        defaultSurchargeLabel = "PKR 0.00",
        openingDateLabel = "2026-01-01",
        openingNotes = null,
        expenseCategories = emptyList(),
    )
}

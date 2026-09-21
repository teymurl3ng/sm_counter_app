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
import com.saaf.milkshop.application.owner.NativeOwnerCustomerDetail
import com.saaf.milkshop.application.owner.NativeOwnerCustomerRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerCustomersScreen
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
    fun detailShowsFactsAndBack() {
        var back = false
        setScreen(
            customers = listOf(sampleCustomer()),
            customerDetail = NativeOwnerCustomerDetail(
                row = sampleCustomer(),
                address = "Main Market",
                notes = "Friday collect",
                creditLimitLabel = "PKR 12,000.00",
                dailyQuotaLabel = "25.00 L / day",
            ),
            onBackFromDetail = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailName).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailReadOnly)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersDetailBack).performClick()
        assertTrue(back)
    }

    @Test
    fun backToMenuFromList() {
        var back = false
        setScreen(
            customers = listOf(sampleCustomer()),
            onBackToMenu = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersBack).performClick()
        assertTrue(back)
    }

    @Test
    fun createPreviewSurfacesNotice() {
        var preview = false
        setScreen(
            customers = listOf(sampleCustomer()),
            onCreatePreview = { preview = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCustomersCreatePreview).performClick()
        assertTrue(preview)
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
        customersError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenCustomer: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onCreatePreview: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
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
                    customersError = customersError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenCustomer = onOpenCustomer,
                    onBackFromDetail = onBackFromDetail,
                    onCreatePreview = onCreatePreview,
                    onNavigate = onNavigate,
                )
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
}

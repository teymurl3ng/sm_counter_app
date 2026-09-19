package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.sale.CashierSaleScreen
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeSaleScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saleLoadFailureRetryChangesToVisibleLoadingState() {
        var retried = false
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                var state: NativeSaleState by remember {
                    mutableStateOf(NativeSaleState.LoadFailure("Today’s sale details are unavailable."))
                }
                CashierSaleScreen(
                    snackbarHostState = remember { SnackbarHostState() },
                    identity = NativeSignedInIdentity(
                        userId = 1L,
                        loginName = "contract.owner",
                        displayName = "Contract Owner",
                        role = AppRole.OWNER,
                        effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
                    ),
                    state = state,
                    onRetry = {
                        retried = true
                        state = NativeSaleState.Loading
                    },
                    onClearFeedback = {},
                    onRecordSale = {},
                    onLeaveConfirmed = {},
                    onNavigate = {},
                )
            }
        }

        composeRule.onNodeWithText("Today’s sale details are unavailable.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.CashierRetryLoad).performClick()
        composeRule.onNodeWithText("Loading today’s sale details…").assertIsDisplayed()
        assertTrue(retried)
    }
}

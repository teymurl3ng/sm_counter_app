package com.saaf.milkshop

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.drawing.*
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.drawing.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeDrawingScreenContractTest {
    @get:Rule val compose = createComposeRule()
    private val account = NativeDrawingPaymentAccount(10, "Cash")
    private val context = NativeDrawingContext("2026-10-10", true, account)
    private fun screen(state: NativeDrawingState.Ready, record: (NativeDrawingRequest) -> Unit = {},
        another: () -> Unit = {}, back: () -> Unit = {}) {
        compose.setContent {
            SaafTheme {
                OwnerDrawingEntryScreen(remember { SnackbarHostState() },
                    NativeSignedInIdentity(1, "owner", "Owner", AppRole.OWNER, RolePermissions.permissions(AppRole.OWNER)),
                    state, {}, record, another, back, ownScaffold = { _, _, content -> content(PaddingValues()) })
            }
        }
    }
    @Test fun drawingAmountAndNoteSubmitExactlyOnce() {
        val requests = mutableListOf<NativeDrawingRequest>()
        screen(NativeDrawingState.Ready(context), requests::add)
        compose.onNodeWithTag(NativeUiTags.OwnerDrawingEntryRoot).assertExists()
        compose.onNodeWithTag(NativeUiTags.DrawingForm).assertIsDisplayed()
        compose.onNodeWithTag(NativeUiTags.DrawingRecord).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag(NativeUiTags.DrawingAmountInput).performScrollTo().performTextReplacement("125.50")
        compose.onNodeWithTag(NativeUiTags.DrawingAmountInput).performImeAction()
        compose.onNodeWithTag(NativeUiTags.DrawingNoteToggle).performScrollTo().performClick()
        compose.onNodeWithTag(NativeUiTags.DrawingNoteInput).performTextReplacement("Home use")
        compose.onNodeWithTag(NativeUiTags.DrawingRecord).performScrollTo().performClick()
        assertEquals(NativeDrawingRequest(12_550, "Home use"), requests.single())
    }
    @Test fun closedDrawingCannotSubmit() {
        screen(NativeDrawingState.Ready(context.copy(postingOpen = false)))
        compose.onNodeWithTag(NativeUiTags.DrawingPostingClosed).assertIsDisplayed()
        compose.onNodeWithTag(NativeUiTags.DrawingRecord).performScrollTo().assertIsNotEnabled()
    }
    @Test fun drawingErrorRemainsRecoverable() {
        screen(NativeDrawingState.Ready(context, error = "Could not save drawing."))
        compose.onNodeWithTag(NativeUiTags.DrawingError).performScrollTo().assertIsDisplayed()
    }
    @Test fun drawingSuccessOffersAnotherAndBack() {
        var another = 0; var back = 0
        screen(NativeDrawingState.Ready(context, receipt = NativeDrawingReceipt(42, 12550, account)),
            another = { another++ }, back = { back++ })
        compose.onNodeWithTag(NativeUiTags.DrawingSuccess).assertIsDisplayed()
        compose.onNodeWithTag(NativeUiTags.DrawingAnother).performScrollTo().performClick()
        compose.onNodeWithTag(NativeUiTags.DrawingBackToWork).performScrollTo().performClick()
        assertEquals(1, another); assertEquals(1, back)
    }
}

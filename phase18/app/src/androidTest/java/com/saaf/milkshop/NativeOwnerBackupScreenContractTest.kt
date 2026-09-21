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
import com.saaf.milkshop.application.owner.NativeOwnerBackupSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerBackupScreen
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerBackupScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentShowsStatusWithoutCashierChrome() {
        setScreen(snapshot = sampleSnapshot())
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupContent).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupStatusHeadline).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun backToMenuFromContent() {
        var back = false
        setScreen(snapshot = sampleSnapshot(), onBackToMenu = { back = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupBack).performClick()
        assertTrue(back)
    }

    @Test
    fun exportPreviewSurfacesNotice() {
        var preview = false
        setScreen(snapshot = sampleSnapshot(), onExportPreview = { preview = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupExportPreview)
            .performScrollTo()
            .performClick()
        assertTrue(preview)
    }

    @Test
    fun restorePreviewSurfacesNotice() {
        var preview = false
        setScreen(snapshot = sampleSnapshot(), onRestorePreview = { preview = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestorePreview)
            .performScrollTo()
            .performClick()
        assertTrue(preview)
    }

    @Test
    fun connectPreviewSurfacesNotice() {
        var preview = false
        setScreen(snapshot = sampleSnapshot(), onConnectPreview = { preview = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnectPreview)
            .performScrollTo()
            .performClick()
        assertTrue(preview)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            snapshot = null,
            error = "Backup status could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRetry).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        snapshot: NativeOwnerBackupSnapshot?,
        loading: Boolean = false,
        error: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onExportPreview: () -> Unit = {},
        onRestorePreview: () -> Unit = {},
        onConnectPreview: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerBackupScreen(
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
                    onExportPreview = onExportPreview,
                    onRestorePreview = onRestorePreview,
                    onConnectPreview = onConnectPreview,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleSnapshot() = NativeOwnerBackupSnapshot(
        configured = false,
        enabled = false,
        accountDisplay = null,
        scheduleDescription = "Google Drive vault is not configured on this device.",
        lastSuccessAt = null,
        lastAttemptAt = null,
        lastErrorMessage = null,
        stale = false,
        pending = false,
        statusHeadline = "Backup not configured",
    )
}

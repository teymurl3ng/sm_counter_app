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
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditDetail
import com.saaf.milkshop.application.owner.NativeOwnerSecurityAuditRow
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.owner.OwnerSecurityHistoryScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerSecurityHistoryScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsAuditsWithoutCashierChrome() {
        setScreen(audits = listOf(sampleAudit()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerSecurityAudit(11L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(audits = emptyList())
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No security audit entries yet.").assertIsDisplayed()
    }

    @Test
    fun auditRowOpensDetail() {
        var opened: Long? = null
        setScreen(audits = listOf(sampleAudit()), onOpenAudit = { opened = it })
        composeRule.onNodeWithTag(NativeUiTags.ownerSecurityAudit(11L)).performClick()
        assertEquals(11L, opened)
    }

    @Test
    fun detailShowsFactsAndBack() {
        var back = false
        setScreen(
            audits = listOf(sampleAudit()),
            auditDetail = NativeOwnerSecurityAuditDetail(
                row = sampleAudit(),
                reasonCode = "owner_review",
                targetUserId = 3L,
                details = "Passphrase reset",
            ),
            onBackFromDetail = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryDetailAction).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryDetailReadOnly)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryDetailBack).performClick()
        assertTrue(back)
    }

    @Test
    fun backToMenuFromList() {
        var back = false
        setScreen(audits = listOf(sampleAudit()), onBackToMenu = { back = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryBack).performClick()
        assertTrue(back)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            audits = emptyList(),
            auditsError = "Security history could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerSecurityHistoryRetry).performClick()
        assertEquals(1, retries)
    }

    private fun setScreen(
        audits: List<NativeOwnerSecurityAuditRow>,
        auditsLoading: Boolean = false,
        auditDetail: NativeOwnerSecurityAuditDetail? = null,
        auditsError: String? = null,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenAudit: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
    ) {
        composeRule.setContent {
            SaafTheme(darkTheme = false) {
                OwnerSecurityHistoryScreen(
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
                    audits = audits,
                    auditsLoading = auditsLoading,
                    auditDetail = auditDetail,
                    auditsError = auditsError,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenAudit = onOpenAudit,
                    onBackFromDetail = onBackFromDetail,
                    onNavigate = onNavigate,
                )
            }
        }
    }

    private fun sampleAudit() = NativeOwnerSecurityAuditRow(
        auditId = 11L,
        occurredAt = "2026-09-21T10:00:00Z",
        actionCode = "PASS_PHRASE_RESET",
        resultCode = "SUCCESS",
        actorLabel = "Owner · Owner",
        detailsPreview = "Passphrase reset",
    )
}

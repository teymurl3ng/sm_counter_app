package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.sale.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeGuardRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun outgoingPaneDisposalCannotClearIncomingGuard() {
        var outgoing by mutableStateOf(true)
        var incoming by mutableStateOf(false)
        var active by mutableStateOf<EntryLeaveGuard?>(null)
        val first = EntryLeaveGuard()
        val second = EntryLeaveGuard().also { it.markDirty() }
        var releaseFirst: (() -> Unit)? = null
        compose.setContent {
            if (outgoing) key("outgoing") {
                val register = rememberPaneGuardRegistration({ active }) { active = it }
                DisposableEffect(Unit) {
                    register(first); releaseFirst = { register(null) }
                    onDispose { register(null) }
                }
            }
            if (incoming) key("incoming") {
                val register = rememberPaneGuardRegistration({ active }) { active = it }
                DisposableEffect(Unit) {
                    register(second)
                    // Exercise disposal before Compose can recompose the old
                    // pane with the newly published guard value.
                    releaseFirst?.invoke()
                    onDispose { register(null) }
                }
            }
        }
        compose.runOnIdle { incoming = true }
        compose.waitForIdle()
        assertSame(second, active)
        compose.runOnIdle { outgoing = false }
        compose.waitForIdle()
        assertSame(second, active)
        assertTrue(active!!.dirty)
    }
    @Test fun saleDiscardClearsRealFormBeforeLeaving() {
        lateinit var guard: EntryLeaveGuard
        var left = 0
        val context = NativeSaleContext("2026-10-10", true,
            listOf(NativeSaleProduct(1, "Milk", "Liters", 19000, "10 L")),
            emptyList(), listOf(NativeSaleAccount(10, "Cash")), 10, 0, 0, 50.0)
        compose.setContent {
            SaafTheme {
                CashierSaleScreen(remember { SnackbarHostState() },
                    NativeSignedInIdentity(1, "owner", "Owner", AppRole.OWNER, RolePermissions.permissions(AppRole.OWNER)),
                    NativeSaleState.Ready(context), {}, {}, {}, {}, {}, {},
                    onRegisterGuard = { if (it != null) guard = it })
            }
        }
        compose.onNodeWithTag(NativeUiTags.CashierEntryInput).performTextReplacement("3.5")
        compose.onNodeWithTag(NativeUiTags.CashierEntryInput).performImeAction()
        compose.runOnIdle { guard.requestLeave { left++ } }
        compose.onNodeWithTag(NativeUiTags.CashierKeepDraft).performClick()
        assertEquals(0, left)
        compose.onNodeWithTag(NativeUiTags.CashierEntryInput).assertTextEquals("3.5")
        compose.runOnIdle { guard.requestLeave { left++ } }
        compose.onNodeWithTag(NativeUiTags.CashierDiscardDraft).performClick()
        compose.onNodeWithTag(NativeUiTags.CashierEntryInput)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        assertEquals(1, left)
        assertFalse(guard.dirty)
    }
}

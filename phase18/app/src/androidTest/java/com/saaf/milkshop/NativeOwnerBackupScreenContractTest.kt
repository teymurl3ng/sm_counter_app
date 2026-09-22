package com.saaf.milkshop

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerBackupInspection
import com.saaf.milkshop.application.owner.NativeOwnerBackupSnapshot
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.OwnerBackupScreen
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
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
    fun exportOpensExportForm() {
        var opened = false
        setScreen(snapshot = sampleSnapshot(), onOpenExport = { opened = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupExport)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun restoreOpensRestoreForm() {
        var opened = false
        setScreen(snapshot = sampleSnapshot(), onOpenRestore = { opened = true })
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestore)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun connectOpensConnectFormWhenAvailable() {
        var opened = false
        setScreen(
            snapshot = sampleSnapshot(cloudActionsAvailable = true),
            onOpenConnect = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnect)
            .performScrollTo()
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun configuredDriveShowsEnablePauseRunNowDisconnect() {
        var enabled: Boolean? = null
        var ran = false
        var disconnectRequested = false
        setScreen(
            snapshot = sampleSnapshot(
                configured = true,
                enabled = true,
                cloudActionsAvailable = true,
                statusHeadline = "Drive vault protected",
                accountDisplay = "owner@example.com",
            ),
            onSetDriveEnabled = { enabled = it },
            onRunDriveNow = { ran = true },
            onRequestDisconnect = { disconnectRequested = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupPause)
            .performScrollTo()
            .performClick()
        assertEquals(false, enabled)
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRunNow)
            .performScrollTo()
            .performClick()
        assertTrue(ran)
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupDisconnect)
            .performScrollTo()
            .performClick()
        assertTrue(disconnectRequested)
    }

    @Test
    fun exportFormShowsPassphraseControls() {
        var back = false
        setScreen(
            snapshot = sampleSnapshot(),
            exportOpen = true,
            onBackFromExport = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupExportForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupExportPassphrase).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupExportConfirm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupExportSubmit).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupExportBack).performClick()
        assertTrue(back)
    }

    @Test
    fun restoreFormShowsPickControls() {
        var back = false
        setScreen(
            snapshot = sampleSnapshot(),
            restoreOpen = true,
            onBackFromRestore = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestoreForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestorePassphrase).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestorePick).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestoreBack).performClick()
        assertTrue(back)
    }

    @Test
    fun connectFormShowsBack() {
        var back = false
        setScreen(
            snapshot = sampleSnapshot(cloudActionsAvailable = true),
            connectOpen = true,
            onBackFromConnect = { back = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnectBack).performClick()
        assertTrue(back)
    }

    @Test
    fun pausedDriveShowsEnable() {
        var enabled: Boolean? = null
        setScreen(
            snapshot = sampleSnapshot(
                configured = true,
                enabled = false,
                cloudActionsAvailable = true,
                statusHeadline = "Drive vault paused",
                accountDisplay = "owner@example.com",
            ),
            onSetDriveEnabled = { enabled = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupEnable)
            .performScrollTo()
            .performClick()
        assertEquals(true, enabled)
    }

    @Test
    fun disconnectConfirmOffersConfirmAndCancel() {
        var cancelled = false
        setScreen(
            snapshot = sampleSnapshot(
                configured = true,
                enabled = true,
                cloudActionsAvailable = true,
                statusHeadline = "Drive vault protected",
                accountDisplay = "owner@example.com",
            ),
            disconnectConfirmOpen = true,
            onCancelDisconnect = { cancelled = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupDisconnectCancel).performClick()
        assertTrue(cancelled)
    }

    @Test
    fun disconnectConfirmConfirmsDisconnect() {
        var confirmed = false
        setScreen(
            snapshot = sampleSnapshot(
                configured = true,
                enabled = true,
                cloudActionsAvailable = true,
                statusHeadline = "Drive vault protected",
                accountDisplay = "owner@example.com",
            ),
            disconnectConfirmOpen = true,
            onConfirmDisconnect = { confirmed = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupDisconnectConfirm).performClick()
        assertTrue(confirmed)
    }

    @Test
    fun reauthFormShowsPassphraseControls() {
        var cancelled = false
        setScreen(
            snapshot = sampleSnapshot(),
            reauthOpen = true,
            onCancelReauth = { cancelled = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupReauth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupReauthPassphrase).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupReauthConfirm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupReauthCancel).performClick()
        assertTrue(cancelled)
    }

    @Test
    fun restoreInspectionShowsConfirmControls() {
        var confirmed: Boolean? = null
        setScreen(
            snapshot = sampleSnapshot(),
            restoreOpen = true,
            inspection = NativeOwnerBackupInspection(
                restoreTicket = "ticket-1",
                createdAt = "2026-09-22T00:00:00Z",
                formatVersion = 2,
                containsSecurityState = true,
                fileName = "test.milkbackup",
            ),
            onConfirmRestore = { confirmed = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestoreInspection).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestoreConfirmCheck)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupRestoreSubmit)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(true, confirmed)
    }

    @Test
    fun connectFormCollectsRecoveryPassphrase() {
        var submitted: Pair<String, Boolean>? = null
        setScreen(
            snapshot = sampleSnapshot(cloudActionsAvailable = true),
            connectOpen = true,
            onConnectDrive = { chars, metered ->
                submitted = String(chars) to metered
                chars.fill('\u0000')
            },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnectForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnectPassphrase)
            .performTextInput("portable backup phrase 2026!!")
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnectPassphraseConfirm)
            .performTextInput("portable backup phrase 2026!!")
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnectAllowMetered)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerBackupConnectSubmit)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals("portable backup phrase 2026!!", submitted?.first)
        assertTrue(submitted != null)
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
        exportOpen: Boolean = false,
        restoreOpen: Boolean = false,
        connectOpen: Boolean = false,
        inspection: NativeOwnerBackupInspection? = null,
        busy: Boolean = false,
        reauthOpen: Boolean = false,
        reauthBusy: Boolean = false,
        disconnectConfirmOpen: Boolean = false,
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onOpenExport: () -> Unit = {},
        onBackFromExport: () -> Unit = {},
        onExport: (Uri, CharArray) -> Unit = { _, passphrase -> passphrase.fill('\u0000') },
        onOpenRestore: () -> Unit = {},
        onBackFromRestore: () -> Unit = {},
        onInspectRestore: (Uri, CharArray) -> Unit = { _, passphrase -> passphrase.fill('\u0000') },
        onConfirmRestore: (Boolean) -> Unit = {},
        onOpenConnect: () -> Unit = {},
        onBackFromConnect: () -> Unit = {},
        onConnectDrive: (CharArray, Boolean) -> Unit = { passphrase, _ -> passphrase.fill('\u0000') },
        onSetDriveEnabled: (Boolean) -> Unit = {},
        onRunDriveNow: () -> Unit = {},
        onRequestDisconnect: () -> Unit = {},
        onConfirmDisconnect: () -> Unit = {},
        onCancelDisconnect: () -> Unit = {},
        onSubmitReauth: (CharArray) -> Unit = { it.fill('\u0000') },
        onCancelReauth: () -> Unit = {},
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
                    exportOpen = exportOpen,
                    restoreOpen = restoreOpen,
                    connectOpen = connectOpen,
                    inspection = inspection,
                    busy = busy,
                    reauthOpen = reauthOpen,
                    reauthBusy = reauthBusy,
                    disconnectConfirmOpen = disconnectConfirmOpen,
                    onBackToMenu = onBackToMenu,
                    onRetry = onRetry,
                    onOpenExport = onOpenExport,
                    onBackFromExport = onBackFromExport,
                    onExport = onExport,
                    onOpenRestore = onOpenRestore,
                    onBackFromRestore = onBackFromRestore,
                    onInspectRestore = onInspectRestore,
                    onConfirmRestore = onConfirmRestore,
                    onOpenConnect = onOpenConnect,
                    onBackFromConnect = onBackFromConnect,
                    onConnectDrive = onConnectDrive,
                    onSetDriveEnabled = onSetDriveEnabled,
                    onRunDriveNow = onRunDriveNow,
                    onRequestDisconnect = onRequestDisconnect,
                    onConfirmDisconnect = onConfirmDisconnect,
                    onCancelDisconnect = onCancelDisconnect,
                    onSubmitReauth = onSubmitReauth,
                    onCancelReauth = onCancelReauth,
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

    private fun sampleSnapshot(
        configured: Boolean = false,
        enabled: Boolean = false,
        cloudActionsAvailable: Boolean = false,
        statusHeadline: String = "Backup not configured",
        accountDisplay: String? = null,
    ) = NativeOwnerBackupSnapshot(
        configured = configured,
        enabled = enabled,
        accountDisplay = accountDisplay,
        scheduleDescription = if (configured) {
            "Encrypted Google Drive vault"
        } else {
            "Google Drive vault is not configured on this device."
        },
        lastSuccessAt = null,
        lastAttemptAt = null,
        lastErrorMessage = null,
        stale = false,
        pending = false,
        statusHeadline = statusHeadline,
        allowMetered = false,
        cloudActionsAvailable = cloudActionsAvailable,
    )
}

package com.saaf.milkshop

import android.net.Uri
import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BackupBridgeHandler
import com.saaf.milkshop.application.bridge.BackupDocumentPicker
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.hybrid.HybridActivity
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase8BackupRestoreBridgeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var documents: FakeBackupDocuments
    private lateinit var dispatcher: HybridBridgeDispatcher
    private lateinit var backupFile: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        documents = FakeBackupDocuments()
        backupFile = File(context.cacheDir, "phase8_bridge_${System.nanoTime()}.milkbackup")
        documents.createUri = Uri.fromFile(backupFile)
        documents.openUri = Uri.fromFile(backupFile)
        dispatcher = HybridBridgeDispatcher(database, security, backupManager = BackupManager(context), backupDocuments = documents)
    }

    @After
    fun tearDown() {
        dispatcher.close()
        database.close()
        backupFile.delete()
    }

    @Test
    fun exportUsesNativeDocumentBoundaryAndAdvertisesAllNinetyOneActions() = runBlocking {
        val owner = security.bootstrapOwner("backup.owner", "Backup Owner", OWNER_PASSWORD.toCharArray())
        val response = dispatch("system.exportEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), owner.session.sessionId)
        assertTrue(response.toString(), response.getBoolean("ok"))
        val data = response.getJSONObject("data")
        assertEquals(BackupManager.FORMAT_VERSION, data.getInt("formatVersion"))
        assertTrue(data.getString("createdAt").isNotBlank())
        assertEquals(1, documents.createCount)
        assertTrue(backupFile.isFile)
        assertFalse(backupFile.readText().contains("backup.owner"))
        assertTrue(database.securityAuditDao().all().any { it.action == BackupBridgeHandler.ACTION_EXPORT_BACKUP })

        val actions = dispatch("system.bridgeInfo").getJSONObject("data").getJSONArray("supportedActions")
        val names = (0 until actions.length()).map(actions::getString)
        assertEquals(91, names.size)
        assertTrue(names.containsAll(listOf("system.exportEncryptedBackup", "system.inspectEncryptedBackup", "system.restoreInspectedBackup")))
        assertTrue(names.containsAll(listOf("system.cloudBackupStatus", "system.connectCloudBackup", "system.restoreInspectedCloudBackup")))
        assertError(dispatch("system.exportEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD).put("extra", true), owner.session.sessionId), "invalid_request")
    }

    @Test
    fun inspectionRejectsWrongDamagedAndCancelledFilesWithoutIssuingUsableState() = runBlocking {
        val owner = security.bootstrapOwner("inspect.owner", "Inspect Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        assertTrue(dispatch("system.exportEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), session).getBoolean("ok"))
        assertError(dispatch("system.inspectEncryptedBackup", JSONObject().put("passphrase", WRONG_BACKUP_PASSWORD), session), "backup_wrong_passphrase")
        backupFile.writeText("{}")
        assertError(dispatch("system.inspectEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), session), "backup_tampered")
        documents.openUri = null
        assertError(dispatch("system.inspectEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), session), "user_cancelled")
        documents.createUri = null
        assertError(dispatch("system.exportEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), session), "user_cancelled")
    }

    @Test
    fun inspectedOneUseTicketRestoresReplacementAndInvalidatesEverySession() = runBlocking {
        val owner = security.bootstrapOwner("restore.owner", "Restore Owner", OWNER_PASSWORD.toCharArray())
        val session = owner.session.sessionId
        assertTrue(dispatch("system.exportEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), session).getBoolean("ok"))
        val laterProduct = "Created after backup ${System.nanoTime()}"
        ProductCatalogService(database).create(ProductCatalogInput(laterProduct, "Kg", null, true, null))
        assertNotNull(database.productDao().findByName(laterProduct))

        val inspection = dispatch("system.inspectEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), session).getJSONObject("data")
        assertTrue(inspection.getBoolean("containsSecurityState"))
        assertFalse(inspection.getBoolean("requiresEmptyInstallation"))
        val ticket = inspection.getString("restoreTicket")
        assertError(dispatch("system.restoreInspectedBackup", JSONObject().put("restoreTicket", ticket).put("confirmRestore", false), session), "validation_failed")
        val restored = dispatch("system.restoreInspectedBackup", JSONObject().put("restoreTicket", ticket).put("confirmRestore", true), session)
        assertTrue(restored.toString(), restored.getBoolean("ok"))
        assertTrue(restored.getJSONObject("data").getBoolean("sessionsInvalidated"))
        assertTrue(database.productDao().findByName(laterProduct) == null)
        assertError(dispatch("query.currentSession", session = session), "session_expired")
        assertError(dispatch("system.restoreInspectedBackup", JSONObject().put("restoreTicket", ticket).put("confirmRestore", true), session), "restore_ticket_expired")
        assertTrue(database.securityAuditDao().all().any { it.action == BackupBridgeHandler.ACTION_RESTORE_BACKUP })
        assertTrue(security.login("restore.owner", OWNER_PASSWORD.toCharArray()).sessionId.isNotBlank())
    }

    @Test
    fun nativePermissionsAndTicketBindingPreventPickerAndRestoreByOtherUsers() = runBlocking {
        val owner = security.bootstrapOwner("guard.owner", "Guard Owner", OWNER_PASSWORD.toCharArray())
        val ownerSession = owner.session.sessionId
        val adminPending = security.beginUserEnrollment(ownerSession, CreateUserInput("guard.admin", "Guard Admin", "admin"))
        security.completeUserEnrollment(adminPending.enrollmentToken, ADMIN_PASSWORD.toCharArray())
        val cashierPending = security.beginUserEnrollment(ownerSession, CreateUserInput("guard.cashier", "Guard Cashier", "cashier"))
        security.completeUserEnrollment(cashierPending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val adminSession = security.login("guard.admin", ADMIN_PASSWORD.toCharArray()).sessionId
        val cashierSession = security.login("guard.cashier", CASHIER_PASSWORD.toCharArray()).sessionId

        assertTrue(dispatch("system.exportEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), adminSession).getBoolean("ok"))
        val createCount = documents.createCount
        assertError(dispatch("system.exportEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), cashierSession), "permission_denied")
        assertEquals(createCount, documents.createCount)
        val openCount = documents.openCount
        assertError(dispatch("system.inspectEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), adminSession), "permission_denied")
        assertEquals(openCount, documents.openCount)

        val ticket = dispatch("system.inspectEncryptedBackup", JSONObject().put("passphrase", BACKUP_PASSWORD), ownerSession)
            .getJSONObject("data").getString("restoreTicket")
        assertError(dispatch("system.restoreInspectedBackup", JSONObject().put("restoreTicket", ticket).put("confirmRestore", true), adminSession), "restore_ticket_expired")
        assertTrue(database.appUserDao().count() >= 3L)
    }

    @Test
    fun packagedAppCreatesInspectsAndRestoresThroughAndroidOwnedPickers() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val file = File(instrumentation.targetContext.cacheDir, "phase8_packaged_${System.nanoTime()}.milkbackup")
        HybridActivity.backupDocumentPickerForTesting = object : BackupDocumentPicker {
            override suspend fun createDocument(suggestedFileName: String?): Uri = Uri.fromFile(file)
            override suspend fun openDocument(): Uri = Uri.fromFile(file)
        }
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val webView = activityWebView(scenario)
            waitFor(webView, "document.getElementById('login-button') !== null")
            setInput(webView, "login-name", SaafMilkTestRunner.TEST_LOGIN)
            setInput(webView, "login-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            evaluate(webView, "document.getElementById('login-button').click(); true")
            waitFor(webView, "document.getElementById('backup-nav') !== null", 20_000L)
            evaluate(webView, "document.getElementById('backup-nav').click(); true")
            waitFor(webView, "document.getElementById('encrypted-backup-form') !== null")
            evaluate(webView, "document.getElementById('backup-cloud-tab').click(); true")
            waitFor(webView, "document.getElementById('connect-cloud-backup-form') !== null")
            assertEquals("true", evaluate(webView, "document.body.innerText.includes('Automatic encrypted Google Drive backup') && document.body.innerText.includes('not live synchronization')"))
            evaluate(webView, "document.getElementById('backup-create-tab').click(); true")
            waitFor(webView, "document.getElementById('encrypted-backup-form') !== null")
            setInput(webView, "backup-account-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            setInput(webView, "backup-file-passphrase", UI_BACKUP_PASSWORD)
            setInput(webView, "backup-file-passphrase-confirm", UI_BACKUP_PASSWORD)
            evaluate(webView, "document.getElementById('create-encrypted-backup-button').click(); true")
            waitFor(webView, "document.body.innerText.includes('Encrypted backup created')", 30_000L)
            assertTrue(file.isFile)
            assertFalse(file.readText().contains(SaafMilkTestRunner.TEST_LOGIN))

            evaluate(webView, "document.getElementById('backup-restore-tab').click(); true")
            waitFor(webView, "document.getElementById('inspect-backup-form') !== null")
            setInput(webView, "restore-account-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            setInput(webView, "restore-file-passphrase", UI_BACKUP_PASSWORD)
            evaluate(webView, "document.getElementById('inspect-encrypted-backup-button').click(); true")
            waitFor(webView, "document.getElementById('backup-inspection-result') !== null", 30_000L)
            assertEquals("true", evaluate(webView, "document.body.innerText.includes('Backup passed its security checks') && document.body.innerText.includes('Restore fully replaces the current app data')"))
            evaluate(webView, "document.getElementById('open-restore-confirmation').click(); true")
            waitFor(webView, "document.getElementById('confirm-restore-checkbox') !== null")
            assertEquals("true", evaluate(webView, "document.body.innerText.includes('This is not a merge')"))
            evaluate(webView, "document.getElementById('confirm-restore-checkbox').click(); true")
            setInput(webView, "final-restore-passphrase", SaafMilkTestRunner.TEST_PASSPHRASE)
            waitFor(webView, "document.getElementById('restore-inspected-backup-button').disabled === false")
            evaluate(webView, "document.getElementById('restore-inspected-backup-button').click(); true")
            waitFor(webView, "document.getElementById('login-button') !== null && document.body.innerText.includes('Backup restored safely')", 30_000L)
            assertTrue(app.database.securityAuditDao().all().any { it.action == BackupBridgeHandler.ACTION_RESTORE_BACKUP })
        } finally {
            scenario.close()
            HybridActivity.backupDocumentPickerForTesting = null
            file.delete()
            val replacement = app.userSecurity.login(SaafMilkTestRunner.TEST_LOGIN, SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray())
            app.currentSessionId = replacement.sessionId
        }
    }

    private fun assertError(response: JSONObject, code: String) {
        assertFalse(response.toString(), response.getBoolean("ok"))
        assertEquals(code, response.getJSONObject("error").getString("code"))
    }

    private suspend fun dispatch(action: String, payload: JSONObject = JSONObject(), session: String? = null): JSONObject {
        val request = JSONObject().put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("requestId", UUID.randomUUID().toString()).put("action", action).put("payload", payload)
        if (session != null) request.put("sessionHandle", session)
        return JSONObject(dispatcher.dispatch(request.toString()))
    }

    private fun activityWebView(scenario: ActivityScenario<HybridActivity>): WebView {
        val holder = AtomicReference<WebView>()
        scenario.onActivity { holder.set(it.webViewForTesting()) }
        return holder.get()
    }

    private fun setInput(webView: WebView, id: String, value: String) {
        evaluate(webView, """(() => { const input = document.getElementById(${JSONObject.quote(id)}); const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; setter.call(input, ${JSONObject.quote(value)}); input.dispatchEvent(new Event('input', { bubbles: true })); return input.value; })()""")
    }

    private fun waitFor(webView: WebView, expression: String, timeoutMs: Long = 10_000L) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (evaluate(webView, "Boolean($expression)") == "true") return
            SystemClock.sleep(100)
        }
        throw AssertionError("Web condition did not become true: $expression")
    }

    private fun evaluate(webView: WebView, script: String): String? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = AtomicReference<String?>()
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { webView.evaluateJavascript(script) { result.set(it); latch.countDown() } }
        assertTrue("JavaScript evaluation timed out", latch.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    private class FakeBackupDocuments : BackupDocumentPicker {
        var createUri: Uri? = null
        var openUri: Uri? = null
        var createCount = 0
        var openCount = 0
        override suspend fun createDocument(suggestedFileName: String?): Uri? = createUri.also { createCount += 1 }
        override suspend fun openDocument(): Uri? = openUri.also { openCount += 1 }
    }

    private companion object {
        const val OWNER_PASSWORD = "phase eight backup owner private passphrase 2026"
        const val ADMIN_PASSWORD = "phase eight backup admin private passphrase 2026"
        const val CASHIER_PASSWORD = "phase eight backup cashier private passphrase 2026"
        const val BACKUP_PASSWORD = "portable encrypted backup passphrase 2026"
        const val WRONG_BACKUP_PASSWORD = "incorrect portable backup passphrase 2026"
        const val UI_BACKUP_PASSWORD = "instrumentation portable backup passphrase 2026"
    }
}

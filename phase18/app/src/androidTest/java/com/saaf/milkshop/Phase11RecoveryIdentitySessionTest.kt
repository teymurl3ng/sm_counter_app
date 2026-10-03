package com.saaf.milkshop

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.sync.SyncRebaselineStore
import com.saaf.milkshop.cloud.CloudAccountIdentity
import com.saaf.milkshop.cloud.CloudBackupCoordinator
import com.saaf.milkshop.cloud.CloudBackupCryptography
import com.saaf.milkshop.cloud.CloudBackupFailure
import com.saaf.milkshop.cloud.CloudBackupOutbox
import com.saaf.milkshop.cloud.CloudBackupProvider
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudBackupState
import com.saaf.milkshop.cloud.CloudKeyProtector
import com.saaf.milkshop.cloud.CloudPendingSnapshot
import com.saaf.milkshop.cloud.CloudRemoteSnapshot
import com.saaf.milkshop.cloud.WrappedCiphertext
import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.NativeSession
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import java.io.File
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11RecoveryIdentitySessionTest {
    @Test
    fun preferenceRestore_isCompensatedWhenVerificationFailsOrIsCancelled() = runBlocking {
        val database = MilkShopDatabase.createInMemory()
        try {
            suspend fun verifyCompensation(failure: Throwable) {
                var current = JSONObject().put("mode", "before")
                val applied = mutableListOf<String>()
                val engine = BackupRestoreEngine(
                    createSafetySnapshot = {},
                    buildVerifiedBytes = { _, _, _ -> throw failure },
                    capturePreferences = { JSONObject(current.toString()) },
                    restorePreferences = { replacement ->
                        current = JSONObject(replacement?.toString() ?: "{}")
                        applied += current.optString("mode")
                    },
                    rebaselineStore = SyncRebaselineStore(
                        InstrumentationRegistry.getInstrumentation().targetContext,
                    ),
                )
                val backup = JSONObject().put(
                    "data",
                    JSONObject().put("preferences", JSONObject().put("mode", "replacement")),
                )

                val caught = runCatching {
                    engine.restore(
                        database,
                        backup,
                        BackupManager.RestoreSelection(
                            business = false,
                            planning = false,
                            preferences = true,
                        ),
                        includeSecurity = false,
                    )
                }.exceptionOrNull()

                assertSame(failure, caught)
                assertEquals(listOf("replacement", "before"), applied)
                assertEquals("before", current.getString("mode"))
            }

            verifyCompensation(IllegalStateException("post-restore verification failed"))
            verifyCompensation(CancellationException("restore cancelled"))
        } finally {
            database.close()
        }
    }

    @Test
    fun safetySnapshotFailureOrCancellation_keepsDatabaseAndSessionsUntouched() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val existingBackups = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val sourcePassphrase = "phase eleven source owner 2026".toCharArray()
        val targetPassphrase = "phase eleven target owner 2026".toCharArray()
        var payload: ByteArray? = null
        try {
            UserSecurityService(source).bootstrapOwner("safe.source", "Safe Source", sourcePassphrase)
            payload = BackupManager(context).buildCompleteSnapshotBytes(source)

            val targetSessions = NativeSessionManager()
            val targetSecurity = UserSecurityService(target, targetSessions)
            val targetOwner = targetSecurity.bootstrapOwner("safe.target", "Safe Target", targetPassphrase)
            ProductCatalogService(target).create(ProductCatalogInput("Target Safety Marker", "Kg"))
            val secondSession = targetSessions.start(targetOwner.user.userId)
            val sessions = listOf(targetOwner.session, secondSession)

            val cancellation = CancellationException("cancel safety snapshot")
            val cancelled = try {
                BackupManager(context) { throw cancellation }
                    .restoreCompleteSnapshotReplacing(target, payload, targetSessions, "cancelled.milkbackup")
                null
            } catch (error: Throwable) {
                error
            }
            assertSame(cancellation, cancelled)
            assertTargetSurvivedFailedRestore(target, targetSessions, sessions)

            val failed = BackupManager(context) { error("fail safety snapshot") }
                .restoreCompleteSnapshotReplacing(target, payload, targetSessions, "failed.milkbackup")
            assertTrue(failed.exceptionOrNull() is BackupManager.BackupFailure)
            assertTargetSurvivedFailedRestore(target, targetSessions, sessions)

            val afterBackups = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
            assertEquals(existingBackups, afterBackups)
        } finally {
            payload?.fill(0)
            sourcePassphrase.fill('\u0000')
            targetPassphrase.fill('\u0000')
            source.close()
            target.close()
            backupDir.listFiles()?.filter { it.name !in existingBackups }?.forEach { it.delete() }
        }
    }

    @Test
    fun repeatedReplacementRestore_preservesIdentityActorAndDisablement_andInvalidatesAllSessions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val existingBackups = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val ownerPassphrase = "phase eleven restored owner 2026".toCharArray()
        val cashierPassphrase = "phase eleven restored cashier 2026".toCharArray()
        val oldOwnerPassphrase = "phase eleven replaced owner 2026".toCharArray()
        var payload: ByteArray? = null
        try {
            val sourceSessions = NativeSessionManager()
            val sourceSecurity = UserSecurityService(source, sourceSessions)
            val owner = sourceSecurity.bootstrapOwner("restore.owner", "Restore Owner", ownerPassphrase)
            val pendingCashier = sourceSecurity.beginUserEnrollment(
                owner.session.sessionId,
                CreateUserInput("restore.cashier", "Restore Cashier", "cashier"),
            )
            val cashier = sourceSecurity.completeUserEnrollment(pendingCashier.enrollmentToken, cashierPassphrase)
            val cashierSession = sourceSecurity.login(cashier.loginName, cashierPassphrase)
            val cashId = source.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val count = PhysicalCashCountService(source).record(
                PhysicalCashCountInput(cashId, "2026-09-04", 0.0),
            )
            DailyCloseService(source).closeAuthenticated(
                cashierSession.sessionId,
                sourceSecurity,
                VerifiedDailyCloseInput("2026-09-04", count.count.countId),
            )
            sourceSecurity.setUserEnabled(owner.session.sessionId, cashier.userId, enabled = false)
            ProductCatalogService(source).create(ProductCatalogInput("Recovery Source Product", "Kg"))
            val expectedAuditCount = source.securityAuditDao().all().size
            payload = BackupManager(context).buildCompleteSnapshotBytes(source)

            val targetSessions = NativeSessionManager()
            val targetSecurity = UserSecurityService(target, targetSessions)
            val replacedOwner = targetSecurity.bootstrapOwner("replaced.owner", "Replaced Owner", oldOwnerPassphrase)
            ProductCatalogService(target).create(ProductCatalogInput("Pre-restore Product", "Kg"))
            val firstGenerationSessions = listOf(
                replacedOwner.session,
                targetSessions.start(replacedOwner.user.userId),
            )

            val manager = BackupManager(context)
            val firstRestore = manager.restoreCompleteSnapshotReplacing(
                target,
                payload,
                targetSessions,
                "recovery-generation-1.milkbackup",
            )
            assertTrue(firstRestore.exceptionOrNull()?.message, firstRestore.isSuccess)
            assertSessionsInvalid(targetSessions, firstGenerationSessions)
            assertRestoredIdentityState(target, owner.user.userId, cashier.userId, expectedAuditCount)
            assertNull(target.appUserDao().findByLoginName("replaced.owner"))
            assertNotNull(target.productDao().findByName("Recovery Source Product"))
            assertNull(target.productDao().findByName("Pre-restore Product"))

            val restoredSecurity = UserSecurityService(target, targetSessions)
            val restoredOwnerSession = restoredSecurity.login(owner.user.loginName, ownerPassphrase)
            ProductCatalogService(target).create(ProductCatalogInput("Between Restore Product", "Kg"))
            val secondGenerationSessions = listOf(
                restoredOwnerSession,
                targetSessions.start(owner.user.userId),
            )

            val secondRestore = manager.restoreCompleteSnapshotReplacing(
                target,
                payload,
                targetSessions,
                "recovery-generation-2.milkbackup",
            )
            assertTrue(secondRestore.exceptionOrNull()?.message, secondRestore.isSuccess)
            assertSessionsInvalid(targetSessions, secondGenerationSessions)
            assertRestoredIdentityState(target, owner.user.userId, cashier.userId, expectedAuditCount)
            assertNull(target.productDao().findByName("Between Restore Product"))
            assertTrue(UserSecurityService(target, targetSessions).login(owner.user.loginName, ownerPassphrase).sessionId.isNotBlank())
            val disabledLogin = runCatching {
                UserSecurityService(target, targetSessions).login(cashier.loginName, cashierPassphrase)
            }.exceptionOrNull()
            assertTrue(disabledLogin is SecurityFailure.Authentication)
        } finally {
            payload?.fill(0)
            ownerPassphrase.fill('\u0000')
            cashierPassphrase.fill('\u0000')
            oldOwnerPassphrase.fill('\u0000')
            source.close()
            target.close()
            backupDir.listFiles()?.filter { it.name !in existingBackups }?.forEach { it.delete() }
        }
    }

    @Test
    fun restoreSelection_keepsUnselectedGroupsAndReplacesOnlySelectedGroups() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        val backupFile = File(context.cacheDir, "phase11_restore_selection.milkbackup")
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val existingBackups = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        try {
            ProductCatalogService(source).create(ProductCatalogInput("Selected Source Product", "Kg"))
            source.budgetPlanDao().insert(budget("source-budget", 42.0))
            assertTrue(preferences.edit().putString(PREFERENCE_MARKER, "source").commit())
            val manager = BackupManager(context)
            assertTrue(manager.exportSnapshot(source, Uri.fromFile(backupFile)).isSuccess)

            ProductCatalogService(target).create(ProductCatalogInput("Unselected Target Product", "Kg"))
            target.budgetPlanDao().insert(budget("target-budget", 9.0))
            assertTrue(preferences.edit().putString(PREFERENCE_MARKER, "target").commit())

            val planningOnly = manager.restore(
                target,
                Uri.fromFile(backupFile),
                BackupManager.RestoreSelection(business = false, planning = true, preferences = false),
            )
            assertTrue(planningOnly.exceptionOrNull()?.message, planningOnly.isSuccess)
            assertNotNull(target.productDao().findByName("Unselected Target Product"))
            assertNull(target.productDao().findByName("Selected Source Product"))
            assertEquals(42.0, target.budgetPlanDao().findAllForCategory("source-budget").single().plannedAmount!!, 0.0)
            assertTrue(target.budgetPlanDao().findAllForCategory("target-budget").isEmpty())
            assertEquals("target", preferences.getString(PREFERENCE_MARKER, null))

            val businessAndPreferences = manager.restore(
                target,
                Uri.fromFile(backupFile),
                BackupManager.RestoreSelection(business = true, planning = false, preferences = true),
            )
            assertTrue(businessAndPreferences.exceptionOrNull()?.message, businessAndPreferences.isSuccess)
            assertNull(target.productDao().findByName("Unselected Target Product"))
            assertNotNull(target.productDao().findByName("Selected Source Product"))
            assertEquals(42.0, target.budgetPlanDao().findAllForCategory("source-budget").single().plannedAmount!!, 0.0)
            assertEquals("source", preferences.getString(PREFERENCE_MARKER, null))
        } finally {
            preferences.edit().remove(PREFERENCE_MARKER).commit()
            source.close()
            target.close()
            backupFile.delete()
            backupDir.listFiles()?.filter { it.name !in existingBackups }?.forEach { it.delete() }
        }
    }

    @Test
    fun repeatedWrongCloudPassphrase_keepsRecoveryPointUsableForLaterSuccess() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = "phase11_restore_passphrase_${System.nanoTime()}"
        val settings = CloudBackupSettingsStore(context, suffix)
        val outbox = CloudBackupOutbox(context, "$suffix/outbox")
        val database = MilkShopDatabase.createInMemory()
        val cryptography = CloudBackupCryptography(TestProtector())
        val correct = "phase eleven cloud recovery 2026".toCharArray()
        val wrong = "phase eleven wrong recovery 2026".toCharArray()
        val payload = "recoverable cloud payload".toByteArray()
        var encrypted: ByteArray? = null
        var decrypted: ByteArray? = null
        try {
            val vault = cryptography.createVault(correct)
            val snapshotId = "64bc86cb-9066-4851-b80d-89da1b3d9c64"
            encrypted = cryptography.encryptSnapshot(
                payload,
                vault,
                snapshotId,
                "2026-09-04T14:00:00Z",
            )
            val remote = CloudRemoteSnapshot(
                remoteFileId = "phase11-remote-file",
                snapshotId = snapshotId,
                vaultId = vault.vaultId,
                createdAt = "2026-09-04T14:00:00Z",
                sizeBytes = encrypted.size.toLong(),
                sha256 = null,
                name = "phase11-cloud.milkbackup",
            )
            val provider = RecoveryProvider(remote, encrypted)
            val initialState = CloudBackupState(
                installationId = "70d74ff8-ccf4-4df4-8df4-02f90fb48513",
                configuredAt = "2026-09-04T13:00:00Z",
                enabled = true,
                allowMetered = false,
                accountPermissionId = provider.account.permissionId,
                accountDisplay = "o••@example.com",
                enabledByUserId = 1,
                enabledByName = "Owner",
                enabledByRole = "owner",
                vault = vault,
                snapshots = listOf(remote),
            )
            settings.write(initialState)
            val coordinator = CloudBackupCoordinator(
                database,
                BackupManager(context),
                settings,
                outbox,
                cryptography,
                provider,
            )

            val unknown = runCatching {
                coordinator.downloadAndDecrypt("unknown-remote-file", correct)
            }.exceptionOrNull()
            assertTrue(unknown is CloudBackupFailure.Remote)
            assertEquals(0, provider.downloads)

            repeat(3) {
                val failure = runCatching {
                    coordinator.downloadAndDecrypt(remote.remoteFileId, wrong)
                }.exceptionOrNull()
                assertTrue(failure is CloudBackupFailure.WrongPassphraseOrTampered)
                assertEquals(initialState, settings.read())
            }

            val recovered = coordinator.downloadAndDecrypt(remote.remoteFileId, correct)
            decrypted = recovered.payload
            assertEquals(snapshotId, recovered.snapshotId)
            assertArrayEquals(payload, decrypted)
            assertEquals(4, provider.downloads)
            assertEquals(initialState, settings.read())
        } finally {
            decrypted?.fill(0)
            encrypted?.fill(0)
            payload.fill(0)
            correct.fill('\u0000')
            wrong.fill('\u0000')
            outbox.clearAll()
            settings.clear()
            database.close()
        }
    }

    private suspend fun assertTargetSurvivedFailedRestore(
        database: MilkShopDatabase,
        sessions: NativeSessionManager,
        expectedSessions: List<NativeSession>,
    ) {
        assertNotNull(database.appUserDao().findByLoginName("safe.target"))
        assertNull(database.appUserDao().findByLoginName("safe.source"))
        assertNotNull(database.productDao().findByName("Target Safety Marker"))
        expectedSessions.forEach { assertEquals(it.userId, sessions.require(it.sessionId).userId) }
    }

    private fun assertSessionsInvalid(sessions: NativeSessionManager, expected: List<NativeSession>) {
        expected.forEach { session ->
            assertTrue(runCatching { sessions.require(session.sessionId) }.isFailure)
        }
    }

    private suspend fun assertRestoredIdentityState(
        database: MilkShopDatabase,
        ownerUserId: Long,
        cashierUserId: Long,
        expectedAuditCount: Int,
    ) {
        assertEquals(2L, database.appUserDao().count())
        val cashier = database.appUserDao().findById(cashierUserId)!!
        assertEquals("disabled", cashier.status)
        assertEquals(ownerUserId, cashier.createdByUserId)
        assertEquals(ownerUserId, cashier.disabledByUserId)
        assertNotNull(cashier.disabledAt)
        assertEquals(expectedAuditCount, database.securityAuditDao().all().size)
        val close = DailyCloseService(database).history("2026-09-04").single()
        assertEquals(cashierUserId, close.actorUserId)
        assertEquals("Restore Cashier", close.actorName)
        assertEquals("cashier", close.actorRole)
    }

    private fun budget(category: String, amount: Double) = BudgetPlanEntity(
        category = category,
        periodStart = "2026-09-01",
        periodEndExclusive = "2026-10-01",
        plannedAmount = amount,
        createdAt = "2026-09-04T12:00:00Z",
    )

    private class TestProtector : CloudKeyProtector {
        override fun wrap(plaintext: ByteArray, authenticatedData: ByteArray) = WrappedCiphertext(
            nonceBase64 = Base64.getEncoder().encodeToString(ByteArray(12)),
            ciphertextBase64 = Base64.getEncoder().encodeToString(plaintext),
        )

        override fun unwrap(wrapped: WrappedCiphertext, authenticatedData: ByteArray): ByteArray =
            Base64.getDecoder().decode(wrapped.ciphertextBase64)
    }

    private class RecoveryProvider(
        private val remote: CloudRemoteSnapshot,
        private val bytes: ByteArray,
    ) : CloudBackupProvider {
        val account = CloudAccountIdentity("phase11-owner-permission", "owner@example.com", "Owner")
        var downloads = 0

        override suspend fun accountIdentity() = account

        override suspend fun listSnapshots(vaultId: String?): List<CloudRemoteSnapshot> =
            listOf(remote).filter { vaultId == null || it.vaultId == vaultId }

        override suspend fun findSnapshot(vaultId: String, snapshotId: String): CloudRemoteSnapshot? =
            remote.takeIf { it.vaultId == vaultId && it.snapshotId == snapshotId }

        override suspend fun uploadSnapshot(
            pending: CloudPendingSnapshot,
            vaultId: String,
            installationId: String,
            bytes: ByteArray,
        ): CloudRemoteSnapshot = error("Upload is not part of this recovery test")

        override suspend fun downloadSnapshot(remoteFileId: String): ByteArray {
            assertEquals(remote.remoteFileId, remoteFileId)
            downloads += 1
            return bytes.copyOf()
        }

        override suspend fun deleteSnapshot(remoteFileId: String) = Unit
    }

    private companion object {
        const val PREFERENCES_NAME = "milk_shop_preferences"
        const val PREFERENCE_MARKER = "phase11_restore_selection_marker"
    }
}

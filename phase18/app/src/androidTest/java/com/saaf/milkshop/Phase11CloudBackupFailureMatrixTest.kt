package com.saaf.milkshop

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.NetworkType
import com.saaf.milkshop.cloud.CloudAccountIdentity
import com.saaf.milkshop.cloud.CloudBackupCoordinator
import com.saaf.milkshop.cloud.CloudBackupCryptography
import com.saaf.milkshop.cloud.CloudBackupFailure
import com.saaf.milkshop.cloud.CloudBackupOutbox
import com.saaf.milkshop.cloud.CloudBackupProvider
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudBackupSchedulingPolicy
import com.saaf.milkshop.cloud.CloudBackupState
import com.saaf.milkshop.cloud.CloudBackupWorkerPolicy
import com.saaf.milkshop.cloud.CloudKeyProtector
import com.saaf.milkshop.cloud.CloudPendingSnapshot
import com.saaf.milkshop.cloud.CloudRemoteSnapshot
import com.saaf.milkshop.cloud.CloudVaultConfiguration
import com.saaf.milkshop.cloud.WrappedCiphertext
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11CloudBackupFailureMatrixTest {
    @Test
    fun schedulingPolicy_preservesApprovedCadenceBackoffAndConstraints() {
        assertEquals(24L, CloudBackupSchedulingPolicy.PERIODIC_INTERVAL_HOURS)
        assertEquals(30L, CloudBackupSchedulingPolicy.BACKOFF_MINUTES)
        val wifiDefault = CloudBackupSchedulingPolicy.periodicConstraints(allowMetered = false)
        assertEquals(NetworkType.UNMETERED, wifiDefault.requiredNetworkType)
        assertTrue(wifiDefault.requiresBatteryNotLow())
        assertTrue(wifiDefault.requiresStorageNotLow())
        assertFalse(wifiDefault.requiresCharging())
        assertEquals(
            NetworkType.CONNECTED,
            CloudBackupSchedulingPolicy.periodicConstraints(allowMetered = true).requiredNetworkType,
        )
        assertEquals(NetworkType.CONNECTED, CloudBackupSchedulingPolicy.immediateConstraints().requiredNetworkType)
    }

    @Test
    fun missedSchedule_becomesStaleOnlyAfterFortyEightHours() = runBlocking {
        withFixture { fixture ->
            fixture.now = Instant.parse("2026-09-06T12:00:00Z")
            assertFalse(fixture.coordinator().status().stale)
            fixture.now = fixture.now.plusSeconds(1)
            assertTrue(fixture.coordinator().status().stale)
        }
    }

    @Test
    fun prolongedNetworkFailure_reusesOneSnapshot_andAlertsOnlyAfterTwentyFourHours() = runBlocking {
        withFixture { fixture ->
            fixture.provider.uploadFailure = { CloudBackupFailure.Network() }

            assertTrue(runCatching { fixture.coordinator().runBackup() }.exceptionOrNull() is CloudBackupFailure.Network)
            val firstFailure = fixture.settings.read()!!
            val pendingId = firstFailure.pending!!.snapshotId
            assertEquals(fixture.now.toString(), firstFailure.lastFailureStartedAt)
            assertFalse(CloudBackupWorkerPolicy.shouldNotifyNetwork(firstFailure, fixture.now.plusSeconds(23 * 3600)))

            fixture.now = fixture.now.plusSeconds(23 * 3600)
            assertTrue(runCatching { fixture.coordinator().runBackup() }.exceptionOrNull() is CloudBackupFailure.Network)
            val repeatedFailure = fixture.settings.read()!!
            assertEquals(pendingId, repeatedFailure.pending!!.snapshotId)
            assertEquals(firstFailure.lastFailureStartedAt, repeatedFailure.lastFailureStartedAt)
            assertFalse(CloudBackupWorkerPolicy.shouldNotifyNetwork(repeatedFailure, fixture.now))

            fixture.now = fixture.now.plusSeconds(2 * 3600)
            assertTrue(runCatching { fixture.coordinator().runBackup() }.exceptionOrNull() is CloudBackupFailure.Network)
            assertTrue(CloudBackupWorkerPolicy.shouldNotifyNetwork(fixture.settings.read()!!, fixture.now))

            fixture.provider.uploadFailure = null
            fixture.now = fixture.now.plusSeconds(3600)
            val completed = fixture.coordinator().runBackup()
            assertEquals(pendingId, completed.snapshotId)
            assertEquals(setOf(pendingId), fixture.provider.uploadedSnapshotIds.toSet())
            assertEquals(1, fixture.provider.uploadedHashes.toSet().size)
            assertNull(fixture.settings.read()!!.lastFailureStartedAt)
            assertNull(fixture.settings.read()!!.lastErrorCode)
        }
    }

    @Test
    fun revokedAuthorization_stopsBeforeSnapshot_andRecoversAfterReconnect() = runBlocking {
        withFixture { fixture ->
            fixture.provider.identityFailure = { CloudBackupFailure.AuthorizationRequired() }

            val failure = runCatching { fixture.coordinator().runBackup() }.exceptionOrNull()

            assertTrue(failure is CloudBackupFailure.AuthorizationRequired)
            assertEquals("authorization_required", fixture.settings.read()!!.lastErrorCode)
            assertNull(fixture.settings.read()!!.pending)
            assertEquals(0, fixture.provider.uploadAttempts)

            fixture.provider.identityFailure = null
            assertTrue(fixture.coordinator().runBackup().uploaded)
        }
    }

    @Test
    fun wrongAccount_stopsBeforeSnapshot_andPreservesConfiguredVault() = runBlocking {
        withFixture { fixture ->
            val vaultId = fixture.vault.vaultId
            fixture.provider.activeAccount = CloudAccountIdentity("different-account", "other@example.com", "Other")

            val failure = runCatching { fixture.coordinator().runBackup() }.exceptionOrNull()

            assertTrue(failure is CloudBackupFailure.AccountMismatch)
            assertEquals("account_mismatch", fixture.settings.read()!!.lastErrorCode)
            assertEquals(vaultId, fixture.settings.read()!!.vault.vaultId)
            assertNull(fixture.settings.read()!!.pending)
            assertEquals(0, fixture.provider.uploadAttempts)
        }
    }

    @Test
    fun quotaFailure_keepsPendingAndEveryExistingRemote_thenManualRetryUsesSameBytes() = runBlocking {
        withFixture { fixture ->
            val originalRemoteIds = fixture.provider.seedHistory(fixture.vault.vaultId, 2).map { it.remoteFileId }.toSet()
            fixture.provider.uploadFailure = { CloudBackupFailure.Quota() }

            val failure = runCatching { fixture.coordinator().runBackup() }.exceptionOrNull()
            val pending = fixture.settings.read()!!.pending

            assertTrue(failure is CloudBackupFailure.Quota)
            assertNotNull(pending)
            assertEquals("quota", fixture.settings.read()!!.lastErrorCode)
            assertTrue(fixture.provider.remoteIds().containsAll(originalRemoteIds))
            assertTrue(fixture.provider.deleteAttempts.isEmpty())

            fixture.provider.uploadFailure = null
            val completed = fixture.coordinator().runBackup()
            assertEquals(pending!!.snapshotId, completed.snapshotId)
            assertEquals(setOf(pending.snapshotId), fixture.provider.uploadedSnapshotIds.toSet())
            assertEquals(1, fixture.provider.uploadedHashes.toSet().size)
            assertTrue(fixture.provider.remoteIds().containsAll(originalRemoteIds))
        }
    }

    @Test
    fun unavailableLocalKey_neverCreatesOutbox_andWorkingKeyCanResume() = runBlocking {
        withFixture { fixture ->
            val unavailable = CloudBackupCryptography(UnavailableProtector())

            val failure = runCatching {
                fixture.coordinator(cryptography = unavailable).runBackup()
            }.exceptionOrNull()

            assertTrue(failure is CloudBackupFailure.KeyUnavailable)
            assertEquals("key_unavailable", fixture.settings.read()!!.lastErrorCode)
            assertNull(fixture.settings.read()!!.pending)
            assertEquals(0, fixture.provider.uploadAttempts)
            assertTrue(fixture.coordinator().runBackup().uploaded)
        }
    }

    @Test
    fun cancellationDuringSnapshotRead_leavesNoPartialOutboxOrFailureAudit() = runBlocking {
        withFixture { fixture ->
            val snapshotStarted = CompletableDeferred<Unit>()
            val cancellingManager = BackupManager(fixture.context) {
                snapshotStarted.complete(Unit)
                awaitCancellation()
            }
            val job = async(Dispatchers.IO) { fixture.coordinator(backupManager = cancellingManager).runBackup() }
            snapshotStarted.await()

            job.cancelAndJoin()

            val state = fixture.settings.read()!!
            assertNull(state.pending)
            assertNull(state.lastErrorCode)
            assertTrue(fixture.database.securityAuditDao().all().none { it.action == "scheduled_cloud_backup" })
            assertTrue(fixture.coordinator().runBackup().uploaded)
        }
    }

    @Test
    fun cancellationDuringUpload_keepsPendingIdentityForExactRetry() = runBlocking {
        withFixture { fixture ->
            fixture.provider.cancelUpload = true
            val job = async(Dispatchers.IO) { fixture.coordinator().runBackup() }
            fixture.provider.uploadStarted.await()

            job.cancelAndJoin()

            val pending = fixture.settings.read()!!.pending
            assertNotNull(pending)
            assertNull(fixture.settings.read()!!.lastErrorCode)
            assertNotNull(fixture.outbox.readOrNull(pending!!))

            fixture.provider.cancelUpload = false
            val completed = fixture.coordinator().runBackup()
            assertEquals(pending.snapshotId, completed.snapshotId)
            assertEquals(setOf(pending.snapshotId), fixture.provider.uploadedSnapshotIds.toSet())
            assertEquals(1, fixture.provider.uploadedHashes.toSet().size)
        }
    }

    @Test
    fun cancellationDuringDownloadVerification_keepsRemoteAndPendingForRetry() = runBlocking {
        withFixture { fixture ->
            fixture.provider.exposeUploadedSha256 = false
            fixture.provider.cancelDownload = true
            val job = async(Dispatchers.IO) { fixture.coordinator().runBackup() }
            fixture.provider.downloadStarted.await()

            job.cancelAndJoin()

            val pending = fixture.settings.read()!!.pending
            assertNotNull(pending)
            assertEquals(1, fixture.provider.uploadAttempts)
            assertEquals(1, fixture.provider.remoteIds().size)

            fixture.provider.cancelDownload = false
            val completed = fixture.coordinator().runBackup()
            assertEquals(pending!!.snapshotId, completed.snapshotId)
            assertFalse(completed.uploaded)
            assertEquals(1, fixture.provider.uploadAttempts)
        }
    }

    @Test
    fun retentionFailures_areWarnings_butRetentionCancellationRemainsRetryable() = runBlocking {
        withFixture { fixture ->
            fixture.provider.seedHistory(fixture.vault.vaultId, 5)
            fixture.provider.deleteFailure = { CloudBackupFailure.Network("retention unavailable") }

            val completedWithWarnings = fixture.coordinator().runBackup()

            assertEquals(3, completedWithWarnings.retentionWarnings)
            assertNull(fixture.settings.read()!!.pending)
            assertNull(fixture.settings.read()!!.lastErrorCode)
            assertTrue(fixture.settings.read()!!.lastErrorMessage!!.contains("3 old recovery point"))
            assertEquals(6, fixture.provider.remoteIds().size)

            fixture.provider.deleteFailure = { kotlinx.coroutines.CancellationException("worker stopped") }
            val cancelled = runCatching { fixture.coordinator().runBackup() }.exceptionOrNull()
            val pendingAfterCancellation = fixture.settings.read()!!.pending

            assertTrue(cancelled is kotlinx.coroutines.CancellationException)
            assertNotNull(pendingAfterCancellation)
            fixture.provider.deleteFailure = null
            val retried = fixture.coordinator().runBackup()
            assertEquals(pendingAfterCancellation!!.snapshotId, retried.snapshotId)
            assertFalse(retried.uploaded)
        }
    }

    private suspend fun withFixture(block: suspend (Fixture) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = "phase11_failure_${System.nanoTime()}"
        val passphrase = "phase eleven failure matrix 2026".toCharArray()
        val cryptography = CloudBackupCryptography(TestProtector())
        val vault = cryptography.createVault(passphrase)
        val settings = CloudBackupSettingsStore(context, suffix)
        val outbox = CloudBackupOutbox(context, "$suffix/outbox")
        val database = MilkShopDatabase.createInMemory()
        val provider = MatrixProvider()
        settings.write(
            CloudBackupState(
                installationId = "214ffb72-2590-47b8-a055-399baae5df35",
                configuredAt = "2026-09-04T12:00:00Z",
                enabled = true,
                allowMetered = false,
                accountPermissionId = provider.configuredAccount.permissionId,
                accountDisplay = "o••@example.com",
                enabledByUserId = 1,
                enabledByName = "Owner",
                enabledByRole = "owner",
                vault = vault,
            ),
        )
        val fixture = Fixture(context, database, settings, outbox, cryptography, vault, provider)
        try {
            block(fixture)
        } finally {
            passphrase.fill('\u0000')
            outbox.clearAll()
            settings.clear()
            database.close()
        }
    }

    private data class Fixture(
        val context: Context,
        val database: MilkShopDatabase,
        val settings: CloudBackupSettingsStore,
        val outbox: CloudBackupOutbox,
        val cryptography: CloudBackupCryptography,
        val vault: CloudVaultConfiguration,
        val provider: MatrixProvider,
        var now: Instant = Instant.parse("2026-09-04T14:00:00Z"),
    ) {
        fun coordinator(
            backupManager: BackupManager = BackupManager(context),
            cryptography: CloudBackupCryptography = this.cryptography,
        ) = CloudBackupCoordinator(
            database = database,
            backupManager = backupManager,
            settings = settings,
            outbox = outbox,
            cryptography = cryptography,
            provider = provider,
            now = { now },
        )
    }

    private class TestProtector : CloudKeyProtector {
        override fun wrap(plaintext: ByteArray, authenticatedData: ByteArray) = WrappedCiphertext(
            nonceBase64 = Base64.getEncoder().encodeToString(ByteArray(12)),
            ciphertextBase64 = Base64.getEncoder().encodeToString(plaintext),
        )

        override fun unwrap(wrapped: WrappedCiphertext, authenticatedData: ByteArray): ByteArray =
            Base64.getDecoder().decode(wrapped.ciphertextBase64)
    }

    private class UnavailableProtector : CloudKeyProtector {
        override fun wrap(plaintext: ByteArray, authenticatedData: ByteArray): WrappedCiphertext =
            throw CloudBackupFailure.KeyUnavailable("Android Keystore key is unavailable")

        override fun unwrap(wrapped: WrappedCiphertext, authenticatedData: ByteArray): ByteArray =
            throw CloudBackupFailure.KeyUnavailable("Android Keystore key is unavailable")
    }

    private class MatrixProvider : CloudBackupProvider {
        val configuredAccount = CloudAccountIdentity("phase11-owner", "owner@example.com", "Owner")
        var activeAccount = configuredAccount
        var identityFailure: (() -> Throwable)? = null
        var uploadFailure: (() -> Throwable)? = null
        var deleteFailure: (() -> Throwable)? = null
        var cancelUpload = false
        var cancelDownload = false
        var exposeUploadedSha256 = true
        val uploadStarted = CompletableDeferred<Unit>()
        val downloadStarted = CompletableDeferred<Unit>()
        var uploadAttempts = 0
            private set
        var downloadAttempts = 0
            private set
        val uploadedSnapshotIds = mutableListOf<String>()
        val uploadedHashes = mutableListOf<String>()
        val deleteAttempts = mutableListOf<String>()
        private val snapshots = linkedMapOf<String, Pair<CloudRemoteSnapshot, ByteArray>>()

        override suspend fun accountIdentity(): CloudAccountIdentity {
            identityFailure?.let { throw it() }
            return activeAccount
        }

        override suspend fun listSnapshots(vaultId: String?): List<CloudRemoteSnapshot> =
            snapshots.values.map { it.first }.filter { vaultId == null || it.vaultId == vaultId }

        override suspend fun findSnapshot(vaultId: String, snapshotId: String): CloudRemoteSnapshot? =
            snapshots[snapshotId]?.first?.takeIf { it.vaultId == vaultId }

        override suspend fun uploadSnapshot(
            pending: CloudPendingSnapshot,
            vaultId: String,
            installationId: String,
            bytes: ByteArray,
        ): CloudRemoteSnapshot {
            uploadAttempts += 1
            uploadedSnapshotIds += pending.snapshotId
            uploadedHashes += pending.sha256
            uploadStarted.complete(Unit)
            if (cancelUpload) awaitCancellation()
            uploadFailure?.let { throw it() }
            return seed(pending, vaultId, bytes, exposeUploadedSha256)
        }

        override suspend fun downloadSnapshot(remoteFileId: String): ByteArray {
            downloadAttempts += 1
            downloadStarted.complete(Unit)
            if (cancelDownload) awaitCancellation()
            return snapshots.values.single { it.first.remoteFileId == remoteFileId }.second.copyOf()
        }

        override suspend fun deleteSnapshot(remoteFileId: String) {
            deleteAttempts += remoteFileId
            deleteFailure?.let { throw it() }
            val snapshotId = snapshots.entries.single { it.value.first.remoteFileId == remoteFileId }.key
            snapshots.remove(snapshotId)
        }

        fun remoteIds(): Set<String> = snapshots.values.mapTo(mutableSetOf()) { it.first.remoteFileId }

        fun seedHistory(vaultId: String, count: Int): List<CloudRemoteSnapshot> = (0 until count).map { index ->
            val sequence = index + 1
            val snapshotId = "00000000-0000-4000-8000-${sequence.toString().padStart(12, '0')}"
            CloudRemoteSnapshot(
                remoteFileId = "remote-history-${sequence.toString().padStart(3, '0')}",
                snapshotId = snapshotId,
                vaultId = vaultId,
                createdAt = Instant.parse("2026-09-01T12:00:00Z").minusSeconds(index * 3600L).toString(),
                sizeBytes = 1,
                sha256 = "00",
                name = "history-$sequence.milkbackup",
            ).also { remote -> snapshots[snapshotId] = remote to byteArrayOf(0) }
        }

        private fun seed(
            pending: CloudPendingSnapshot,
            vaultId: String,
            bytes: ByteArray,
            exposeSha256: Boolean,
        ): CloudRemoteSnapshot = CloudRemoteSnapshot(
            remoteFileId = "remote-${pending.snapshotId}",
            snapshotId = pending.snapshotId,
            vaultId = vaultId,
            createdAt = pending.createdAt,
            sizeBytes = pending.sizeBytes,
            sha256 = pending.sha256.takeIf { exposeSha256 },
            name = "saaf-milk-${pending.snapshotId}.milkbackup",
        ).also { remote -> snapshots[pending.snapshotId] = remote to bytes.copyOf() }
    }
}

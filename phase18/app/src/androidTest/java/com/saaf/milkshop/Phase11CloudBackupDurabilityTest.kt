package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import com.saaf.milkshop.cloud.CloudVaultConfiguration
import com.saaf.milkshop.cloud.WrappedCiphertext
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase11CloudBackupDurabilityTest {
    @Test
    fun finalizedOutbox_withoutPendingState_isAuthenticatedAndResumed() = runBlocking {
        withFixture { fixture ->
            val snapshotId = "c949c5bf-5e32-4ccc-8eb3-1e97b72eb389"
            val createdAt = "2026-09-04T13:00:00Z"
            val pending = fixture.writeFinalizedSnapshot(snapshotId, createdAt)

            val result = fixture.coordinator().runBackup()

            assertEquals(snapshotId, result.snapshotId)
            assertTrue(result.uploaded)
            assertEquals(1, fixture.provider.uploadAttempts)
            assertNull(fixture.settings.read()!!.pending)
            assertNull(fixture.outbox.readOrNull(pending))
        }
    }

    @Test
    fun pendingState_withoutOutbox_completesFromVerifiedRemoteCopy() = runBlocking {
        withFixture { fixture ->
            val snapshotId = "b0d39029-daf6-4be7-9c43-b61c7d5ebdac"
            val createdAt = "2026-09-04T13:10:00Z"
            val encrypted = fixture.encryptedSnapshot(snapshotId, createdAt)
            val pending = fixture.outbox.create(snapshotId, createdAt, encrypted)
            fixture.outbox.remove(pending)
            fixture.provider.seed(pending, fixture.vault.vaultId, encrypted, exposeSha256 = false)
            fixture.settings.update { it.copy(pending = pending) }
            encrypted.fill(0)

            val result = fixture.coordinator().runBackup()

            assertEquals(snapshotId, result.snapshotId)
            assertFalse(result.uploaded)
            assertEquals(0, fixture.provider.uploadAttempts)
            assertEquals(1, fixture.provider.downloadAttempts)
            assertNull(fixture.settings.read()!!.pending)
        }
    }

    @Test
    fun recoveredOutbox_withAuthenticatedCiphertextDamage_isNeverUploaded() = runBlocking {
        withFixture { fixture ->
            val snapshotId = "e6462a47-553b-466e-aefe-77b37ef750cc"
            val createdAt = "2026-09-04T13:20:00Z"
            val encrypted = fixture.encryptedSnapshot(snapshotId, createdAt)
            val envelope = JSONObject(encrypted.toString(Charsets.UTF_8))
            val cipher = envelope.getJSONObject("cipher")
            val ciphertext = Base64.getDecoder().decode(cipher.getString("ciphertext"))
            ciphertext[ciphertext.lastIndex / 2] = (ciphertext[ciphertext.lastIndex / 2].toInt() xor 1).toByte()
            cipher.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
            fixture.outbox.create(snapshotId, createdAt, envelope.toString().toByteArray())

            val failure = runCatching { fixture.coordinator().runBackup() }.exceptionOrNull()

            assertTrue(failure is CloudBackupFailure.KeyUnavailable)
            assertEquals(0, fixture.provider.uploadAttempts)
            assertEquals("key_unavailable", fixture.settings.read()!!.lastErrorCode)
            encrypted.fill(0)
            ciphertext.fill(0)
        }
    }

    @Test
    fun overlappingRuns_areSerializedAndShareOneCompletedSnapshot() = runBlocking {
        withFixture(gateUpload = true) { fixture ->
            val firstCoordinator = fixture.coordinator()
            val secondCoordinator = fixture.coordinator()
            val first = async(Dispatchers.IO) { firstCoordinator.runBackup() }
            fixture.provider.uploadStarted.await()
            val second = async(Dispatchers.IO) { secondCoordinator.runBackup() }
            delay(100)
            fixture.provider.releaseUpload.complete(Unit)

            val firstResult = first.await()
            val secondResult = second.await()

            assertEquals(firstResult.snapshotId, secondResult.snapshotId)
            assertTrue(firstResult.uploaded)
            assertFalse(secondResult.uploaded)
            assertEquals(1, fixture.provider.uploadAttempts)
        }
    }

    private suspend fun withFixture(
        gateUpload: Boolean = false,
        block: suspend (Fixture) -> Unit,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = "phase11_cloud_${System.nanoTime()}"
        val passphrase = "phase eleven recovery phrase 2026".toCharArray()
        val cryptography = CloudBackupCryptography(TestProtector())
        val vault = cryptography.createVault(passphrase)
        val settings = CloudBackupSettingsStore(context, suffix)
        val outbox = CloudBackupOutbox(context, "$suffix/outbox")
        val database = MilkShopDatabase.createInMemory()
        val provider = RecordingProvider(gateUpload)
        settings.write(state(vault, provider.account))
        val fixture = Fixture(
            database = database,
            backupManager = BackupManager(context),
            settings = settings,
            outbox = outbox,
            cryptography = cryptography,
            vault = vault,
            provider = provider,
        )
        try {
            block(fixture)
        } finally {
            passphrase.fill('\u0000')
            outbox.clearAll()
            settings.clear()
            database.close()
        }
    }

    private fun state(vault: CloudVaultConfiguration, account: CloudAccountIdentity) = CloudBackupState(
        installationId = "ef75b8f0-cdbc-477d-9578-ce319c0c452f",
        configuredAt = "2026-09-04T12:00:00Z",
        enabled = true,
        allowMetered = false,
        accountPermissionId = account.permissionId,
        accountDisplay = "o••@example.com",
        enabledByUserId = 1,
        enabledByName = "Owner",
        enabledByRole = "owner",
        vault = vault,
    )

    private data class Fixture(
        val database: MilkShopDatabase,
        val backupManager: BackupManager,
        val settings: CloudBackupSettingsStore,
        val outbox: CloudBackupOutbox,
        val cryptography: CloudBackupCryptography,
        val vault: CloudVaultConfiguration,
        val provider: RecordingProvider,
    ) {
        fun coordinator() = CloudBackupCoordinator(
            database = database,
            backupManager = backupManager,
            settings = settings,
            outbox = outbox,
            cryptography = cryptography,
            provider = provider,
            now = { Instant.parse("2026-09-04T14:00:00Z") },
        )

        fun encryptedSnapshot(snapshotId: String, createdAt: String): ByteArray =
            cryptography.encryptSnapshot(
                payload = "phase-11-complete-snapshot".toByteArray(),
                configuration = vault,
                snapshotId = snapshotId,
                createdAt = createdAt,
            )

        fun writeFinalizedSnapshot(snapshotId: String, createdAt: String): CloudPendingSnapshot {
            val encrypted = encryptedSnapshot(snapshotId, createdAt)
            return try {
                outbox.create(snapshotId, createdAt, encrypted)
            } finally {
                encrypted.fill(0)
            }
        }
    }

    private class TestProtector : CloudKeyProtector {
        override fun wrap(plaintext: ByteArray, authenticatedData: ByteArray) = WrappedCiphertext(
            nonceBase64 = Base64.getEncoder().encodeToString(ByteArray(12)),
            ciphertextBase64 = Base64.getEncoder().encodeToString(plaintext),
        )

        override fun unwrap(wrapped: WrappedCiphertext, authenticatedData: ByteArray): ByteArray =
            Base64.getDecoder().decode(wrapped.ciphertextBase64)
    }

    private class RecordingProvider(
        private val gateUpload: Boolean,
    ) : CloudBackupProvider {
        val account = CloudAccountIdentity("phase11-owner-permission", "owner@example.com", "Owner")
        val uploadStarted = CompletableDeferred<Unit>()
        val releaseUpload = CompletableDeferred<Unit>()
        var uploadAttempts = 0
            private set
        var downloadAttempts = 0
            private set
        private val snapshots = linkedMapOf<String, Pair<CloudRemoteSnapshot, ByteArray>>()

        override suspend fun accountIdentity() = account

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
            uploadStarted.complete(Unit)
            if (gateUpload) releaseUpload.await()
            return seed(pending, vaultId, bytes)
        }

        fun seed(
            pending: CloudPendingSnapshot,
            vaultId: String,
            bytes: ByteArray,
            exposeSha256: Boolean = true,
        ): CloudRemoteSnapshot = CloudRemoteSnapshot(
            remoteFileId = "remote-${pending.snapshotId}",
            snapshotId = pending.snapshotId,
            vaultId = vaultId,
            createdAt = pending.createdAt,
            sizeBytes = pending.sizeBytes,
            sha256 = pending.sha256.takeIf { exposeSha256 },
            name = "saaf-milk-${pending.snapshotId}.milkcloud",
        ).also { remote -> snapshots[pending.snapshotId] = remote to bytes.copyOf() }

        override suspend fun downloadSnapshot(remoteFileId: String): ByteArray {
            downloadAttempts += 1
            return snapshots.values.single { it.first.remoteFileId == remoteFileId }.second.copyOf()
        }

        override suspend fun deleteSnapshot(remoteFileId: String) {
            snapshots.entries.removeAll { it.value.first.remoteFileId == remoteFileId }
        }
    }
}

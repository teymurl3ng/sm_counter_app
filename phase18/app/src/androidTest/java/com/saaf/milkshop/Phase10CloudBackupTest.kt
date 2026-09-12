package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.saaf.milkshop.cloud.CloudBackupCryptography
import com.saaf.milkshop.cloud.CloudAccountIdentity
import com.saaf.milkshop.cloud.CloudBackupCoordinator
import com.saaf.milkshop.cloud.CloudBackupFailure
import com.saaf.milkshop.cloud.CloudBackupOutbox
import com.saaf.milkshop.cloud.CloudBackupProvider
import com.saaf.milkshop.cloud.CloudBackupRetention
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudBackupState
import com.saaf.milkshop.cloud.CloudBackupScheduler
import com.saaf.milkshop.cloud.CloudKeyProtector
import com.saaf.milkshop.cloud.CloudRemoteSnapshot
import com.saaf.milkshop.cloud.WrappedCiphertext
import com.saaf.milkshop.data.MilkShopDatabase
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase10CloudBackupTest {
    @Test
    fun cloudEnvelope_isSelfContained_andRejectsWrongPassphraseAndTampering() {
        val passphrase = "cloud recovery phrase 2026".toCharArray()
        val payload = "complete native backup payload".toByteArray()
        val creator = CloudBackupCryptography(TestProtector())
        val vault = creator.createVault(passphrase)
        val first = creator.encryptSnapshot(
            payload = payload,
            configuration = vault,
            snapshotId = "55d6b2b8-81ef-4e1f-aedc-e68f9c88fd86",
            createdAt = "2026-09-04T10:15:30Z",
        )
        val second = creator.encryptSnapshot(
            payload = payload,
            configuration = vault,
            snapshotId = "5b43e1c7-ced1-4f09-ab18-83f6dca047de",
            createdAt = "2026-09-04T10:16:30Z",
        )

        val replacementDevice = CloudBackupCryptography(FailingProtector())
        val decrypted = replacementDevice.decryptSnapshot(first, passphrase)
        val locallyDecrypted = creator.decryptSnapshotWithLocalKey(first, vault)
        assertArrayEquals(payload, decrypted.payload)
        assertArrayEquals(payload, locallyDecrypted.payload)
        assertNotEquals(
            JSONObject(first.toString(Charsets.UTF_8)).getJSONObject("cipher").getString("nonce"),
            JSONObject(second.toString(Charsets.UTF_8)).getJSONObject("cipher").getString("nonce"),
        )

        val wrong = "different recovery phrase 2026".toCharArray()
        assertThrows(CloudBackupFailure.WrongPassphraseOrTampered::class.java) {
            replacementDevice.decryptSnapshot(first, wrong)
        }

        val altered = JSONObject(first.toString(Charsets.UTF_8))
        val cipher = altered.getJSONObject("cipher")
        val ciphertext = Base64.getDecoder().decode(cipher.getString("ciphertext"))
        ciphertext[ciphertext.lastIndex / 2] = (ciphertext[ciphertext.lastIndex / 2].toInt() xor 1).toByte()
        cipher.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
        assertThrows(CloudBackupFailure.WrongPassphraseOrTampered::class.java) {
            replacementDevice.decryptSnapshot(altered.toString().toByteArray(), passphrase)
        }

        passphrase.fill('\u0000')
        wrong.fill('\u0000')
        decrypted.payload.fill(0)
        locallyDecrypted.payload.fill(0)
    }

    @Test
    fun differentLocalVault_requiresPortableRecoveryPassphrase() {
        val sourcePassphrase = "source cloud recovery phrase 2026".toCharArray()
        val replacementPassphrase = "replacement cloud recovery phrase 2026".toCharArray()
        val payload = "portable replacement-phone payload".toByteArray()
        val sourceCryptography = CloudBackupCryptography(TestProtector())
        val sourceVault = sourceCryptography.createVault(sourcePassphrase)
        val encrypted = sourceCryptography.encryptSnapshot(
            payload = payload,
            configuration = sourceVault,
            snapshotId = "84545e81-4938-4092-93de-fafb95cc879f",
            createdAt = "2026-09-12T19:05:30Z",
        )
        val replacementCryptography = CloudBackupCryptography(TestProtector())
        val replacementVault = replacementCryptography.createVault(replacementPassphrase)

        assertNotEquals(sourceVault.vaultId, replacementVault.vaultId)
        assertThrows(CloudBackupFailure.KeyUnavailable::class.java) {
            replacementCryptography.decryptSnapshotWithLocalKey(encrypted, replacementVault)
        }
        val recovered = replacementCryptography.decryptSnapshot(encrypted, sourcePassphrase)
        assertArrayEquals(payload, recovered.payload)

        sourcePassphrase.fill('\u0000')
        replacementPassphrase.fill('\u0000')
        recovered.payload.fill(0)
    }

    @Test
    fun outbox_reusesExactBytes_andRetentionNeverDropsThreeNewest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outbox = CloudBackupOutbox(context)
        outbox.clearAll()
        val bytes = ByteArray(4096) { index -> (index % 251).toByte() }
        val pending = outbox.create(
            snapshotId = "866c2c68-c21a-4f86-9303-58dcedbf70c6",
            createdAt = "2026-09-04T11:00:00Z",
            bytes = bytes,
        )
        try {
            assertArrayEquals(bytes, outbox.read(pending))
        } finally {
            outbox.remove(pending)
        }

        val sameDay = (0 until 5).map { index ->
            CloudRemoteSnapshot(
                remoteFileId = "remote-file-id-${10 + index}",
                snapshotId = "00000000-0000-4000-8000-0000000000${10 + index}",
                vaultId = "00000000-0000-4000-8000-000000000001",
                createdAt = "2026-09-04T${14 - index}:00:00Z",
                sizeBytes = 100,
                sha256 = null,
                name = "backup-$index",
            )
        }
        val removals = CloudBackupRetention.removals(sameDay)
        assertEquals(2, removals.size)
        assertTrue(sameDay.take(3).none { newest -> removals.any { it.remoteFileId == newest.remoteFileId } })
    }

    @Test
    fun completePayload_holdsOneRoomSnapshot_whenWriteIsAttempted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = MilkShopDatabase.createInMemory()
        val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val setting = database.settingsDao().getAll().first()
            val original = setting.value
            val replacement = original + 123.0
            var writer = writerScope.async { 0 }
            val manager = BackupManager(context) {
                writer = writerScope.async { database.settingsDao().updateValue(setting.key, replacement) }
                assertNull(withTimeoutOrNull(100) { writer.await() })
            }
            val bytes = manager.buildCompleteSnapshotBytes(database)
            assertEquals(1, writer.await())
            val serialized = JSONObject(bytes.toString(Charsets.UTF_8))
                .getJSONObject("data")
                .getJSONArray("settings")
            val backedUp = (0 until serialized.length())
                .map { serialized.getJSONObject(it) }
                .single { it.getString("setting_key") == setting.key }
                .getDouble("setting_value")
            assertEquals(original, backedUp, 0.0)
            assertEquals(replacement, database.settingsDao().getValue(setting.key)!!, 0.0)
        } finally {
            writerScope.cancel()
            database.close()
        }
    }

    @Test
    fun uncertainUpload_retryFindsRemoteSnapshot_andReusesPendingIdentity() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = "cloud_backup_test_${System.nanoTime()}"
        val settings = CloudBackupSettingsStore(context, suffix)
        val outbox = CloudBackupOutbox(context, "$suffix/outbox")
        val database = MilkShopDatabase.createInMemory()
        val passphrase = "retry recovery phrase 2026".toCharArray()
        val cryptography = CloudBackupCryptography(TestProtector())
        val vault = cryptography.createVault(passphrase)
        val provider = UncertainUploadProvider()
        settings.write(
            CloudBackupState(
                installationId = "f8dc461a-f12f-451e-8b99-c38561b1bbed",
                configuredAt = "2026-09-04T12:00:00Z",
                enabled = true,
                allowMetered = false,
                accountPermissionId = provider.account.permissionId,
                accountDisplay = "o••@example.com",
                enabledByUserId = 1,
                enabledByName = "Owner",
                enabledByRole = "owner",
                vault = vault,
            ),
        )
        val coordinator = CloudBackupCoordinator(
            database = database,
            backupManager = BackupManager(context),
            settings = settings,
            outbox = outbox,
            cryptography = cryptography,
            provider = provider,
            now = { java.time.Instant.parse("2026-09-04T12:30:00Z") },
        )
        try {
            val firstFailure = runCatching { coordinator.runBackup() }.exceptionOrNull()
            assertTrue(firstFailure is CloudBackupFailure.Network)
            val pendingId = settings.read()!!.pending!!.snapshotId
            val completed = coordinator.runBackup()
            assertEquals(pendingId, completed.snapshotId)
            assertEquals(1, provider.uploadAttempts)
            assertNull(settings.read()!!.pending)
        } finally {
            passphrase.fill('\u0000')
            outbox.clearAll()
            settings.clear()
            database.close()
        }
    }

    @Test
    fun schedulerUpdatesOneUniquePeriodicRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduler = CloudBackupScheduler(context)
        try {
            scheduler.apply(enabled = true, allowMetered = false)
            scheduler.apply(enabled = true, allowMetered = true)
            val work = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(CloudBackupScheduler.PERIODIC_WORK_NAME)
                .get(10, TimeUnit.SECONDS)
            assertEquals(1, work.size)
            assertTrue(work.single().tags.contains(CloudBackupScheduler.WORK_TAG))
        } finally {
            scheduler.cancelAll()
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

    private class FailingProtector : CloudKeyProtector {
        override fun wrap(plaintext: ByteArray, authenticatedData: ByteArray): WrappedCiphertext =
            error("Replacement-device recovery must not use a local key")

        override fun unwrap(wrapped: WrappedCiphertext, authenticatedData: ByteArray): ByteArray =
            error("Replacement-device recovery must not use a local key")
    }

    private class UncertainUploadProvider : CloudBackupProvider {
        val account = CloudAccountIdentity("drive-permission-owner", "owner@example.com", "Owner")
        var uploadAttempts = 0
        private var remote: CloudRemoteSnapshot? = null
        private var remoteBytes: ByteArray? = null

        override suspend fun accountIdentity() = account

        override suspend fun listSnapshots(vaultId: String?): List<CloudRemoteSnapshot> =
            listOfNotNull(remote).filter { vaultId == null || it.vaultId == vaultId }

        override suspend fun findSnapshot(vaultId: String, snapshotId: String): CloudRemoteSnapshot? =
            remote?.takeIf { it.vaultId == vaultId && it.snapshotId == snapshotId }

        override suspend fun uploadSnapshot(
            pending: com.saaf.milkshop.cloud.CloudPendingSnapshot,
            vaultId: String,
            installationId: String,
            bytes: ByteArray,
        ): CloudRemoteSnapshot {
            uploadAttempts += 1
            remoteBytes = bytes.copyOf()
            remote = CloudRemoteSnapshot(
                remoteFileId = "remote-file-id-uncertain",
                snapshotId = pending.snapshotId,
                vaultId = vaultId,
                createdAt = pending.createdAt,
                sizeBytes = pending.sizeBytes,
                sha256 = null,
                name = "saaf-milk-test.milkbackup",
            )
            throw CloudBackupFailure.Network("The response was lost after upload")
        }

        override suspend fun downloadSnapshot(remoteFileId: String): ByteArray =
            remoteBytes?.copyOf() ?: error("No uploaded bytes")

        override suspend fun deleteSnapshot(remoteFileId: String) {
            remote = null
            remoteBytes = null
        }
    }
}

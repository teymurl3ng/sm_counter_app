package com.saaf.milkshop

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.owner.FakeNativeOwnerCloudBackupGateway
import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.application.owner.NativeOwnerBackupFailure
import com.saaf.milkshop.cloud.CloudBackupCryptography
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudKeyProtector
import com.saaf.milkshop.cloud.WrappedCiphertext
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.UserSecurityService
import java.io.File
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerBackupFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerBackupFacade
    private lateinit var settings: CloudBackupSettingsStore
    private lateinit var gateway: FakeNativeOwnerCloudBackupGateway
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String
    private lateinit var backupFile: File

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        settings = CloudBackupSettingsStore(
            context,
            directoryName = "owner-backup-facade-${UUID.randomUUID()}",
        )
        gateway = FakeNativeOwnerCloudBackupGateway(
            settings = settings,
            cryptography = CloudBackupCryptography(TestProtector()),
        )
        facade = NativeOwnerBackupFacade(
            database = database,
            security = security,
            cloudBackupSettings = settings,
            backupManager = BackupManager(context),
            cloudGateway = gateway,
        )
        ownerSession = security.bootstrapOwner(
            "owner.backup",
            "Backup Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "backup.cashier",
                displayName = "Backup Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("backup.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
        backupFile = File(context.cacheDir, "owner-backup-${UUID.randomUUID()}.milkbackup")
    }

    @After
    fun tearDown() {
        facade.close()
        database.close()
        settings.clear()
        if (::backupFile.isInitialized && backupFile.exists()) backupFile.delete()
    }

    @Test
    fun loadWithNullCloudSettingsReportsNotConfigured() = runBlocking {
        val empty = NativeOwnerBackupFacade(
            database = database,
            security = security,
            cloudBackupSettings = null,
            backupManager = BackupManager(InstrumentationRegistry.getInstrumentation().targetContext),
        )
        try {
            val snapshot = empty.load(ownerSession)
            assertFalse(snapshot.configured)
            assertFalse(snapshot.enabled)
            assertEquals("Backup not configured", snapshot.statusHeadline)
            assertTrue(snapshot.scheduleDescription.contains("not configured"))
            assertFalse(snapshot.cloudActionsAvailable)
        } finally {
            empty.close()
        }
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.load(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun exportInspectRestoreRoundTrip() = runBlocking {
        val passphrase = BACKUP_PASSPHRASE.toCharArray()
        val uri = Uri.fromFile(backupFile)
        try {
            val exported = facade.exportEncrypted(ownerSession, uri, passphrase.copyOf())
            assertTrue(exported.displayName.isNotBlank())
            assertEquals(BackupManager.FORMAT_VERSION, exported.formatVersion)
            assertTrue(backupFile.exists() && backupFile.length() > 0)

            val inspection = facade.inspectEncrypted(ownerSession, uri, passphrase.copyOf())
            assertTrue(inspection.containsSecurityState)
            assertEquals(BackupManager.FORMAT_VERSION, inspection.formatVersion)

            val result = facade.confirmRestore(
                ownerSession,
                inspection.restoreTicket,
                confirmRestore = true,
            )
            assertTrue(result.restored)
            assertTrue(result.sessionsInvalidated)
            assertTrue(runCatching { security.sessionManager().require(ownerSession) }.isFailure)
        } finally {
            passphrase.fill('\u0000')
        }
    }

    @Test
    fun inspectRejectsWrongPassphrase() = runBlocking {
        val good = BACKUP_PASSPHRASE.toCharArray()
        val bad = "definitely wrong passphrase!!".toCharArray()
        try {
            facade.exportEncrypted(ownerSession, Uri.fromFile(backupFile), good.copyOf())
            val rejected = runCatching {
                facade.inspectEncrypted(ownerSession, Uri.fromFile(backupFile), bad.copyOf())
            }.exceptionOrNull()
            assertTrue(rejected is NativeOwnerBackupFailure)
            assertEquals("backup_wrong_passphrase", (rejected as NativeOwnerBackupFailure).code)
        } finally {
            good.fill('\u0000')
            bad.fill('\u0000')
        }
    }

    @Test
    fun cloudConnectEnableRunNowDisconnectWithFakeGateway() = runBlocking {
        val recovery = BACKUP_PASSPHRASE.toCharArray()
        try {
            val connected = facade.connectDrive(ownerSession, recovery.copyOf(), allowMetered = false)
            assertTrue(connected.configured)
            assertTrue(connected.enabled)
            assertEquals("owner@example.com", connected.accountDisplay)
            assertTrue(connected.cloudActionsAvailable)

            val paused = facade.setDrivePolicy(ownerSession, enabled = false, allowMetered = false)
            assertTrue(paused.configured)
            assertFalse(paused.enabled)

            val enabled = facade.setDrivePolicy(ownerSession, enabled = true, allowMetered = true)
            assertTrue(enabled.enabled)
            assertTrue(enabled.allowMetered)

            val ran = facade.runDriveBackupNow(ownerSession)
            assertTrue(ran.lastSuccessAt != null)

            val disconnected = facade.disconnectDrive(ownerSession, confirmRevoke = true)
            assertFalse(disconnected.configured)
            assertEquals("Backup not configured", disconnected.statusHeadline)
        } finally {
            recovery.fill('\u0000')
        }
    }

    @Test
    fun cloudRefreshInspectRestoreWithFakeGateway() = runBlocking {
        val recovery = BACKUP_PASSPHRASE.toCharArray()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = BackupManager(context)
        try {
            facade.connectDrive(ownerSession, recovery.copyOf(), allowMetered = false)
            gateway.remotePayloadProvider = {
                kotlinx.coroutines.runBlocking { manager.buildCompleteSnapshotBytes(database) }
            }
            val vaultId = settings.read()!!.vault.vaultId
            gateway.seedRemoteSnapshot(
                remoteFileId = "fake-remote-restore",
                vaultId = vaultId,
                sizeBytes = 1L,
            )
            val refreshed = facade.refreshCloudSnapshots(ownerSession)
            assertEquals(1, refreshed.remoteSnapshots.size)
            assertEquals("fake-remote-restore", refreshed.remoteSnapshots.first().remoteFileId)

            val inspection = facade.inspectCloudBackup(
                ownerSession,
                "fake-remote-restore",
                recoveryPassphrase = null,
            )
            assertTrue(inspection.containsSecurityState)
            assertTrue(inspection.restoreTicket.isNotBlank())

            val result = facade.confirmCloudRestore(
                ownerSession,
                inspection.restoreTicket,
                confirmRestore = true,
            )
            assertTrue(result.restored)
            assertTrue(result.sessionsInvalidated)
            assertFalse(settings.read()!!.enabled)
        } finally {
            recovery.fill('\u0000')
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

    companion object {
        private const val OWNER_PASSPHRASE = "owner backup passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "backup cashier passphrase 2026!!"
        private const val BACKUP_PASSPHRASE = "portable backup phrase 2026!!"
    }
}

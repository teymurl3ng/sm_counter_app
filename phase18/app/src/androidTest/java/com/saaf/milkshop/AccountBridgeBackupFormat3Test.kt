package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.AccountInstallationBindingEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ServerIdentityLinkEntity
import com.saaf.milkshop.data.ServerShopSnapshotEntity
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


/**
 * Account Bridge backup format 3 (ACCOUNT_BRIDGE_PHASE1_SPEC.md §8):
 * the encrypted payload carries the installation binding, local-user ↔
 * server-staff links, and shop snapshots; restore preserves the stable
 * server IDs but forces every binding/link to restored_unverified
 * regardless of its backed-up value.
 */
@RunWith(AndroidJUnit4::class)
class AccountBridgeBackupFormat3Test {
    @Test
    fun format3_roundTrip_preservesStableIds_andForcesRestoredUnverified() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val restored = MilkShopDatabase.createInMemory()
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val existing = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val ownerPassphrase = "format three owner phrase 2026".toCharArray()
        try {
            val sourceSecurity = UserSecurityService(source)
            val owner = sourceSecurity.bootstrapOwner("bridge.owner", "Bridge Owner", ownerPassphrase)
            val verifiedAt = 1_790_000_000_000L
            source.serverAccountDao().insertBinding(
                AccountInstallationBindingEntity(
                    environment = "dev",
                    businessId = "aaaaaaa1-0000-4000-8000-000000000001",
                    shopId = "aaaaaaa2-0000-4000-8000-000000000002",
                    shopNameSnapshot = "Bridge Shop",
                    status = "active",
                    verifiedAtEpochMs = verifiedAt,
                    contractVersion = "v5",
                    createdAt = "2026-09-25T00:00:00Z",
                    updatedAt = "2026-09-25T00:00:00Z",
                )
            )
            source.serverAccountDao().insertIdentityLink(
                ServerIdentityLinkEntity(
                    localUserId = owner.user.userId,
                    serverStaffId = "bbbbbbb1-0000-4000-8000-000000000003",
                    businessId = "aaaaaaa1-0000-4000-8000-000000000001",
                    usernameSnapshot = "bridge.owner",
                    roleSnapshot = "owner",
                    status = "verified",
                    verifiedAtEpochMs = verifiedAt,
                )
            )
            source.serverAccountDao().insertServerShopSnapshots(
                listOf(
                    ServerShopSnapshotEntity(
                        serverShopId = "aaaaaaa2-0000-4000-8000-000000000002",
                        shopNameSnapshot = "Bridge Shop",
                        observedAtEpochMs = verifiedAt,
                        localUserId = owner.user.userId,
                    )
                )
            )

            val manager = BackupManager(context)
            val meta = manager.createLocalSnapshot(source).getOrThrow()
            assertEquals(BackupManager.FORMAT_VERSION, meta.formatVersion)
            assertEquals(BackupManager.CURRENT_SCHEMA_VERSION, meta.schemaVersion)
            val snapshot = File(backupDir, meta.fileName)

            val restore = manager.restoreLocalSnapshot(
                restored,
                android.net.Uri.fromFile(snapshot),
                BackupManager.RestoreSelection(true, true, false, security = true),
                NativeSessionManager(),
            )
            assertTrue(restore.exceptionOrNull()?.message, restore.isSuccess)

            val binding = restored.serverAccountDao().findBinding()!!
            assertEquals("aaaaaaa1-0000-4000-8000-000000000001", binding.businessId)
            assertEquals("aaaaaaa2-0000-4000-8000-000000000002", binding.shopId)
            assertEquals("restored_unverified", binding.status)

            val links = restored.serverAccountDao().identityLinks()
            assertEquals(1, links.size)
            assertEquals(owner.user.userId, links.single().localUserId)
            assertEquals("bbbbbbb1-0000-4000-8000-000000000003", links.single().serverStaffId)
            assertEquals("restored_unverified", links.single().status)

            val snapshots = restored.serverAccountDao().serverShopSnapshots()
            assertEquals(1, snapshots.size)
            assertEquals("aaaaaaa2-0000-4000-8000-000000000002", snapshots.single().serverShopId)
        } finally {
            ownerPassphrase.fill('\u0000')
            source.close()
            restored.close()
            backupDir.listFiles()?.filter { it.name !in existing }?.forEach { it.delete() }
        }
    }
}

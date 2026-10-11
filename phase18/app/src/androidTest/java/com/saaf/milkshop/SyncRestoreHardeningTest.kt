package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.AccountInstallationBindingEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.SyncScheduler
import com.saaf.milkshop.sync.SyncServerConfig
import com.saaf.milkshop.sync.SyncServerConfigStore
import com.saaf.milkshop.sync.SyncShopBinding
import com.saaf.milkshop.sync.SyncShopBindingStore
import com.saaf.milkshop.sync.SyncTokenStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Phase 8: restore hardening. Two rules protect a synced installation:
 *  - a security-bearing backup from a DIFFERENT shop than the device's
 *    enrollment is refused (spec §8.6 conflict, never silently merged);
 *  - every successful restore deletes persisted remote tokens (spec §8.2),
 *    so the device re-verifies before its next pull or push — the shop
 *    binding and config survive outside the backup.
 */
@RunWith(AndroidJUnit4::class)
class SyncRestoreHardeningTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
    private val existingBackups = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
    private val passphrase = "phase eight hardening phrase".toCharArray()
    private val shopA = "aaaaaaa2-0000-4000-8000-000000000002"
    private val shopB = "aaaaaaa2-0000-4000-8000-000000000099"

    @After
    fun tearDown() {
        passphrase.fill('\u0000')
        backupDir.listFiles()?.filter { it.name !in existingBackups }?.forEach { it.delete() }
        SyncShopBindingStore.clear(context)
        SyncServerConfigStore.clear(context)
        SyncTokenStore(context).clear()
        com.saaf.milkshop.sync.SyncRebaselineStore(context).clear()
        SyncScheduler.warmUp(context)
    }

    private suspend fun sourceDatabaseWithShop(shopId: String): Pair<MilkShopDatabase, Long> {
        val source = MilkShopDatabase.createInMemory()
        val owner = UserSecurityService(source).bootstrapOwner(
            "harden.owner",
            "Harden Owner",
            "harden owner passphrase 123".toCharArray(),
        )
        source.serverAccountDao().insertBinding(
            AccountInstallationBindingEntity(
                environment = "dev",
                businessId = "aaaaaaa1-0000-4000-8000-000000000001",
                shopId = shopId,
                shopNameSnapshot = "Hardened Shop",
                status = "active",
                verifiedAtEpochMs = 1_790_000_000_000L,
                contractVersion = "v5",
                createdAt = "2026-09-29T00:00:00Z",
                updatedAt = "2026-09-29T00:00:00Z",
            ),
        )
        return source to owner.user.userId
    }

    private suspend fun exportLocalSnapshot(source: MilkShopDatabase): File {
        val manager = BackupManager(context)
        val meta = manager.createLocalSnapshot(source).getOrThrow()
        return File(backupDir, meta.fileName)
    }

    private fun enrollLocalDevice(shopId: String) {
        SyncShopBindingStore.save(
            context,
            SyncShopBinding(
                environment = SyncServerConfig.ENVIRONMENT_DEV,
                businessId = "aaaaaaa1-0000-4000-8000-000000000001",
                shopId = shopId,
                shopName = "Enrolled Shop",
                staffId = "ccccccc1-0000-4000-8000-000000000004",
                role = "owner",
            ),
        )
        SyncServerConfigStore.save(
            context,
            SyncServerConfig(SyncServerConfig.ENVIRONMENT_DEV, "https://saafmilk-dev.example", "sb_publishable_test"),
        )
        SyncTokenStore(context).save(SyncTokenStore.Session("stale-token", "stale-refresh", null))
    }

    @Test
    fun restoreFromDifferentShop_isRefused_withEnrollmentIntact() = runBlocking {
        val (source, _) = sourceDatabaseWithShop(shopB)
        enrollLocalDevice(shopA) // device is enrolled for shop A; backup is shop B
        val snapshot = exportLocalSnapshot(source)
        val restored = MilkShopDatabase.createInMemory()
        try {
            restored.syncRebaselineHoldDao().upsert(com.saaf.milkshop.data.SyncRebaselineHoldEntity(
                "products", "old-review-item", "{}", "old-device", 42, "2026-10-10"))
            val restore = BackupManager(context).restoreLocalSnapshot(
                restored,
                android.net.Uri.fromFile(snapshot),
                BackupManager.RestoreSelection(true, true, false, security = true),
                NativeSessionManager(),
            )
            assertTrue(restore.exceptionOrNull()?.message, restore.isFailure)
            assertTrue(
                restore.exceptionOrNull()?.message ?: "",
                restore.exceptionOrNull()!!.message!!.contains("different shop"),
            )
            // Nothing was restored onto the target and the enrollment is untouched.
            assertEquals(0L, restored.appUserDao().count())
            assertEquals(1, restored.syncRebaselineHoldDao().count())
            assertEquals(shopA, SyncShopBindingStore.load(context)?.shopId)
            assertNotNull(SyncTokenStore(context).load())
        } finally {
            source.close()
            restored.close()
        }
    }

    @Test
    fun restoreFromSameShop_succeeds_clearsTokens_keepsBindingAndConfig() = runBlocking {
        val (source, _) = sourceDatabaseWithShop(shopA)
        enrollLocalDevice(shopA) // same shop: restore is legitimate
        val snapshot = exportLocalSnapshot(source)
        val restored = MilkShopDatabase.createInMemory()
        try {
            restored.syncRebaselineHoldDao().upsert(com.saaf.milkshop.data.SyncRebaselineHoldEntity(
                "products", "old-review-item", "{}", "old-device", 42, "2026-10-10"))
            val restore = BackupManager(context).restoreLocalSnapshot(
                restored,
                android.net.Uri.fromFile(snapshot),
                BackupManager.RestoreSelection(true, true, false, security = true),
                NativeSessionManager(),
            )
            assertTrue(restore.exceptionOrNull()?.message, restore.isSuccess)

            // Tokens are gone (spec §8.2) — the device re-verifies before
            // its next pull or push.
            assertNull(SyncTokenStore(context).load())
            // The shop binding and config survive outside the backup.
            assertEquals(shopA, SyncShopBindingStore.load(context)?.shopId)
            assertNotNull(SyncServerConfigStore.load(context))
            // The restored Room data carries the shop binding forced to
            // restored_unverified and an empty outbox with the snapshot's
            // pull cursor.
            val binding = restored.serverAccountDao().findBinding()!!
            assertEquals(shopA, binding.shopId)
            assertEquals("restored_unverified", binding.status)
            assertEquals(0, restored.syncRebaselineHoldDao().count())
            assertEquals(source.syncOutboxDao().count(), restored.syncOutboxDao().count())
            assertTrue(restored.syncOutboxDao().all().all { it.held })
        } finally {
            source.close()
            restored.close()
        }
    }

    @Test
    fun restoreWithNoLocalEnrollment_alwaysSucceeds() = runBlocking {
        val (source, _) = sourceDatabaseWithShop(shopB) // no local enrollment at all
        val snapshot = exportLocalSnapshot(source)
        val restored = MilkShopDatabase.createInMemory()
        try {
            val restore = BackupManager(context).restoreLocalSnapshot(
                restored,
                android.net.Uri.fromFile(snapshot),
                BackupManager.RestoreSelection(true, true, false, security = true),
                NativeSessionManager(),
            )
            assertTrue(restore.exceptionOrNull()?.message, restore.isSuccess)
            assertEquals(source.syncOutboxDao().count(), restored.syncOutboxDao().count())
            assertTrue(restored.syncOutboxDao().all().all { it.held })
        } finally {
            source.close()
            restored.close()
        }
    }
}

package com.saaf.milkshop

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncPullStateEntity
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.sync.SyncRebaselineStore
import com.saaf.milkshop.sync.SyncBaseline
import com.saaf.milkshop.sync.SyncRebaseline
import com.saaf.milkshop.sync.SyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SyncPlanningRestoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
    private val existingBackups = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()

    @After fun cleanup() {
        backupDir.listFiles()?.filter { it.name !in existingBackups }?.forEach { it.delete() }
        SyncRebaselineStore(context).clear()
        SyncScheduler.warmUp(context)
    }

    @Test fun planningOnlyRestoreDoesNotLeaveStaleOrBlindPushablePlanningOutbox() = runBlocking {
        SyncScheduler.suppressSchedulingForTest()
        SyncRebaselineStore(context).clear()
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            source.budgetPlanDao().insert(BudgetPlanEntity(
                category = "Milk purchases", periodStart = "2030-01-07", periodEndExclusive = "2030-01-14",
                plannedLiters = 100.0, plannedAmount = 18000.0, createdAt = "2029-12-01T00:00:00Z",
                uid = "backup-budget",
            ))
            val manager = BackupManager(context)
            val snapshot = manager.createLocalSnapshot(source).getOrThrow()
            target.syncOutboxDao().deleteAll()
            target.budgetPlanDao().insert(BudgetPlanEntity(
                category = "Milk purchases", periodStart = "2030-01-07", periodEndExclusive = "2030-01-14",
                plannedLiters = 120.0, plannedAmount = 22000.0, createdAt = "2029-12-02T00:00:00Z",
                uid = "replaced-local-budget",
            ))
            target.accountDao().insert(AccountEntity(
                name = "Customer: Unsent", type = "Asset", category = "Customer", uid = "unsent-customer",
            ))
            target.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 77, updatedAt = "2029-12-01T00:00:00Z"))
            val restored = manager.restoreLocalSnapshot(
                target, Uri.fromFile(File(backupDir, snapshot.fileName)),
                BackupManager.RestoreSelection(business = false, planning = true, preferences = false),
                NativeSessionManager(),
            )
            assertTrue(restored.exceptionOrNull()?.message, restored.isSuccess)
            assertEquals("backup-budget", target.budgetPlanDao().findActive("Milk purchases", "2030-01-07")?.uid)
            assertEquals(0, target.syncOutboxDao().countByTableAndUid("budget_plans", "replaced-local-budget"))
            assertEquals(0, target.syncOutboxDao().countByTableAndUid("budget_plans", "backup-budget"))
            assertEquals(1, target.syncOutboxDao().countByTableAndUid("accounts", "unsent-customer"))
            assertEquals(77L, target.syncPullStateDao().find()!!.lastServerSeq)
            assertTrue(SyncRebaselineStore(context).needsFreshIdentity)
            assertFalse(SyncRebaselineStore(context).restoredRebaseline)
            SyncBaseline.enqueueAllHeld(target) // The next plain enrollment's queue step.
            val queued = target.syncOutboxDao().all()
            assertTrue(queued.single { it.tableName == "budget_plans" && it.rowUid == "backup-budget" }.held)
            assertFalse(queued.single { it.tableName == "accounts" && it.rowUid == "unsent-customer" }.held)
            val serverPlan = budgetJson(target.budgetPlanDao()
                .findActive("Milk purchases", "2030-01-07")!!).put("planned_amount", 25000.0)
            val classified = SyncRebaseline.classifyLiveAndApply(target, mapOf(
                SyncRebaseline.seenKey("budget_plans", "backup-budget") to
                    SyncRebaseline.SeenServerRow("other-device", 99, serverPlan),
            ))
            assertEquals(1, classified.heldDiffer)
            assertTrue(target.syncOutboxDao().all().single {
                it.tableName == "budget_plans" && it.rowUid == "backup-budget"
            }.held)
            assertTrue(target.syncRebaselineHoldDao().all().any {
                it.tableName == "budget_plans" && it.rowUid == "backup-budget"
            })
            assertFalse(target.syncOutboxDao().all().single {
                it.tableName == "accounts" && it.rowUid == "unsent-customer"
            }.held)
        } finally {
            source.close()
            target.close()
        }
    }
}

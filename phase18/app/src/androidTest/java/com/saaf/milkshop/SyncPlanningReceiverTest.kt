package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.CollectionsTargetEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.SyncApplyEngine
import com.saaf.milkshop.sync.SyncEnvelopeFactory
import com.saaf.milkshop.sync.SyncPulledRecord
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncPlanningReceiverTest {
    @Test
    fun revisionsResolveByUidAfterLateParentAndReplayWithoutDuplicates() = runBlocking {
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            // Shift target IDs so copied source integer links would be wrong.
            target.budgetPlanDao().insert(BudgetPlanEntity(
                category = "Milk purchases", periodStart = "2029-01-01", periodEndExclusive = "2029-01-08",
                plannedLiters = 1.0, plannedAmount = 1.0, createdAt = "2028-01-01T00:00:00Z", uid = "local-budget",
            ))
            target.collectionsTargetDao().insert(CollectionsTargetEntity(
                weekStart = "2029-01-01", weekEndExclusive = "2029-01-08", targetAmount = 1.0,
                createdAt = "2028-01-01T00:00:00Z", uid = "local-target",
            ))
            val budgetId = source.budgetPlanDao().insert(BudgetPlanEntity(
                category = "Milk purchases", periodStart = "2030-01-07", periodEndExclusive = "2030-01-14",
                plannedLiters = 100.0, plannedAmount = 18000.0, vendorRateSnapshot = 180.0,
                retailRateSnapshot = 200.0, createdAt = "2029-12-01T00:00:00Z", uid = "budget-parent",
            ))
            source.budgetPlanDao().archive(budgetId)
            source.budgetPlanDao().insert(BudgetPlanEntity(
                category = "Milk purchases", periodStart = "2030-01-07", periodEndExclusive = "2030-01-14",
                plannedLiters = 120.0, plannedAmount = 22200.0, vendorRateSnapshot = 185.0,
                retailRateSnapshot = 205.0, createdAt = "2029-12-02T00:00:00Z",
                supersedesId = budgetId, uid = "budget-child",
            ))
            val targetId = source.collectionsTargetDao().insert(CollectionsTargetEntity(
                weekStart = "2030-01-07", weekEndExclusive = "2030-01-14", targetAmount = 25000.0,
                createdAt = "2029-12-01T00:00:00Z", uid = "target-parent",
            ))
            source.collectionsTargetDao().archive(targetId)
            source.collectionsTargetDao().insert(CollectionsTargetEntity(
                weekStart = "2030-01-07", weekEndExclusive = "2030-01-14", targetAmount = 27000.0,
                createdAt = "2029-12-02T00:00:00Z", supersedesId = targetId, uid = "target-child",
            ))
            val envelopes = SyncEnvelopeFactory(source).build(source.syncOutboxDao().nextBatch(100)).envelopes
                .filter { it.getString("table") in setOf("budget_plans", "collections_targets") }
            assertEquals(4, envelopes.size)
            val records = envelopes.mapIndexed { index, envelope ->
                SyncPulledRecord((index + 1).toLong(), null, envelope.getString("table"),
                    envelope.getString("uid"), envelope.getJSONObject("payload"), "source")
            }
            val children = records.filter { it.rowUid.endsWith("child") }
            val parents = records.filter { it.rowUid.endsWith("parent") }
            val engine = SyncApplyEngine(target)
            val early = engine.apply(children)
            assertEquals(2, early.parked)
            assertEquals(2, target.syncParkedRowDao().count())
            val afterParents = engine.apply(parents)
            assertEquals(0, afterParents.skipped)
            assertEquals(0, target.syncParkedRowDao().count())

            val budgetParent = target.syncEnvelopeDao().budgetsByUid(listOf("budget-parent")).single()
            val budgetChild = target.syncEnvelopeDao().budgetsByUid(listOf("budget-child")).single()
            val targetParent = target.syncEnvelopeDao().targetsByUid(listOf("target-parent")).single()
            val targetChild = target.syncEnvelopeDao().targetsByUid(listOf("target-child")).single()
            assertTrue(budgetParent.archived)
            assertTrue(targetParent.archived)
            assertEquals(budgetParent.budgetId, budgetChild.supersedesId)
            assertEquals(targetParent.targetId, targetChild.supersedesId)
            assertEquals(22200.0, budgetChild.plannedAmount!!, 0.0)
            assertEquals(185.0, budgetChild.vendorRateSnapshot!!, 0.0)
            assertEquals(27000.0, targetChild.targetAmount, 0.0)
            assertEquals("budget-child", target.budgetPlanDao().findActive("Milk purchases", "2030-01-07")?.uid)
            assertEquals("target-child", target.collectionsTargetDao().findActive("2030-01-07")?.uid)

            val replay = engine.apply(records)
            assertEquals(0, replay.applied)
            assertEquals(0, replay.updated)
            assertEquals(3, target.budgetPlanDao().findAllForCategory("Milk purchases").size)
            assertEquals(3, target.collectionsTargetDao().findAll().size)
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun invalidAndSiblingRevisionsDoNotChangeActivePlanning() = runBlocking {
        val database = MilkShopDatabase.createInMemory()
        try {
            val engine = SyncApplyEngine(database)
            fun record(seq: Long, uid: String, amount: Double, parent: String? = null) = SyncPulledRecord(
                seq, null, "collections_targets", uid,
                JSONObject().put("week_start", "2030-01-07").put("week_end_exclusive", "2030-01-14")
                    .put("target_amount", amount).put("created_at", "2029-12-01T00:00:00Z")
                    .put("archived", uid == "parent").put("supersedes_id", if (parent == null) JSONObject.NULL else 9999)
                    .apply { parent?.let { put("supersedes_uid", it) } }, "source",
            )
            assertEquals(1, engine.apply(listOf(record(1, "parent", 25000.0))).applied)
            assertEquals(1, engine.apply(listOf(record(2, "winner", 27000.0, "parent"))).applied)
            assertEquals(1, engine.apply(listOf(record(3, "sibling", 29000.0, "parent"))).conflicts)
            assertEquals(1, engine.apply(listOf(record(5, "other-root", 31000.0))).conflicts)
            assertNull(database.syncEnvelopeDao().targetsByUid(listOf("sibling")).firstOrNull())
            assertNull(database.syncEnvelopeDao().targetsByUid(listOf("other-root")).firstOrNull())
            assertEquals("winner", database.collectionsTargetDao().findActive("2030-01-07")?.uid)
            assertEquals(1, engine.apply(listOf(record(4, "negative", -1.0))).skipped)
            assertNull(database.syncEnvelopeDao().targetsByUid(listOf("negative")).firstOrNull())
            assertNotNull(database.backupDataDao().securityAuditEntries().firstOrNull {
                it.reasonCode == "sibling_planning_revision"
            })
        } finally {
            database.close()
        }
    }

    @Test
    fun childWaitsForArchivedParentAndMissingUidLinkIsRejected() = runBlocking {
        val database = MilkShopDatabase.createInMemory()
        try {
            fun budget(seq: Long, uid: String, archived: Boolean, parent: String? = null) = SyncPulledRecord(
                seq, null, "budget_plans", uid,
                JSONObject().put("category", "Milk purchases").put("period_start", "2030-01-07")
                    .put("period_end_exclusive", "2030-01-14").put("planned_liters", 100.0)
                    .put("planned_amount", 18000.0).put("vendor_rate_snapshot", 180.0)
                    .put("retail_rate_snapshot", 200.0).put("created_at", "2029-12-01T00:00:00Z")
                    .put("archived", archived)
                    .put("supersedes_id", if (parent == null) JSONObject.NULL else 9999)
                    .apply { parent?.let { put("supersedes_uid", it) } }, "source",
            )
            val engine = SyncApplyEngine(database)
            assertEquals(1, engine.apply(listOf(budget(1, "parent", false))).applied)
            assertEquals(1, engine.apply(listOf(budget(2, "child", false, "parent"))).parked)
            assertNull(database.syncEnvelopeDao().budgetsByUid(listOf("child")).firstOrNull())
            assertEquals(1, engine.apply(listOf(budget(3, "parent", true))).updated)
            assertNotNull(database.syncEnvelopeDao().budgetsByUid(listOf("child")).firstOrNull())
            val broken = budget(4, "broken", false, "parent").payload.apply { remove("supersedes_uid") }
            assertEquals(1, engine.apply(listOf(SyncPulledRecord(
                4, null, "budget_plans", "broken", broken, "source",
            ))).skipped)
            assertNull(database.syncEnvelopeDao().budgetsByUid(listOf("broken")).firstOrNull())
        } finally {
            database.close()
        }
    }
}

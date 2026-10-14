package com.saaf.milkshop

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncSeedCompatibilityTest {
    @Test fun actualFactoryAndMigrationProfilesRoundTripThroughCatchup() = runBlocking {
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            // Execute the shipped backfill against all five real profile families.
            for ((name, type, category) in listOf(Triple("Customer: Seed test", "Asset", "Customer"),
                Triple("Vendor: Seed test", "Liability", "Vendor"), Triple("Salary: Seed test", "Expense", "Salary"))) {
                source.accountDao().insert(AccountEntity(name = name, type = type, category = category))
            }
            source.withWriteTransaction { Phase3MigrationSql.backfillStatements.forEach { executeSQL(it) } }
            val envelopes = SyncEnvelopeFactory(source).build(source.syncOutboxDao().all()).envelopes
            val records = envelopes.mapIndexed { index, e -> SyncPulledRecord(index + 1L, null,
                e.getString("table"), e.getString("uid"), e.getJSONObject("payload"), "source") }
            assertEquals(22, records.size)
            val profiles = records.filter { it.payload.has("profile") }
            assertEquals(13, profiles.size)
            profiles.forEach { assertEquals("2026-01-01 00:00:00", it.payload.getJSONObject("profile").getString("created_at")) }
            target.syncOutboxDao().deleteAll() // Empty receiver fixture, not an enrollment workaround.
            target.syncPullStateDao().upsert(SyncPullStateEntity(lastServerSeq = 77, updatedAt = "2026-10-14T00:00:00Z"))
            assertEquals(SyncProfileCatchup.Outcome.Done(1, 13, 0), SyncProfileCatchup(target).replay {
                SyncPullOutcome.Fetched(SyncPullBatch(records, records.last().serverSeq), false)
            })
            assertEquals(77L, target.syncPullStateDao().find()!!.lastServerSeq)
            SyncBaseline.enqueueAll(target)
            val received = SyncEnvelopeFactory(target).build(target.syncOutboxDao().all()).envelopes
                .associateBy { it.getString("uid") }
            profiles.forEach { r -> assertTrue(r.rowUid,
                SyncRebaseline.livePayloadsEqual(r.payload, received.getValue(r.rowUid).getJSONObject("payload"))) }
        } finally { source.close(); target.close() }
    }

    @Test fun invalidSqliteCalendarAndMalformedDatesRollbackParentAndProfile() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val milk = db.productDao().findByName("Milk")!!
            val before = db.productProfileDao().findByProductId(milk.productId)!!
            val envelope = SyncEnvelopeFactory(db).build(db.syncOutboxDao().all()).envelopes.single { it.getString("uid") == milk.uid }
            db.syncOutboxDao().deleteAll()
            for ((i, date) in listOf("2026-02-30 00:00:00", "2026-01-01 25:00:00", "2026-1-1 00:00:00", "invalid", "2026-01-01").withIndex()) {
                val payload = JSONObject(envelope.getJSONObject("payload").toString()).put("name", "Must roll back")
                payload.getJSONObject("profile").put("created_at", date)
                assertEquals(date, 1, SyncApplyEngine(db).apply(listOf(SyncPulledRecord(i + 1L, null, "products", milk.uid, payload, "source"))).skipped)
                assertEquals("Milk", db.productDao().findByName("Milk")!!.name)
                assertEquals(before, db.productProfileDao().findByProductId(milk.productId))
            }
        } finally { db.close() }
    }
}

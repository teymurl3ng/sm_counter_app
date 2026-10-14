package com.saaf.milkshop

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncEventDetailsTest {
    private val date = "2026-10-14"
    private val stamp = "2026-10-14T00:00:00Z"

    @Test fun zeroActorFailsForeignKeyWithoutCreatingAnIdentity() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val id = db.eventDao().insert(sale("u1_fk_actor"))
            val failure = runCatching { db.eventActorLinkDao().insert(EventActorLinkEntity(id, 0L, "Remote", "owner", stamp)) }.exceptionOrNull()
            assertNotNull(failure)
            assertTrue(failure.toString(), failure.toString().contains("FOREIGN KEY", ignoreCase = true))
            assertNull(db.eventActorLinkDao().find(id))
            assertTrue(db.appUserDao().all().isEmpty())
        } finally { db.close() }
    }

    @Test fun remoteActorSnapshotIsUnlinkedAndReplayRepairsOlderHeader() = runBlocking {
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            val user = source.appUserDao().insert(AppUserEntity(loginName = "source.actor", displayName = "Source Owner",
                role = "owner", createdAt = stamp, updatedAt = stamp))
            target.appUserDao().insert(AppUserEntity(loginName = "different.actor", displayName = "Different local person",
                role = "cashier", createdAt = stamp, updatedAt = stamp))
            val id = source.eventDao().insert(sale("u1_actor_snapshot"))
            source.eventActorLinkDao().insert(EventActorLinkEntity(id, user, "Source Owner", "owner", stamp))
            val envelope = SyncEnvelopeFactory(source).build(source.syncOutboxDao().all()).envelopes.single { it.getString("uid") == "u1_actor_snapshot" }
            target.syncOutboxDao().deleteAll()
            val payload = JSONObject(envelope.getJSONObject("payload").toString())
            val old = JSONObject(payload.toString()).apply { remove("actor_link") }
            assertEquals(1, SyncApplyEngine(target).apply(listOf(record(envelope, old))).applied)
            val remoteId = target.syncEnvelopeDao().eventsByUid(listOf("u1_actor_snapshot")).single().eventId
            val headers = target.backupDataDao().events()
            assertNull(target.eventActorLinkDao().find(remoteId))
            assertEquals(0, SyncApplyEngine(target).apply(listOf(record(envelope, payload))).skipped)
            val snapshot = target.eventActorLinkDao().find(remoteId)
            assertNotNull("Remote actor snapshot must be retained", snapshot)
            assertEquals(null, snapshot!!.actorUserId)
            assertEquals("Source Owner", snapshot.actorNameSnapshot)
            assertEquals("owner", snapshot.actorRoleSnapshot)
            assertEquals(stamp, snapshot.attributedAt)
            SyncApplyEngine(target).apply(listOf(record(envelope, payload)))
            assertEquals(headers, target.backupDataDao().events())
            assertEquals(listOf(snapshot), target.backupDataDao().eventActorLinks())
            assertEquals(listOf("different.actor"), target.appUserDao().all().map { it.loginName })
            val freshUid = "u1_actor_fresh_remote"
            val freshPayload = JSONObject(payload.toString()).put("event_uid", freshUid)
            assertEquals(1, SyncApplyEngine(target).apply(listOf(record(envelope, freshPayload).copy(rowUid = freshUid))).applied)
            val freshId = target.syncEnvelopeDao().eventsByUid(listOf(freshUid)).single().eventId
            assertEquals(snapshot.copy(eventId = freshId), target.eventActorLinkDao().find(freshId))
            val stored = target.backupDataDao().eventActorLinks()
            val changedActor = JSONObject(freshPayload.toString()).apply { getJSONObject("actor_link").put("actor_name_snapshot", "Unexpected rewrite") }
            assertEquals(1, SyncApplyEngine(target).apply(listOf(record(envelope, changedActor).copy(rowUid = freshUid))).skipped)
            assertEquals(stored, target.backupDataDao().eventActorLinks())
            assertEquals(0, target.syncOutboxDao().count())
        } finally { source.close(); target.close() }
    }

    @Test fun openingDetailsRoundTripCorrectionAndReplayWithDifferentIds() = runBlocking {
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            val product = source.productDao().insert(ProductEntity(name = "Opening cream", unit = "Kg", uid = "u1_opening_product"))
            val cash = source.accountDao().insert(AccountEntity(name = "Opening wallet", type = "Asset", category = "Cash/Bank", uid = "u1_opening_wallet"))
            StoreIdentityService(source).save(StoreIdentityInput("Opening sync test"))
            val opening = OpeningStateService(source)
            opening.record(OpeningStateInput(date, listOf(OpeningInventoryBalance(product, 10.0)), listOf(OpeningAccountBalance(cash, 100.0))))
            opening.correct(OpeningStateInput(date, listOf(OpeningInventoryBalance(product, 7.0)), listOf(OpeningAccountBalance(cash, 130.0))))
            target.withWriteTransaction {
                executeSQL("UPDATE sqlite_sequence SET seq=1000 WHERE name IN ('products', 'accounts')")
                executeSQL("INSERT INTO sqlite_sequence(name,seq) VALUES('events',1000)")
            }
            target.syncOutboxDao().deleteAll()
            val envelopes = SyncEnvelopeFactory(source).build(source.syncOutboxDao().all()).envelopes
            val parents = envelopes.filter { it.getString("table") != "events" }.map { record(it) }
            val events = envelopes.filter { it.getString("table") == "events" }.map { record(it) }
            assertEquals(0, SyncApplyEngine(target).apply(parents).skipped)
            val legacyOpening = JSONObject(events.first().payload.toString()).apply {
                remove("opening_state"); remove("opening_state_contract_version")
            }
            assertEquals(1, SyncApplyEngine(target).apply(listOf(events.first().copy(payload = legacyOpening))).applied)
            assertTrue(target.backupDataDao().openingBatches().isEmpty())
            assertEquals(0, SyncApplyEngine(target).apply(events).skipped)
            val snapshot = OpeningStateService(target).current()
            assertNotNull("Opening detail batch must be received", snapshot)
            val tp = target.syncEnvelopeDao().productsByUid(listOf("u1_opening_product")).single().productId
            val ta = target.syncEnvelopeDao().accountsByUid(listOf("u1_opening_wallet")).single().accountId
            assertNotEquals(product, tp); assertNotEquals(cash, ta)
            assertNotEquals(opening.current()!!.eventId, snapshot!!.eventId)
            assertEquals(listOf(OpeningInventoryBalance(tp, 7.0)), snapshot.inventoryBalances)
            assertEquals(listOf(OpeningAccountBalance(ta, 130.0)), snapshot.accountBalances)
            assertEquals(2, OpeningStateService(target).history().size)
            val before = listOf(target.backupDataDao().events(), target.backupDataDao().inventoryLogs(),
                target.backupDataDao().financialLogs(), target.backupDataDao().openingBatches(),
                target.backupDataDao().openingInventoryEntries(), target.backupDataDao().openingAccountEntries())
            SyncApplyEngine(target).apply(events)
            assertEquals(before, listOf(target.backupDataDao().events(), target.backupDataDao().inventoryLogs(),
                target.backupDataDao().financialLogs(), target.backupDataDao().openingBatches(),
                target.backupDataDao().openingInventoryEntries(), target.backupDataDao().openingAccountEntries()))
            assertEquals(7.0, target.readModelDao().currentStock().single { it.productId == tp }.quantityOnHand, 0.0)
            assertEquals(130.0, target.readModelDao().accountBalances().single { it.accountId == ta }.balance, 0.0)
            assertEquals(0, target.syncOutboxDao().count())
        } finally { source.close(); target.close() }
    }

    @Test fun missingOpeningParentsAndMalformedDetailsPreserveFullBusinessRows() = runBlocking {
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            val product = source.productDao().insert(ProductEntity(name = "Dependency cream", unit = "Kg", uid = "u1_opening_dep_product"))
            val account = source.accountDao().insert(AccountEntity(name = "Dependency wallet", type = "Asset", category = "Cash/Bank", uid = "u1_opening_dep_account"))
            StoreIdentityService(source).save(StoreIdentityInput("Dependency test"))
            OpeningStateService(source).record(OpeningStateInput(date, listOf(OpeningInventoryBalance(product, 10.0)), listOf(OpeningAccountBalance(account, 100.0))))
            val envelopes = SyncEnvelopeFactory(source).build(source.syncOutboxDao().all()).envelopes
            val event = envelopes.single { it.getString("table") == "events" }
            val incoming = record(event)
            target.syncOutboxDao().deleteAll()
            assertEquals(1, SyncApplyEngine(target).apply(listOf(incoming)).parked)
            assertTrue(target.backupDataDao().events().isEmpty())
            assertTrue(target.backupDataDao().openingBatches().isEmpty())
            SyncApplyEngine(target).apply(envelopes.filter { it.getString("table") != "events" }.map { record(it) })
            assertEquals(0, target.syncParkedRowDao().count())
            assertEquals(1, target.backupDataDao().events().size)
            suspend fun rows() = listOf(target.backupDataDao().events(), target.backupDataDao().inventoryLogs(),
                target.backupDataDao().financialLogs(), target.backupDataDao().openingBatches(),
                target.backupDataDao().openingInventoryEntries(), target.backupDataDao().openingAccountEntries(),
                target.backupDataDao().eventActorLinks())
            val before = rows()
            val negative = JSONObject(incoming.payload.toString()).apply {
                getJSONObject("opening_state").getJSONArray("inventory_entries").getJSONObject(0).put("quantity", -1)
            }
            val inconsistent = JSONObject(incoming.payload.toString()).apply {
                getJSONObject("opening_state").getJSONArray("account_entries").getJSONObject(0).put("balance", 101)
            }
            val duplicate = JSONObject(incoming.payload.toString()).apply {
                val array = getJSONObject("opening_state").getJSONArray("inventory_entries")
                array.put(JSONObject(array.getJSONObject(0).toString()))
            }
            val invalidActor = JSONObject(incoming.payload.toString()).put("actor_link", JSONObject()
                .put("actor_name_snapshot", "Remote").put("actor_role_snapshot", "owner").put("attributed_at", "bad-date"))
            for ((index, bad) in listOf(negative, inconsistent, duplicate, invalidActor).withIndex()) {
                assertEquals(1, SyncApplyEngine(target).apply(listOf(incoming.copy(payload = bad))).skipped)
                assertEquals(before, rows())
                val freshUid = "u1_bad_opening_$index"
                val fresh = JSONObject(bad.toString()).put("event_uid", freshUid)
                assertEquals(1, SyncApplyEngine(target).apply(listOf(incoming.copy(rowUid = freshUid, payload = fresh))).skipped)
                assertEquals(before, rows())
            }
            assertEquals(0, target.syncOutboxDao().count())
        } finally { source.close(); target.close() }
    }

    private fun sale(uid: String) = EventEntity(createdAt = stamp, occurredAt = date,
        eventType = "SALE", saleQuantity = 1.0, saleValue = 190.0, unitRate = 190.0, eventUid = uid)

    private fun record(envelope: JSONObject, payload: JSONObject = envelope.getJSONObject("payload")) =
        SyncPulledRecord(1, null, envelope.getString("table"), envelope.getString("uid"), payload, "source")
}

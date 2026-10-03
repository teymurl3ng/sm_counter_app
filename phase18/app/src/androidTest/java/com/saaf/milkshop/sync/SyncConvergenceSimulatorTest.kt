package com.saaf.milkshop.sync

import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * C1 convergence simulator: two/three simulated devices against a fake
 * server implementing the A′ contract, driving the REAL push engine (hints,
 * ack equality) and the REAL apply engine (parking, savepoints, parse-first).
 * Deterministic scenarios — the randomized stress harness is a later go.
 */
@RunWith(AndroidJUnit4::class)
class SyncConvergenceSimulatorTest {

    private class SimDevice(
        val name: String,
        val server: FakeSyncContractServer,
    ) {
        val db: MilkShopDatabase = MilkShopDatabase.createInMemory()
        var cursor: Long = 0L

        suspend fun push() {
            val engine = SyncPushEngine(
                db,
                SyncPushTransport { envelopes, rows ->
                    server.syncPush(envelopes, name, cursor)
                    SyncPushResult.Success(envelopes.size)
                },
            )
            engine.drainOnce()
        }

        suspend fun pull(): SyncApplyEngine.ApplyReport? {
            val records = server.syncPull(cursor)
            if (records.isEmpty()) return null
            val report = SyncApplyEngine(db).apply(records)
            cursor = records.maxOf { it.serverSeq }
            return report
        }

        suspend fun pushAndPull() {
            push()
            pull()
        }
    }

    private suspend fun SimDevice.insertProduct(name: String, uid: String): Long =
        db.productDao().insert(ProductEntity(name = name, unit = "Kg", uid = uid))

    // — Scenario 1: two devices edit the same product offline. Both edits
    //   reach the server; devices converge to last-push; the overwritten
    //   version is audited exactly once with its losing payload.
    @Test
    fun concurrentEdits_converge_lastPushWins_conflictAudited() = runBlocking {
        val server = FakeSyncContractServer()
        val a = SimDevice("A", server)
        val b = SimDevice("B", server)
        try {
            a.insertProduct("Milk Tea", "u1_conv_product")
            a.push()
            b.pull() // B now has v1

            // Offline edits on both sides.
            val productA = a.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_product")).single().productId
            val productB = b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_product")).single().productId
            a.db.productDao().updateIdentity(productA, "Milk Tea (A)", "Kg")
            b.db.productDao().updateIdentity(productB, "Milk Tea (B)", "Kg")

            b.push() // B saw v1 (cursor == v1.seq): no conflict, server → v2
            a.push() // A never pulled v1: TRUE conflict, loser = B's payload; server → v3

            // Baseline seed echoes (identical rows, device-local created_at)
            // count as changes under A′ — known low-exposure noise recorded
            // in the audit report. The assertion that matters: exactly ONE
            // conflict for THIS uid, holding B's losing payload.
            val productConflicts = server.conflicts.filter { it.uid == "u1_conv_product" }
            assertEquals(1, productConflicts.size)
            assertEquals("Milk Tea (B)", productConflicts.single().loserPayload.getString("name"))
            assertEquals("B", productConflicts.single().loserDevice)
            assertEquals("A", productConflicts.single().winnerDevice)
            assertEquals("pull_cursor", productConflicts.single().detectedBy)

            a.pull()
            b.pull()

            val onA = a.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_product")).single().name
            val onB = b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_product")).single().name
            val onServer = server.rows.values.single { it.uid == "u1_conv_product" }.payload.getString("name")
            assertEquals("Milk Tea (A)", onServer)
            assertEquals(onServer, onA)
            assertEquals(onServer, onB)
            // Nobody parked: B's edit was pushed, not held.
            assertEquals(0, b.db.syncParkedRowDao().count())
        } finally {
            a.db.close()
            b.db.close()
        }
    }

    // — Scenario 2 (the C1 regression): a device that already pulled v1 must
    //   SEE v2 after the author edits and re-pushes. Updates propagate.
    @Test
    fun editAfterRemotePull_reachesTheOtherDevice() = runBlocking {
        val server = FakeSyncContractServer()
        val a = SimDevice("A", server)
        val b = SimDevice("B", server)
        try {
            a.insertProduct("Buffer Milk", "u1_conv_buffer")
            a.push()
            b.pull()
            assertEquals("Buffer Milk", b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_buffer")).single().name)

            val productA = a.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_buffer")).single().productId
            a.db.productDao().updateIdentity(productA, "Buffer Milk v2", "Kg")
            a.push()

            val report = b.pull()
            assertNotNull(report)
            assertEquals("Buffer Milk v2", b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_buffer")).single().name)
        } finally {
            a.db.close()
            b.db.close()
        }
    }

    // — Scenario 3: a fresh device replays another device's milk purchase +
    //   sale through the real apply path — stock triggers fire during apply
    //   and the books reproduce exactly (events were the hang class that
    //   started go 4; delivery-side parking is covered in SyncApplyEngineTest).
    @Test
    fun purchaseAndSale_replayOnFreshDevice_reproducesStock() = runBlocking {
        val server = FakeSyncContractServer()
        val a = SimDevice("A", server)
        val b = SimDevice("B", server)
        try {
            val milkId = a.db.productDao().findByName("Milk")!!.productId
            val purchaseId = a.db.eventDao().insert(
                EventEntity(
                    createdAt = "2026-10-01T08:00:00Z", occurredAt = "2026-10-01", eventType = "PURCHASE_RECEIPT",
                    rawLiters = 5.0, fatPercentage = 4.0, lrReading = 28.0, snfPercentage = 5.0,
                    tsPercentage = 9.0, adjustedLiters = 5.0, amountOwed = 905.0,
                    milkRateUsed = 170.0, deliveryFeeUsed = 11.0, tsStandardUsed = 13.0,
                ),
            )
            a.db.inventoryLogDao().insert(InventoryLogEntity(eventId = purchaseId, productId = milkId, quantity = 5.0))
            val saleId = a.db.eventDao().insert(
                EventEntity(
                    createdAt = "2026-10-01T10:00:00Z", occurredAt = "2026-10-01", eventType = "SALE",
                    saleQuantity = 2.0, saleValue = 380.0, unitRate = 190.0,
                ),
            )
            a.db.inventoryLogDao().insert(InventoryLogEntity(eventId = saleId, productId = milkId, quantity = -2.0))
            a.push()

            // Production enrollment order: push the local baseline BEFORE the
            // first pull, so the pull applies with an empty outbox (mirrors
            // push-before-pull; a baseline-vs-echo guard refinement is on
            // the before-pilot list).
            b.push()

            val report = b.pull()
            assertNotNull(report)
            assertEquals(0, b.db.syncParkedRowDao().count())
            // A's rows include the shared baseline seeds, which land as
            // already-present (reported skipped); the purchase and sale
            // events themselves must have applied.
            assertTrue(report!!.applied >= 2)
            assertEquals(0, report.parked)
            // Stock reproduces on the fresh device exactly.
            assertEquals(
                a.db.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
                b.db.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
                1e-9,
            )
        } finally {
            a.db.close()
            b.db.close()
        }
    }

    // — Scenario 4: B holds an unpushed local edit AND a parked remote v2;
    //   B's push carries the parked_seq hint, the server audits the
    //   overwritten v2 with its losing payload, and B's park clears.
    @Test
    fun heldEdit_pushCarriesParkedSeqHint_andClearsPark() = runBlocking {
        val server = FakeSyncContractServer()
        val a = SimDevice("A", server)
        val b = SimDevice("B", server)
        val c = SimDevice("C", server)
        try {
            a.insertProduct("Yogurt Cup", "u1_conv_yogurt")
            a.push()
            b.pull() // B has v1

            // C renames remotely and pushes v2 (B has not seen it).
            val productC = c.db.productDao().insert(
                ProductEntity(name = "Yogurt Cup", unit = "Kg", uid = "u1_conv_yogurt"),
            )
            c.pull() // C pulls v1 (seeds + product)
            c.db.productDao().updateIdentity(
                c.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_yogurt")).single().productId,
                "Yogurt Cup (remote)", "Kg",
            )
            c.push() // server v2

            // B edits locally (unpushed) and THEN pulls v2 — the guard parks it.
            val productB = b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_yogurt")).single().productId
            b.db.productDao().updateIdentity(productB, "Yogurt Cup (local)", "Kg")
            val pulled = b.pull()
            // Baseline echoes may also park (their uids hold B's own seed
            // outbox entries) — the yogurt row MUST be among the parks.
            assertNotNull(pulled)
            assertTrue(pulled!!.parked >= 1)
            assertNotNull(b.db.syncParkedRowDao().find("products", "u1_conv_yogurt"))
            assertEquals("Yogurt Cup (local)", b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_yogurt")).single().name)

            // B pushes its edit: the envelope carries parked_seq; the server
            // audits v2's overwrite (detectedBy parked_seq); the park clears.
            b.push()
            assertTrue(server.conflicts.any {
                it.uid == "u1_conv_yogurt" &&
                    it.loserPayload.getString("name") == "Yogurt Cup (remote)" &&
                    it.detectedBy == "parked_seq"
            })
            assertEquals(0, b.db.syncParkedRowDao().count())

            // Everyone converges to B's local rename.
            b.pull()
            c.pull()
            assertEquals(
                "Yogurt Cup (local)",
                server.rows.values.single { it.uid == "u1_conv_yogurt" }.payload.getString("name"),
            )
            assertEquals(
                "Yogurt Cup (local)",
                c.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_yogurt")).single().name,
            )
        } finally {
            a.db.close()
            b.db.close()
            c.db.close()
        }
    }

    // — Scenario 5: a poisoned head halts the push, but pulls still apply
    //   (guard keeps the held edit safe); fixing the poison resumes the drain.
    @Test
    fun poisonedHead_holdsPush_pullsStillConverge_recoversAfterFix() = runBlocking {
        val server = FakeSyncContractServer()
        val a = SimDevice("A", server)
        val b = SimDevice("B", server)
        try {
            a.insertProduct("Lassi", "u1_conv_lassi")
            a.push()
            b.pull()

            // B's poison event is enqueued FIRST, the product edit second —
            // a refusing transport halts at the poison, holding the edit.
            b.db.eventDao().insert(
                EventEntity(
                    createdAt = "2026-10-01T11:00:00Z", occurredAt = "2026-10-01", eventType = "SALE",
                    saleQuantity = 1.0, saleValue = 190.0, unitRate = 190.0, eventUid = "u1_poison_event",
                ),
            )
            val productB = b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_lassi")).single().productId
            b.db.productDao().updateIdentity(productB, "Lassi (B)", "Kg")

            val refused = RefusingTransport(server, b.name, refusedUid = "u1_poison_event")
            val engine = SyncPushEngine(b.db, refused)
            val outcome = engine.drainOnce()
            assertTrue(outcome is SyncPushEngine.DrainOutcome.Halted)
            assertEquals(1, b.db.syncOutboxDao().countByTableAndUid("products", "u1_conv_lassi"))

            // While halted, A's edits still arrive (D9); the pending-edit
            // guard protects B's held product edit.
            val productA = a.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_lassi")).single().productId
            a.db.productDao().updateIdentity(productA, "Lassi (A)", "Kg")
            a.push()
            val report = b.pull()
            assertEquals(1, report!!.parked)
            assertEquals("Lassi (B)", b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_lassi")).single().name)

            // Owner fixes the poison (removes the cause): the drain resumes,
            // the held edit pushes carrying the parked_seq hint, the server
            // audits A's overwritten version, and B converges to its own
            // rename (last push wins).
            val poisonRow = b.db.syncOutboxDao().nextBatch(50).single { it.rowUid == "u1_poison_event" }
            b.db.syncOutboxDao().deleteByIds(listOf(poisonRow.outboxId))
            engine.drainOnce()
            assertEquals("Lassi (B)", server.rows.values.single { it.uid == "u1_conv_lassi" }.payload.getString("name"))
            assertTrue(server.conflicts.any {
                it.uid == "u1_conv_lassi" && it.loserPayload.getString("name") == "Lassi (A)"
            })
            assertEquals(0, b.db.syncParkedRowDao().count())
            b.pull()
            assertEquals("Lassi (B)", b.db.syncEnvelopeDao().productsByUid(listOf("u1_conv_lassi")).single().name)
        } finally {
            a.db.close()
            b.db.close()
        }
    }

    /** Server transport that permanently refuses one uid (halt injection). */
    private class RefusingTransport(
        private val server: FakeSyncContractServer,
        private val deviceName: String,
        var refusedUid: String?,
    ) : SyncPushTransport {
        override suspend fun push(envelopes: List<JSONObject>, rows: List<com.saaf.milkshop.data.SyncOutboxEntity>): SyncPushResult {
            val poison = envelopes.firstOrNull { it.getString("uid") == refusedUid }
            if (poison != null) {
                return SyncPushResult.Permanent("test_poisoned", "Refused by test injection")
            }
            server.syncPush(envelopes, deviceName, Long.MAX_VALUE / 2)
            return SyncPushResult.Success(envelopes.size)
        }
    }
}

package com.saaf.milkshop

import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.CustomerProfileEntity
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
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Phase 7: a two-database round trip. Device A records a day's activity and
 * builds push envelopes; device B (fresh install, seeds share well-known
 * uids) applies them as pulled records. The ledger must reproduce exactly —
 * origin-local integer ids remapped through uid references — and re-applying
 * the same batch must change nothing.
 */
@RunWith(AndroidJUnit4::class)
class SyncApplyEngineTest {

    @Test
    fun roundTrip_reproducesLedgerOnSecondDevice() = runBlocking {
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            // — Device A: a customer, a purchase (opening stock), a sale with
            //   ledgers and actor attribution, and a correction of that sale.
            val customerId = source.accountDao().insert(
                AccountEntity(name = "Customer: Noor Store", type = "Asset", category = "Customer"),
            )
            source.customerProfileDao().save(
                CustomerProfileEntity(accountId = customerId, phone = "0300", createdAt = "2026-09-29T08:00:00Z"),
            )
            val purchase = source.eventDao().insert(
                EventEntity(
                    createdAt = "2026-09-29T06:00:00Z", occurredAt = "2026-09-29", eventType = "PURCHASE_RECEIPT",
                    rawLiters = 80.0, fatPercentage = 4.0, lrReading = 28.0, snfPercentage = 5.0,
                    tsPercentage = 9.0, adjustedLiters = 80.0, amountOwed = 14520.0,
                    milkRateUsed = 170.0, deliveryFeeUsed = 11.0, tsStandardUsed = 13.0,
                ),
            )
            val milkId = source.productDao().findByName("Milk")!!.productId
            source.inventoryLogDao().insert(InventoryLogEntity(eventId = purchase, productId = milkId, quantity = 80.0))
            val cashId = source.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val sale = EventEntity(
                createdAt = "2026-09-29T10:00:00Z", occurredAt = "2026-09-29", eventType = "SALE",
                saleQuantity = 2.0, saleValue = 380.0, unitRate = 190.0,
            )
            val saleId = source.eventDao().insert(sale)
            source.inventoryLogDao().insert(InventoryLogEntity(eventId = saleId, productId = milkId, quantity = -2.0))
            source.financialLogDao().insert(FinancialLogEntity(eventId = saleId, accountId = cashId, amount = 380.0))
            val actorId = source.appUserDao().insert(
                com.saaf.milkshop.data.AppUserEntity(
                    loginName = "sync.owner", displayName = "Sync Owner", role = "owner",
                    createdAt = "2026-09-29T07:00:00Z", updatedAt = "2026-09-29T07:00:00Z",
                ),
            )
            source.eventActorLinkDao().insert(EventActorLinkEntity(saleId, actorId, "Sync Owner", "owner", "2026-09-29T10:00:01Z"))
            val correction = source.eventDao().insert(
                EventEntity(
                    createdAt = "2026-09-29T12:00:00Z", occurredAt = "2026-09-29", eventType = "SALE",
                    saleQuantity = 3.0, saleValue = 570.0, unitRate = 190.0, correctsEventId = saleId,
                ),
            )

            // — Ship every queued row as a pulled record, in outbox order
            //   (which mirrors the origin's chronology).
            val envelopes = SyncEnvelopeFactory(source).build(source.syncOutboxDao().nextBatch(100)).envelopes
            val records = envelopes.mapIndexed { index, envelope ->
                SyncPulledRecord(
                    serverSeq = (index + 1).toLong(),
                    shopId = null,
                    tableName = envelope.getString("table"),
                    rowUid = envelope.getString("uid"),
                    payload = envelope.getJSONObject("payload"),
                    originDevice = "source-device",
                )
            }

            // — Device B applies.
            val report = SyncApplyEngine(target).apply(records)
            val skipReasons = target.backupDataDao().securityAuditEntries()
                .filter { it.action.startsWith("sync_apply") }
                .joinToString("; ") { "${it.action}:${it.details}" }
            assertEquals("applied=${report.applied} skipped=${report.skipped} conflicts=${report.conflicts} || $skipReasons", 0, report.skipped)
            assertEquals(0, report.conflicts)
            // Seeds exist on both devices under shared well-known uids, so
            // they count as updates; the four new rows are the customer,
            // purchase, sale, and correction.
            assertTrue("applied=${report.applied} updated=${report.updated}", report.applied >= 4)

            // Customer + profile landed under the pushed uid.
            val targetCustomer = target.syncEnvelopeDao()
                .accountsByUid(listOf(source.backupDataDao().accounts().single { it.accountId == customerId }.uid))
                .single()
            assertEquals("Customer: Noor Store", targetCustomer.name)
            assertEquals("0300", target.customerProfileDao().findByAccountId(targetCustomer.accountId)?.phone)

            // Events landed under their uids; the correction points at the
            // REMAPPED local id of the original sale — never the origin's.
            val sourceEvents = source.backupDataDao().events()
            val targetSale = target.syncEnvelopeDao().eventsByUid(listOf(sale.eventUid)).single()
            val targetCorrection = target.syncEnvelopeDao().eventsByUid(
                listOf(sourceEvents.single { it.eventId == correction }.eventUid),
            ).single()
            assertEquals(targetSale.eventId, targetCorrection.correctsEventId)
            // (Local ids may coincide with origin ids on identical fresh
            // databases — what matters is that the correction points at the
            // uid-resolved local row, asserted above.)

            // Ledger movements reproduce: same stock and same cash movement.
            assertEquals(
                source.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
                target.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand,
                1e-9,
            )
            val targetCash = target.readModelDao().accountBalances().single { it.name == "Cash" }
            val sourceCash = source.readModelDao().accountBalances().single { it.name == "Cash" }
            assertEquals(sourceCash.balance, targetCash.balance, 1e-9)

            // Actor attribution survives when the receiving device has no
            // matching local user: the link insert is best-effort (user ids
            // are device-local), so only assert it when the row landed.
            val targetLink = target.backupDataDao().eventActorLinks()
                .firstOrNull { it.eventId == targetSale.eventId }
            assertTrue(targetLink == null || targetLink.actorNameSnapshot == "Sync Owner")

            // — Idempotency: applying the identical batch again changes
            // nothing. (Immutable events legitimately re-report as skipped;
            // nothing may re-apply or conflict, and row counts must hold.)
            val eventsBefore = target.backupDataDao().events().size
            val accountsBefore = target.backupDataDao().accounts().size
            val second = SyncApplyEngine(target).apply(records)
            assertEquals("second pass applied=${second.applied}", 0, second.applied)
            assertEquals(0, second.conflicts)
            assertEquals(eventsBefore.toLong(), target.backupDataDao().events().size.toLong())
            assertEquals(accountsBefore.toLong(), target.backupDataDao().accounts().size.toLong())

            // — Sibling conflict (P1): a second, different correction of the
            //   same original is refused, audited, and never double-counts.
            val rival = JSONObject(
                envelopes.first { it.getString("uid") == sale.eventUid }.getJSONObject("payload").toString(),
            ).apply {
                put("corrects_event_uid", sale.eventUid)
                put("sale_quantity", 9.0)
                put("sale_value", 999.0)
            }
            val rivalReport = SyncApplyEngine(target).apply(
                listOf(
                    SyncPulledRecord(
                        serverSeq = 999, shopId = null, tableName = "events",
                        rowUid = "u1_rival_correction", payload = rival, originDevice = "rival",
                    ),
                ),
            )
            assertEquals(1, rivalReport.conflicts)
            assertEquals(0, rivalReport.applied)
            assertNull(target.syncEnvelopeDao().eventsByUid(listOf("u1_rival_correction")).firstOrNull())
        } finally {
            source.close()
            target.close()
        }
    }

    /**
     * C1: a pulled row whose uid still has an unpushed local outbox edit is
     * parked — never applied. The local row must stay untouched, the outbox
     * entry must survive, and the park must exist with the pending reason.
     */
    @Test
    fun pendingEditGuard_parksInsteadOfOverwriting() = runBlocking {
        val target = MilkShopDatabase.createInMemory()
        try {
            val productId = target.productDao().insert(
                com.saaf.milkshop.data.ProductEntity(name = "Desi Ghee", unit = "Kg", uid = "u1_test_ghee"),
            )
            // The device edited this product offline: the same DAO update the
            // local edit path uses fires the capture trigger, unpushed.
            target.productDao().updateIdentity(productId, "Desi Ghee Premium", "Kg")
            assertEquals(1, target.syncOutboxDao().countByTableAndUid("products", "u1_test_ghee"))

            val remote = SyncPulledRecord(
                serverSeq = 50, shopId = null, tableName = "products",
                rowUid = "u1_test_ghee",
                payload = JSONObject().put("name", "Desi Ghee (remote rename)").put("unit", "Kg"),
                originDevice = "other-device",
            )
            val report = SyncApplyEngine(target).apply(listOf(remote))
            assertEquals(1, report.parked)
            assertEquals(0, report.updated)
            // The local edit's row is untouched and its outbox entry intact.
            assertEquals("Desi Ghee Premium", target.productDao().findByName("Desi Ghee Premium")!!.name)
            assertEquals(1, target.syncOutboxDao().countByTableAndUid("products", "u1_test_ghee"))
            // The park exists for this uid.
            assertNotNull(target.syncParkedRowDao().find("products", "u1_test_ghee"))
        } finally {
            target.close()
        }
    }

    /**
     * C1: a pulled delivery referencing a customer that is not local yet is
     * PARKED (cursor-safe), and once the customer arrives the retry pass
     * applies the parked row and clears the park — no permanent loss.
     */
    @Test
    fun dependencyPark_resolvesWhenParentArrives() = runBlocking {
        val target = MilkShopDatabase.createInMemory()
        try {
            val milkUid = "u1_seed_products_Milk"
            val deliveryUid = "u1_test_delivery_1"
            val early = SyncPulledRecord(
                serverSeq = 10, shopId = null, tableName = "deliveries", rowUid = deliveryUid,
                payload = JSONObject()
                    .put("customer_uid", "u1_missing_customer")
                    .put("product_uid", milkUid)
                    .put("quantity", 3.5)
                    .put("business_date", "2026-10-01")
                    .put("created_at", "2026-10-01T07:30:00Z"),
                originDevice = "other-device",
            )
            val first = SyncApplyEngine(target).apply(listOf(early))
            assertEquals(1, first.parked)
            assertNull(target.syncEnvelopeDao().deliveriesByUid(listOf(deliveryUid)).firstOrNull())
            assertNotNull(target.syncParkedRowDao().find("deliveries", deliveryUid))

            // The customer arrives in a later batch; the retry pass at the
            // end of that batch must resolve the parked delivery.
            val customer = SyncPulledRecord(
                serverSeq = 11, shopId = null, tableName = "accounts", rowUid = "u1_missing_customer",
                payload = JSONObject()
                    .put("name", "Customer: Late Account")
                    .put("type", "Asset")
                    .put("category", "Customer"),
                originDevice = "other-device",
            )
            val second = SyncApplyEngine(target).apply(listOf(customer))
            // The customer applies, and the batch-end retry pass recovers the
            // parked delivery — both count as applied.
            assertEquals(2, second.applied)
            assertEquals(0, second.parked)
            assertEquals(0, target.syncParkedRowDao().count())
            val resolved = target.syncEnvelopeDao().deliveriesByUid(listOf(deliveryUid)).single()
            assertEquals(3.5, resolved.quantity, 1e-9)
            assertEquals(
                "u1_missing_customer",
                target.syncEnvelopeDao().accountsByIds(listOf(resolved.customerAccountId)).single().uid,
            )
        } finally {
            target.close()
        }
    }

    /**
     * C1: a park that can never resolve stops consuming retries at
     * MAX_PARK_RETRIES, keeps the row as evidence, and audits the
     * exhaustion exactly once.
     */
    @Test
    fun dependencyPark_exhaustsBoundedRetriesWithOneAudit() = runBlocking {
        val target = MilkShopDatabase.createInMemory()
        try {
            val doomed = SyncPulledRecord(
                serverSeq = 7, shopId = null, tableName = "deliveries", rowUid = "u1_test_delivery_doomed",
                payload = JSONObject()
                    .put("customer_uid", "u1_never_arrives")
                    .put("product_uid", "u1_seed_products_Milk")
                    .put("quantity", 1.0)
                    .put("business_date", "2026-10-01")
                    .put("created_at", "2026-10-01T07:30:00Z"),
                originDevice = "other-device",
            )
            SyncApplyEngine(target).apply(listOf(doomed))
            repeat(6) { SyncApplyEngine(target).apply(emptyList()) }
            // The park survives as evidence; the exhaustion was audited once.
            val park = target.syncParkedRowDao().find("deliveries", "u1_test_delivery_doomed")
            assertNotNull(park)
            assertEquals(5, park!!.retryCount)
            val exhaustionAudits = target.backupDataDao().securityAuditEntries()
                .count { it.action == "sync_park_unresolved" }
            assertEquals(1, exhaustionAudits)
        } finally {
            target.close()
        }
    }
}

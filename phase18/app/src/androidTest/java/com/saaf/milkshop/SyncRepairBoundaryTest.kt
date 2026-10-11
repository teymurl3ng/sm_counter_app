package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room3.withWriteTransaction
import androidx.room3.executeSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.owner.NativeOwnerSyncFacade
import com.saaf.milkshop.application.cashclose.NativeCashCloseFacade
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class SyncRepairBoundaryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = "2026-10-10T08:00:00Z"

    @Test fun failedTakeServerKeepsReviewAndLocalEditThenSuccessfulApplyResolvesBoth() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val product = db.productDao().findByName("Milk")!!
            db.syncOutboxDao().setHeld("products", product.uid, true)
            db.syncRebaselineHoldDao().upsert(SyncRebaselineHoldEntity("products", product.uid, "{}", "remote", 9, now))
            val facade = NativeOwnerSyncFacade(context, db)
            assertTrue(runCatching { facade.resolveHold("products", product.uid, false) }.isFailure)
            assertEquals(1, db.syncRebaselineHoldDao().count())
            assertEquals(1, db.syncOutboxDao().countByTableAndUid("products", product.uid))
            db.syncParkedRowDao().upsert(SyncParkedRowEntity("products", product.uid, "{}", 9, 0, now))
            assertTrue(runCatching { facade.resolveHold("products", product.uid, false) }.isFailure)
            assertEquals(1, db.syncRebaselineHoldDao().count())
            assertTrue(db.syncOutboxDao().all().single { it.rowUid == product.uid }.held)
            val payload = JSONObject().put("name", "Server milk").put("unit", product.unit).toString()
            db.syncParkedRowDao().upsert(SyncParkedRowEntity("products", product.uid, payload, 10, 0, now))
            facade.resolveHold("products", product.uid, false)
            assertEquals("Server milk", db.syncEnvelopeDao().productsByUid(listOf(product.uid)).single().name)
            assertEquals(0, db.syncOutboxDao().countByTableAndUid("products", product.uid))
            assertEquals(0, db.syncRebaselineHoldDao().count())
            assertNull(db.syncParkedRowDao().find("products", product.uid))
        } finally { db.close() }
    }

    @Test fun firstAttendancePullPreservesArchiveFlag() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val employeeId = db.accountDao().insert(AccountEntity(name = "Employee: Archive", type = "Expense", category = "Salary"))
            val employee = db.accountDao().findById(employeeId)!!
            val payload = JSONObject().put("employee_uid", employee.uid).put("business_date", "2026-10-10")
                .put("status", "present").put("created_at", now).put("archived", true)
            val result = SyncApplyEngine(db).apply(listOf(SyncPulledRecord(1, "shop", "attendance_records", "archived-attendance", payload, "remote")))
            assertEquals(0, result.skipped)
            assertTrue(db.syncEnvelopeDao().attendanceByUid(listOf("archived-attendance")).single().archived)
        } finally { db.close() }
    }

    @Test fun queuedWorkIsNotServerContactAndBothCloseRefusalDirectionsAreCounted() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("proof.owner", "Owner", "boundary-passphrase".toCharArray()).session.sessionId
            val facade = NativeCashCloseFacade(db, security)
            assertTrue(db.syncOutboxDao().count() > 0)
            assertEquals("Server contact unknown", facade.load(session).lastSyncedLabel)
            db.securityAuditDao().insert(SecurityAuditEntryEntity(occurredAt = now, action = "sync_push_halted", result = "failure"))
            db.securityAuditDao().insert(SecurityAuditEntryEntity(occurredAt = now, action = "sync_apply_skipped", result = "failure", details = "Business date is closed"))
            db.securityAuditDao().insert(SecurityAuditEntryEntity(occurredAt = now, action = "sync_apply_skipped", result = "failure", details = "Invalid product"))
            assertEquals(2, db.securityAuditDao().refusalsSince("2026-10-10T00:00:00Z"))
        } finally { db.close() }
    }

    @Test fun malformedUnorderedOrForeignShopPullsRemainRetryable() = runBlocking {
        val config = SyncServerConfig(SyncServerConfig.ENVIRONMENT_DEV, "https://example.invalid", "test")
        val tokens = SyncTokenStore(context)
        tokens.save(SyncTokenStore.Session("test", null, null))
        fun row(seq: Int, shop: String = "shop") = """{"server_seq":$seq,"shop_id":"$shop","table":"products","uid":"p$seq","payload":{}}"""
        try {
            for (body in listOf("{}", """{"ok":true,"records":[{}]}""",
                """{"ok":true,"records":[${row(2)},${row(1)}]}""",
                """{"ok":true,"records":[${row(1, "other-shop")}]}""")) {
                val client = SyncPullClient(SyncHttp { SyncHttpResponse(200, body.toByteArray()) }, config, tokens)
                assertEquals(SyncPullOutcome.RetryLater("malformed_response"), client.pull(0, shopId = "shop"))
            }
            val valid = """{"ok":true,"records":[${row(1)},${row(2)}]}"""
            val fetched = SyncPullClient(SyncHttp { SyncHttpResponse(200, valid.toByteArray()) }, config, tokens).pull(0, shopId = "shop")
            assertEquals(2L, (fetched as SyncPullOutcome.Fetched).batch.latestServerSeq)
        } finally { tokens.clear() }
    }

    @Test fun businessDayUsesShopZoneAcrossMonthBoundary() {
        val previous = BusinessClock.currentZone()
        TrustedTime.resetForTest(context); TrustedTime.warmUp(context)
        try {
            TrustedTime.anchor(Instant.parse("2026-10-31T20:00:00Z").toEpochMilli())
            BusinessClock.setZone("Asia/Karachi")
            assertEquals("2026-11-01", BusinessClock.today().toString())
            BusinessClock.setZone("America/Los_Angeles")
            assertEquals("2026-10-31", BusinessClock.today().toString())
        } finally { BusinessClock.setZone(previous.id); TrustedTime.resetForTest(context) }
    }

    @Test fun enrollmentTeardownWaitsForInFlightSyncGate() = runBlocking {
        SyncScheduler.suppressSchedulingForTest()
        SyncServerConfigStore.save(context, SyncServerConfig(SyncServerConfig.ENVIRONMENT_DEV, "https://example.invalid", "test"))
        val release = CompletableDeferred<Unit>()
        val inFlight = async(start = CoroutineStart.UNDISPATCHED) {
            SyncOperationGate.exclusive { release.await() }
        }
        val teardown = async(start = CoroutineStart.UNDISPATCHED) {
            SyncEnrollment(context, SyncHttp { error("No network is expected") }).unenroll()
        }
        try {
            assertFalse(teardown.isCompleted)
            assertNotNull(SyncServerConfigStore.load(context))
            release.complete(Unit); inFlight.await(); teardown.await()
            assertNull(SyncServerConfigStore.load(context))
        } finally { release.complete(Unit); inFlight.cancel(); teardown.cancel(); SyncServerConfigStore.clear(context) }
    }

    @Test fun incomingUpdatesCannotBypassInsertShapeRules() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val product = db.productDao().findByName("Milk")!!
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
            db.syncOutboxDao().deleteAll()
            val invalid = listOf(
                SyncPulledRecord(1, "shop", "products", product.uid,
                    JSONObject().put("name", "Bad unit").put("unit", "Gallons"), "remote"),
                SyncPulledRecord(2, "shop", "accounts", cash.uid,
                    JSONObject().put("name", "Bad category").put("type", "Liability").put("category", "Cash/Bank"), "remote"))
            assertEquals(2, SyncApplyEngine(db).apply(invalid).skipped)
            assertEquals(product, db.productDao().findByName("Milk"))
            assertEquals("Asset", db.accountDao().findById(cash.accountId)!!.type)
        } finally { db.close() }
    }

    @Test fun missingRefusalAuditRollsBackTheBatchInsteadOfReturningSuccess() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            db.syncOutboxDao().deleteAll()
            db.withWriteTransaction { executeSQL("""CREATE TRIGGER reject_test_refusal_audit
                BEFORE INSERT ON security_audit_entries WHEN NEW.action='sync_apply_skipped'
                BEGIN SELECT RAISE(ABORT,'Injected audit failure'); END""") }
            val records = listOf(
                SyncPulledRecord(1,"shop","products","valid-before-refusal",JSONObject().put("name","Valid new product").put("unit","Kg"),"remote"),
                SyncPulledRecord(2,"shop","products","invalid-refusal",JSONObject().put("name","Invalid").put("unit","Gallons"),"remote"))
            assertTrue(runCatching { SyncApplyEngine(db).apply(records) }.isFailure)
            assertTrue(db.syncEnvelopeDao().productsByUid(listOf("valid-before-refusal")).isEmpty())
            db.withWriteTransaction { executeSQL("DROP TRIGGER reject_test_refusal_audit") }
            assertEquals(1, SyncApplyEngine(db).apply(records).skipped)
            assertEquals(1, db.syncEnvelopeDao().productsByUid(listOf("valid-before-refusal")).size)
            assertEquals(1, db.securityAuditDao().all().count { it.action == "sync_apply_skipped" })
        } finally { db.close() }
    }
}

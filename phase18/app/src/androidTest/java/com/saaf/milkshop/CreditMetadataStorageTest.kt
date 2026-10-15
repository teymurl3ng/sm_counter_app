package com.saaf.milkshop

import android.net.Uri
import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File

class CreditMetadataStorageTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val migrationName = "credit-metadata-migration.db"
    @get:Rule val migration = MigrationTestHelper(instrumentation, context.getDatabasePath(migrationName),
        BundledSQLiteDriver(), MilkShopDatabase::class,
        { Room.databaseBuilder<MilkShopDatabase>(context, migrationName).setDriver(BundledSQLiteDriver()).addMigrations(MilkShopDatabase.MIGRATION_17_18).build() }, emptyList())
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var owner: String
    private lateinit var uid: String
    private var customer = 0L
    private var milk = 0L
    private val now = "2026-10-15T08:00:00Z"
    @Before fun setup() = runBlocking {
        db = MilkShopDatabase.createInMemory()
        security = UserSecurityService(db)
        owner = security.bootstrapOwner("credit.storage.owner", "Storage Owner", PASSWORD.toCharArray()).session.sessionId
        customer = CustomerManagementService(db).create("Synthetic weekly customer")
        uid = db.accountDao().findById(customer)!!.uid
        val vendor = VendorManagementService(db).create("Synthetic stock supplier")
        VendorPurchaseService(db).record(VendorPurchaseInput(vendor, "2026-10-15", 100.0, 4.0, 28.0))
        milk = db.productDao().findByName("Milk")!!.productId
    }
    @After fun close() { db.close() }
    private suspend fun ready() = db.creditMetadataDao().saveState(CreditMetadataSyncStateEntity(shopUid = "synthetic-shop", contractVersion = 1, enabled = true))
    private fun revision(id: String, prior: String = CreditMetadataStore.DEFAULT_REVISION, term: CreditPaymentTerm = CreditPaymentTerm.Monthly) =
        CreditTermRevisionEntity(id, uid, prior, term.kind.name, term.value, "local-user:1", now)
    private suspend fun sale() = SaleService(db).record(SaleInput(milk, "2026-10-15", "credit-retail", 100.0,
        value = 100.0, payment = SalePayment.OnCredit(customer)))

    @Test fun migrationPreservesLedgerAndClassifiesOnlyOriginalCreditSales() = runBlocking {
        context.deleteDatabase(migrationName)
        migration.createDatabase(17).use { c ->
            c.execSQL("INSERT INTO accounts(account_id,name,type,category,uid) VALUES(501,'Customer: Legacy','Asset','Customer','legacy-customer')")
            c.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,channel,sale_quantity,sale_value,unit_rate,surcharge_applied,event_uid) VALUES(601,'$now','2026-10-01','SALE','credit-retail',1,100,100,0,'legacy-sale')")
            c.execSQL("INSERT INTO financial_log(event_id,account_id,amount) VALUES(601,501,100.004)")
        }
        migration.runMigrationsAndValidate(18, listOf(MilkShopDatabase.MIGRATION_17_18)).use { c ->
            c.prepare("SELECT evidence,customer_uid,term_value FROM credit_sale_agreements WHERE root_sale_uid='legacy-sale'").use {
                assertTrue(it.step()); assertEquals("LEGACY_WEEKLY", it.getText(0)); assertEquals("legacy-customer", it.getText(1)); assertEquals(7, it.getLong(2))
            }
            c.prepare("SELECT amount FROM financial_log WHERE event_id=601").use { assertTrue(it.step()); assertEquals(100.004, it.getDouble(0), 0.0) }
            c.prepare("SELECT COUNT(*) FROM credit_metadata_pending").use { assertTrue(it.step()); assertEquals(0, it.getLong(0)) }
        }
    }

    @Test fun acceptedRevisionsRejectStaleHeadsIdentityChangesAndSqlMutation() = runBlocking {
        val store = CreditMetadataStore(db)
        val first = revision("revision-1")
        store.applyAcceptedRevision(first)
        store.applyAcceptedRevision(first)
        store.applyAcceptedRevision(revision("revision-2", "revision-1", CreditPaymentTerm(CreditTermKind.NET_DAYS, 30)))
        store.applyAcceptedRevision(first)
        assertEquals("revision-2", db.creditMetadataDao().head(uid)!!.revisionUid)
        assertTrue(runCatching { store.applyAcceptedRevision(revision("stale-revision")) }.isFailure)
        assertTrue(runCatching { store.applyAcceptedRevision(first.copy(termValue = 7, termKind = "NET_DAYS")) }.isFailure)
        assertTrue(runCatching { db.withWriteTransaction { executeSQL("UPDATE credit_term_revisions SET term_value=7 WHERE revision_uid='revision-1'") } }.isFailure)
        assertTrue(runCatching { db.withWriteTransaction { executeSQL("DELETE FROM credit_term_revisions WHERE revision_uid='revision-2'") } }.isFailure)
        assertEquals(2, db.creditMetadataDao().revisions().size)
    }

    @Test fun ownerProposalsStayPendingAndOtherRolesAndExpiredSessionsCannotWrite() = runBlocking {
        val service = CreditReviewService(db, security)
        assertTrue(runCatching { service.proposeTerms(owner, uid, CreditMetadataStore.DEFAULT_REVISION, CreditPaymentTerm.Monthly, "proposal") }.isFailure)
        ready()
        service.proposeTerms(owner, uid, CreditMetadataStore.DEFAULT_REVISION, CreditPaymentTerm.Monthly, "proposal")
        service.proposeTerms(owner, uid, CreditMetadataStore.DEFAULT_REVISION, CreditPaymentTerm.Monthly, "proposal")
        assertNull(db.creditMetadataDao().head(uid))
        assertEquals(1, db.creditMetadataDao().pending().size)
        for (role in listOf("manager", "admin")) {
            val user = security.beginUserEnrollment(owner, CreateUserInput("credit.$role", "Credit $role", role))
            security.completeUserEnrollment(user.enrollmentToken, PASSWORD.toCharArray())
            val session = security.login("credit.$role", PASSWORD.toCharArray()).sessionId
            assertTrue(runCatching { service.proposeTerms(session, uid, CreditMetadataStore.DEFAULT_REVISION, CreditPaymentTerm.Monthly) }.exceptionOrNull() is SecurityFailure.PermissionDenied)
            assertTrue(runCatching { service.setFlag(session, uid, "Should not save") }.exceptionOrNull() is SecurityFailure.PermissionDenied)
        }
        security.logout(owner)
        assertTrue(runCatching { service.setFlag(owner, uid, "Expired") }.exceptionOrNull() is SecurityFailure.SessionExpired)
        assertTrue(db.creditMetadataDao().actions().isEmpty())
    }

    @Test fun saleLegacyEvidenceIsAtomicAndCannotBeRewrittenByFutureTerms() = runBlocking {
        val first = sale()
        val root = db.eventDao().findById(first.eventId)!!.eventUid
        assertEquals(CreditTermEvidence.LegacyWeekly, CreditMetadataStore(db).evidence(uid)[root])
        CreditMetadataStore(db).applyAcceptedRevision(revision("future-monthly"))
        assertEquals(CreditTermEvidence.LegacyWeekly, CreditMetadataStore(db).evidence(uid)[root])
        assertTrue(runCatching { CreditMetadataStore(db).applyAgreement(CreditSaleAgreementEntity(root, uid, "RECORDED", "CALENDAR_MONTHS", 1, "future-monthly")) }.isFailure)
        val count = db.eventDao().count()
        val stock = db.backupDataDao().inventoryLogs()
        ready() // Contracted posting remains deliberately disabled until protocol integration.
        assertTrue(runCatching { sale() }.isFailure)
        assertEquals(count, db.eventDao().count())
        assertEquals(stock, db.backupDataDao().inventoryLogs())
    }

    @Test fun recordedAgreementKeepsAcceptedRevisionAndRefusesForeignOrMissingFacts() = runBlocking {
        val store = CreditMetadataStore(db)
        store.applyAcceptedRevision(revision("monthly"))
        val root = db.withWriteTransaction {
            val id = db.eventDao().insert(EventEntity(createdAt = now, occurredAt = "2026-10-15", eventType = "SALE",
                channel = "credit-retail", saleQuantity = 1.0, saleValue = 100.0, unitRate = 100.0))
            db.financialLogDao().insert(FinancialLogEntity(eventId = id, accountId = customer, amount = 100.0))
            val rootUid = db.eventDao().findById(id)!!.eventUid
            store.applyAgreement(CreditSaleAgreementEntity(rootUid, uid, "RECORDED", "CALENDAR_MONTHS", 1, "monthly"))
            rootUid
        }
        store.applyAcceptedRevision(revision("thirty-days", "monthly", CreditPaymentTerm(CreditTermKind.NET_DAYS, 30)))
        val agreement = db.creditMetadataDao().agreement(root)!!
        store.applyAgreement(agreement)
        assertEquals(CreditPaymentTerm.Monthly, (store.evidence(uid)[root] as CreditTermEvidence.Recorded).agreement.term)
        val other = CustomerManagementService(db).create("Other synthetic customer")
        val otherUid = db.accountDao().findById(other)!!.uid
        assertTrue(runCatching { store.applyAgreement(agreement.copy(customerUid = otherUid)) }.isFailure)
        assertTrue(runCatching { store.applyAgreement(agreement.copy(revisionUid = "missing")) }.isFailure)
        val before = db.eventDao().count()
        assertTrue(runCatching { db.withWriteTransaction {
            val id = db.eventDao().insert(EventEntity(createdAt = now, occurredAt = "2026-10-15", eventType = "SALE",
                channel = "credit-retail", saleQuantity = 1.0, saleValue = 100.0, unitRate = 100.0))
            db.financialLogDao().insert(FinancialLogEntity(eventId = id, accountId = customer, amount = 100.0))
            store.applyAgreement(agreement.copy(rootSaleUid = db.eventDao().findById(id)!!.eventUid, revisionUid = "missing"))
        } }.isFailure)
        assertEquals(before, db.eventDao().count())
    }

    @Test fun flagsKeepConcurrentSetsAndFailedMultiClearRollsBack() = runBlocking {
        ready()
        val service = CreditReviewService(db, security)
        service.setFlag(owner, uid, "First review", "set-a")
        service.setFlag(owner, uid, "First review", "set-a")
        service.setFlag(owner, uid, "Concurrent review", "set-b")
        service.clearFlag(owner, uid, setOf("set-a"), "Resolved first review", "clear-operation")
        service.clearFlag(owner, uid, setOf("set-a"), "Resolved first review", "clear-operation")
        val dao = db.creditMetadataDao()
        assertEquals(setOf("set-b"), CreditFlagReviewCalculator.calculate(uid, dao.actionsFor(uid).map { it.toAction() }).activeSetUids)
        assertEquals(3, dao.pending().size)
        val before = dao.actions()
        assertTrue(runCatching { service.clearFlag(owner, uid, setOf("set-b", "zz-missing"), "Reviewed", "bad-operation") }.isFailure)
        assertEquals(before, dao.actions())
        assertEquals(3, dao.pending().size)
        assertTrue(runCatching { service.setFlag(owner, uid, "Changed identity", "set-a") }.isFailure)
    }

    @Test fun encryptedRoundTripKeepsCreditHistoryAndHoldsPendingWithoutReenabling() = runBlocking {
        sale()
        ready()
        CreditMetadataStore(db).applyAcceptedRevision(revision("monthly"))
        db.withWriteTransaction {
            val eventId = db.eventDao().insert(EventEntity(createdAt = now, occurredAt = "2026-10-15", eventType = "SALE",
                channel = "credit-retail", saleQuantity = 1.0, saleValue = 100.0, unitRate = 100.0))
            db.financialLogDao().insert(FinancialLogEntity(eventId = eventId, accountId = customer, amount = 100.0))
            CreditMetadataStore(db).applyAgreement(CreditSaleAgreementEntity(db.eventDao().findById(eventId)!!.eventUid,
                uid, "RECORDED", "CALENDAR_MONTHS", 1, "monthly"))
        }
        val service = CreditReviewService(db, security)
        service.setFlag(owner, uid, "Review this account", "review-set")
        service.clearFlag(owner, uid, setOf("review-set"), "Reviewed", "review-clear")
        service.proposeTerms(owner, uid, "monthly", CreditPaymentTerm(CreditTermKind.NET_DAYS, 30), "pending-days")
        val manager = BackupManager(context)
        val file = File(context.cacheDir, "credit-storage-roundtrip.milkbackup")
        val target = MilkShopDatabase.createInMemory()
        val pass = "synthetic credit backup phrase".toCharArray()
        try {
            assertTrue(manager.exportSnapshot(db, Uri.fromFile(file)).isFailure)
            manager.exportEncryptedSnapshot(db, Uri.fromFile(file), pass).getOrThrow()
            manager.restoreEncrypted(target, Uri.fromFile(file), BackupManager.RestoreSelection(true, false, false, security = true), pass, NativeSessionManager()).getOrThrow()
            val source = db.creditMetadataDao(); val restored = target.creditMetadataDao()
            assertEquals(source.revisions(), restored.revisions()); assertEquals(source.heads(), restored.heads())
            assertEquals(source.agreements(), restored.agreements()); assertEquals(source.actions(), restored.actions())
            assertEquals(source.pending().map { it.copy(held = true) }, restored.pending())
            assertTrue(restored.state()!!.enabled)
            assertEquals(0, restored.state()!!.contractVersion)
            assertEquals(0, restored.state()!!.lastServerSeq)
            assertEquals(db.backupDataDao().financialLogs(), target.backupDataDao().financialLogs())
            assertTrue(runCatching { CreditReviewService(target, UserSecurityService(target)).setFlag("no-session", uid, "No") }.isFailure)
            val restoredSecurity = UserSecurityService(target)
            val restoredOwner = restoredSecurity.login("credit.storage.owner", PASSWORD.toCharArray()).sessionId
            assertTrue(runCatching { CreditReviewService(target, restoredSecurity).setFlag(restoredOwner, uid, "Still awaiting sync") }
                .exceptionOrNull() is IllegalStateException)
            assertTrue(runCatching { target.withWriteTransaction { executeSQL("DELETE FROM credit_review_actions") } }.isFailure)
        } finally { pass.fill('\u0000'); target.close(); file.delete() }
    }

    @Test fun malformedNewBackupFailsWithoutReplacingExistingBusinessAndOldBackupGetsExplicitLegacy() = runBlocking {
        val original = sale()
        val manager = BackupManager(context)
        val bytes = manager.buildCompleteSnapshotBytes(db)
        val bad = JSONObject(bytes.toString(Charsets.UTF_8))
        bad.getJSONObject("data").remove("credit_review_actions")
        val before = db.backupDataDao().financialLogs()
        assertTrue(manager.restoreCompleteSnapshotReplacing(db, signed(bad), NativeSessionManager(), "bad-credit").isFailure)
        assertEquals(before, db.backupDataDao().financialLogs())
        assertNotNull(db.eventDao().findById(original.eventId))
        val fractionalTerm = JSONObject(bytes.toString(Charsets.UTF_8))
        fractionalTerm.getJSONObject("data").getJSONArray("credit_sale_agreements").getJSONObject(0).put("term_value", 7.5)
        assertTrue(manager.restoreCompleteSnapshotReplacing(db, signed(fractionalTerm), NativeSessionManager(), "invalid-term").isFailure)
        assertEquals(before, db.backupDataDao().financialLogs())
        val old = JSONObject(bytes.toString(Charsets.UTF_8))
        old.getJSONObject("metadata").put("schema_version", 12)
        listOf("credit_term_revisions", "credit_term_heads", "credit_sale_agreements", "credit_review_actions", "credit_metadata_pending", "credit_metadata_sync_state")
            .forEach { old.getJSONObject("data").remove(it) }
        val target = MilkShopDatabase.createInMemory()
        try {
            manager.restoreCompleteSnapshotReplacing(target, signed(old), NativeSessionManager(), "legacy-credit").getOrThrow()
            assertEquals(db.creditMetadataDao().agreements(), target.creditMetadataDao().agreements())
            assertNull(target.creditMetadataDao().state())
        } finally { target.close() }
        val newer = JSONObject(bytes.toString(Charsets.UTF_8))
        newer.getJSONObject("metadata").put("schema_version", 14)
        assertTrue(runCatching { manager.inspectCompleteSnapshotBytes(signed(newer), "newer-credit") }.isFailure)
    }

    private fun signed(root: JSONObject): ByteArray {
        root.remove("integrity_sha256")
        root.put("integrity_sha256", sha256(root.toString().toByteArray(Charsets.UTF_8)))
        return root.toString().toByteArray(Charsets.UTF_8)
    }
    companion object { const val PASSWORD = "credit storage test password 2026" }
}

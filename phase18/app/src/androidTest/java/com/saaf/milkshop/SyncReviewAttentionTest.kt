package com.saaf.milkshop

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.owner.NativeOwnerSyncFacade
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.nativeui.owner.OwnerSyncAttention
import com.saaf.milkshop.nativeui.owner.reviewAttentionFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class SyncReviewAttentionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun hold(uid: String) = SyncRebaselineHoldEntity("products", uid, "{}", "other", 1, "2026-10-15T00:00:00Z")

    @Test fun committedHoldsEmitAndSurviveFileReopen() = runBlocking {
        val name = "review-attention-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder<MilkShopDatabase>(context, name)
            .setDriver(BundledSQLiteDriver()).build()
        var db = open()
        try {
            val emissions = Channel<Int>(Channel.UNLIMITED)
            val observing = launch { db.syncRebaselineHoldDao().observeCount().collect { emissions.send(it) } }
            assertEquals(0, withTimeout(5000) { emissions.receive() })
            db.syncRebaselineHoldDao().upsert(hold("first"))
            assertEquals(1, withTimeout(5000) { emissions.receive() })
            db.syncRebaselineHoldDao().upsert(hold("second"))
            assertEquals(2, withTimeout(5000) { emissions.receive() })
            db.syncRebaselineHoldDao().delete("products", "first")
            assertEquals(1, withTimeout(5000) { emissions.receive() })
            observing.cancelAndJoin()
            db.close()
            db = open()
            assertEquals(1, db.syncRebaselineHoldDao().observeCount().first())
            assertEquals("second", db.syncRebaselineHoldDao().observeAll().first().single().rowUid)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun outboxParkAndHistoricalConflictAreNotReviewHolds() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            assertTrue(db.syncOutboxDao().count() > 0)
            db.syncParkedRowDao().upsert(SyncParkedRowEntity("products", "park-only", "{}", 4, 0, "2026-10-15"))
            db.securityAuditDao().insert(SecurityAuditEntryEntity(occurredAt = "2026-10-15",
                action = "sync_apply_conflict", result = "warning", reasonCode = "sibling_planning_revision"))
            assertEquals(0, db.syncRebaselineHoldDao().observeCount().first())
        } finally { db.close() }
    }

    @Test fun readFailureRetainsCountAndCancellationIsNotFailure() = runBlocking {
        val states = reviewAttentionFlow(flow { emit(3); throw IllegalStateException("read failed") }).toList()
        assertEquals(listOf(3, 3), states.map { it.count })
        assertFalse(states.first().unavailable)
        assertTrue(states.last().unavailable)
        val fresh = reviewAttentionFlow(flow<Int> { throw IllegalStateException() }).first()
        assertNull(fresh.count)
        assertTrue(fresh.description.contains("unavailable"))
        val retried = reviewAttentionFlow(flowOf(0), states.last()).first()
        assertEquals(0, retried.count)
        assertFalse(retried.unavailable)
        try {
            reviewAttentionFlow(flow<Int> { throw CancellationException("session closed") }).toList()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals("1 entry needs review", OwnerSyncAttention(1).description)
    }

    @Test fun ownerReadWorksWithoutEnrollmentButExpiredAndCashierSessionsAreDenied() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            val sessions = NativeSessionManager()
            val security = UserSecurityService(db, sessions)
            val owner = security.bootstrapOwner("review.owner", "Review Owner", "review-owner-passphrase".toCharArray())
            val session = owner.session.sessionId
            val facade = NativeOwnerSyncFacade(context, db)
            db.syncRebaselineHoldDao().upsert(hold("owner-private"))
            assertEquals(1, facade.observeReviewCount(session, security).first())
            assertEquals("owner-private", facade.observeReviewHolds(session, security).first().single().rowUid)
            sessions.logout(session)
            assertTrue(runCatching { facade.observeReviewCount(session, security).first() }.isFailure)
            val cashierId = db.appUserDao().insert(AppUserEntity(loginName = "review.cashier",
                displayName = "Cashier", role = "cashier",
                createdAt = "2026-10-15", updatedAt = "2026-10-15"))
            val cashierSession = sessions.start(cashierId).sessionId
            assertTrue(runCatching { facade.observeReviewHolds(cashierSession, security).first() }.isFailure)
        } finally { db.close() }
    }
}

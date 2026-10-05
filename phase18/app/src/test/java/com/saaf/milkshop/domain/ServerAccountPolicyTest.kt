package com.saaf.milkshop.domain

import com.saaf.milkshop.data.ServerIdentityLinkEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline-grace policy frozen by the owner on 2026-09-25: 7 days from the
 * last successful verification, then writes blocked / reads allowed.
 */
class ServerAccountPolicyTest {

    private val dayMs = 24L * 60L * 60L * 1000L

    @Test
    fun `verified link blocks writes after exactly seven days`() {
        val verifiedAt = 1_000_000_000_000L
        val justInside = verifiedAt + ServerAccountPolicy.GRACE_DURATION_MS
        val justOutside = verifiedAt + ServerAccountPolicy.GRACE_DURATION_MS + 1

        assertFalse(ServerAccountPolicy.writeBlockedByGrace("verified", verifiedAt, verifiedAt))
        assertFalse(ServerAccountPolicy.writeBlockedByGrace("verified", verifiedAt, verifiedAt + 6 * dayMs))
        assertFalse(ServerAccountPolicy.writeBlockedByGrace("verified", verifiedAt, justInside))
        assertTrue(ServerAccountPolicy.writeBlockedByGrace("verified", verifiedAt, justOutside))
    }

    @Test
    fun `offline grace link expires on the same clock`() {
        val verifiedAt = 0L
        assertFalse(ServerAccountPolicy.writeBlockedByGrace("offline_grace", verifiedAt, ServerAccountPolicy.GRACE_DURATION_MS))
        assertTrue(ServerAccountPolicy.writeBlockedByGrace("offline_grace", verifiedAt, ServerAccountPolicy.GRACE_DURATION_MS + 1))
    }

    @Test
    fun `non-verified states never carry a grace clock`() {
        val now = 10_000_000_000L
        for (status in listOf("pending", "suspended", "revoked", "restored_unverified", "conflict")) {
            assertFalse(ServerAccountPolicy.writeBlockedByGrace(status, 0L, now))
        }
    }

    @Test
    fun `missing verification timestamp never blocks`() {
        assertFalse(ServerAccountPolicy.writeBlockedByGrace("verified", null, Long.MAX_VALUE / 2))
        assertFalse(ServerAccountPolicy.writeBlockedByGrace("offline_grace", null, Long.MAX_VALUE / 2))
    }

    @Test
    fun `grace is exactly seven days`() {
        assertTrue(ServerAccountPolicy.GRACE_DAYS == 7L)
        assertTrue(ServerAccountPolicy.GRACE_DURATION_MS == 7L * 24L * 60L * 60L * 1000L)
    }

    // ---------------- H5: reads survive grace expiry, writes do not ----------------

    private val expiredLink = ServerIdentityLinkEntity(
        localUserId = 1L,
        serverStaffId = "staff-1",
        businessId = "biz-1",
        status = ServerAccountPolicy.LinkStatus.OFFLINE_GRACE,
        verifiedAtEpochMs = 0L,
    )

    @Test
    fun `expired grace allows exactly the read family`() {
        val reads = setOf(
            AppPermission.VIEW_OPERATIONAL,
            AppPermission.VIEW_FULL_FINANCIAL,
            AppPermission.VIEW_SECURITY_AUDIT,
        )
        for (permission in AppPermission.entries) {
            val expected = if (permission in reads) {
                ServerAccountPolicy.GraceDecision.ALLOW
            } else {
                ServerAccountPolicy.GraceDecision.BLOCK_WRITE
            }
            assertEquals(expected, ServerAccountPolicy.graceDecision(permission, expired = true))
        }
    }

    @Test
    fun `unexpired grace allows everything`() {
        for (permission in AppPermission.entries) {
            assertEquals(
                ServerAccountPolicy.GraceDecision.ALLOW,
                ServerAccountPolicy.graceDecision(permission, expired = false),
            )
        }
    }

    @Test
    fun `expired link blocks writes and spares reads end-to-end`() {
        val now = 8L * 24L * 60L * 60L * 1000L // beyond the 7-day grace
        assertTrue(ServerAccountPolicy.graceWriteBlocked(expiredLink, AppPermission.EXPENSE, now))
        assertTrue(ServerAccountPolicy.graceWriteBlocked(expiredLink, AppPermission.CASH_COUNT_AND_CLOSE, now))
        assertTrue(ServerAccountPolicy.graceWriteBlocked(expiredLink, AppPermission.CREATE_EXPORT_BACKUP, now))
        assertTrue(ServerAccountPolicy.graceWriteBlocked(expiredLink, AppPermission.MANAGE_STAFF_USERS, now))
        assertFalse(ServerAccountPolicy.graceWriteBlocked(expiredLink, AppPermission.VIEW_OPERATIONAL, now))
        assertFalse(ServerAccountPolicy.graceWriteBlocked(expiredLink, AppPermission.VIEW_FULL_FINANCIAL, now))
        assertFalse(ServerAccountPolicy.graceWriteBlocked(expiredLink, AppPermission.VIEW_SECURITY_AUDIT, now))
    }

    @Test
    fun `verification refresh respects the throttle and clock-carrying statuses`() {
        val throttle = ServerAccountPolicy.VERIFICATION_REFRESH_THROTTLE_MS
        // fresh clock: no refresh
        assertFalse(ServerAccountPolicy.shouldRefreshVerification("verified", 0L, throttle - 1))
        // exactly at the window: refresh
        assertTrue(ServerAccountPolicy.shouldRefreshVerification("verified", 0L, throttle))
        // offline_grace carries the clock too (stale → refresh)
        assertTrue(ServerAccountPolicy.shouldRefreshVerification("offline_grace", 0L, throttle * 5))
        // non-clock-carrying statuses never refresh (enrollment links them)
        assertFalse(ServerAccountPolicy.shouldRefreshVerification("restored_unverified", 0L, throttle * 5))
        assertFalse(ServerAccountPolicy.shouldRefreshVerification("pending", 0L, throttle * 5))
        assertFalse(ServerAccountPolicy.shouldRefreshVerification("revoked", 0L, throttle * 5))
        // no timestamp: nothing to refresh
        assertFalse(ServerAccountPolicy.shouldRefreshVerification("verified", null, Long.MAX_VALUE / 2))
    }

    @Test
    fun `freshly verified link blocks nothing`() {
        val fresh = ServerIdentityLinkEntity(
            localUserId = 1L,
            serverStaffId = "staff-1",
            businessId = "biz-1",
            status = ServerAccountPolicy.LinkStatus.VERIFIED,
            verifiedAtEpochMs = 0L,
        )
        val now = 1L * 24L * 60L * 60L * 1000L // day 1 of 7
        for (permission in AppPermission.entries) {
            assertFalse(ServerAccountPolicy.graceWriteBlocked(fresh, permission, now))
        }
    }
}

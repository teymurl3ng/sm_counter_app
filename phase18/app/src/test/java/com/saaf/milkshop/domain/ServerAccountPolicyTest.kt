package com.saaf.milkshop.domain

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
}

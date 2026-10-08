package com.saaf.milkshop.domain

import com.saaf.milkshop.data.ServerIdentityLinkEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline-grace policy — DEGRADED MODE (2026-10-08 reform, Claude review):
 * 14 days from the enrollment binding's last verification; past that the
 * app enters degraded mode where DAILY ENTRY (sales, collections, cash
 * count/close, deliveries, purchases, expenses, payroll, production,
 * spoilage, attendance, corrections) keeps working and only privileged
 * actions block. SUSPENDED/REVOKED link states are recorded + displayed
 * only (ENFORCE_LINK_STATES = false until the guardrail set ships).
 */
class ServerAccountPolicyTest {

    private val dayMs = 24L * 60L * 60L * 1000L

    // ---------------- binding clock: the authoritative grace ----------------

    @Test
    fun `active binding degrades after exactly fourteen days`() {
        val verifiedAt = 1_000_000_000_000L
        val justInside = verifiedAt + ServerAccountPolicy.GRACE_DURATION_MS
        val justOutside = verifiedAt + ServerAccountPolicy.GRACE_DURATION_MS + 1

        assertFalse(ServerAccountPolicy.bindingGraceExpired("active", verifiedAt, verifiedAt))
        assertFalse(ServerAccountPolicy.bindingGraceExpired("active", verifiedAt, verifiedAt + 13 * dayMs))
        assertFalse(ServerAccountPolicy.bindingGraceExpired("active", verifiedAt, justInside))
        assertTrue(ServerAccountPolicy.bindingGraceExpired("active", verifiedAt, justOutside))
    }

    @Test
    fun `non-active bindings never carry a grace clock`() {
        val now = 10_000_000_000L
        for (status in listOf("pending", "suspended", "restored_unverified", "conflict")) {
            assertFalse(ServerAccountPolicy.bindingGraceExpired(status, 0L, now))
        }
    }

    @Test
    fun `missing verification timestamp never blocks`() {
        assertFalse(ServerAccountPolicy.bindingGraceExpired("active", null, Long.MAX_VALUE / 2))
    }

    @Test
    fun `grace is exactly fourteen days`() {
        assertEquals(14L, ServerAccountPolicy.GRACE_DAYS)
        assertEquals(14L * dayMs, ServerAccountPolicy.GRACE_DURATION_MS)
    }

    // ---------------- degraded mode: what survives expiry ----------------

    private val expired = true

    @Test
    fun `degraded mode keeps daily entry working`() {
        val operational = setOf(
            AppPermission.SALE_AND_COLLECTION,
            AppPermission.PURCHASE,
            AppPermission.SUPPLIER_PAYMENT,
            AppPermission.EXPENSE,
            AppPermission.SALARY_PAYMENT,
            AppPermission.PRODUCTION_AND_SPOILAGE,
            AppPermission.DELIVERY,
            AppPermission.ATTENDANCE,
            AppPermission.CORRECTION,
            AppPermission.CASH_COUNT_AND_CLOSE,
        )
        for (permission in operational) {
            assertEquals(
                "$permission must stay allowed in degraded mode",
                ServerAccountPolicy.GraceDecision.ALLOW,
                ServerAccountPolicy.graceDecision(permission, expired),
            )
        }
    }

    @Test
    fun `degraded mode blocks the privileged family`() {
        val privileged = setOf(
            AppPermission.RESTORE_BACKUP,
            AppPermission.MANAGE_STAFF_USERS,
            AppPermission.MANAGE_PRIVILEGED_USERS,
            AppPermission.MANAGE_BUSINESS_SETTINGS,
            AppPermission.CREATE_EXPORT_BACKUP,
            AppPermission.OWNER_DRAWING,
            AppPermission.REOPEN_DAY,
            AppPermission.MANAGE_MASTER_DATA,
        )
        for (permission in privileged) {
            assertEquals(
                "$permission must block in degraded mode",
                ServerAccountPolicy.GraceDecision.BLOCK_WRITE,
                ServerAccountPolicy.graceDecision(permission, expired),
            )
        }
    }

    @Test
    fun `degraded mode keeps every read alive`() {
        for (permission in AppPermission.entries.filter { it.name.startsWith("VIEW_") }) {
            assertEquals(
                ServerAccountPolicy.GraceDecision.ALLOW,
                ServerAccountPolicy.graceDecision(permission, expired),
            )
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
    fun `binding clock blocks privileged end-to-end at day fifteen`() {
        val now = 15L * dayMs
        assertTrue(
            ServerAccountPolicy.graceWriteBlockedBinding(
                "active", 0L, AppPermission.MANAGE_STAFF_USERS, now,
            ),
        )
        assertFalse(
            ServerAccountPolicy.graceWriteBlockedBinding(
                "active", 0L, AppPermission.SALE_AND_COLLECTION, now,
            ),
        )
        assertFalse(
            ServerAccountPolicy.graceWriteBlockedBinding(
                "active", 0L, AppPermission.CASH_COUNT_AND_CLOSE, now,
            ),
        )
        assertFalse(
            ServerAccountPolicy.graceWriteBlockedBinding(
                "active", 0L, AppPermission.VIEW_FULL_FINANCIAL, now,
            ),
        )
    }

    @Test
    fun `freshly active binding blocks nothing`() {
        val now = 1L * dayMs
        for (permission in AppPermission.entries) {
            assertFalse(
                ServerAccountPolicy.graceWriteBlockedBinding("active", 0L, permission, now),
            )
        }
    }

    // ---------------- warning ladder (proportional to the threshold) ----------------

    @Test
    fun `warning ladder rises through the grace window`() {
        val verifiedAt = 0L
        assertEquals(0, ServerAccountPolicy.graceWarningLevel(verifiedAt, 1L * dayMs))
        assertEquals(1, ServerAccountPolicy.graceWarningLevel(verifiedAt, 4L * dayMs))
        assertEquals(2, ServerAccountPolicy.graceWarningLevel(verifiedAt, 8L * dayMs))
        assertEquals(3, ServerAccountPolicy.graceWarningLevel(verifiedAt, 13L * dayMs))
        assertEquals(3, ServerAccountPolicy.graceWarningLevel(verifiedAt, 20L * dayMs))
        assertEquals(0, ServerAccountPolicy.graceWarningLevel(null, 20L * dayMs))
    }

    // ---------------- link-clock variant (display/record flows) ----------------

    @Test
    fun `link clock follows the same rules`() {
        val link = ServerIdentityLinkEntity(
            localUserId = 1L,
            serverStaffId = "staff-1",
            businessId = "biz-1",
            status = ServerAccountPolicy.LinkStatus.VERIFIED,
            verifiedAtEpochMs = 0L,
        )
        assertFalse(ServerAccountPolicy.graceWriteBlocked(link, AppPermission.SALE_AND_COLLECTION, 13L * dayMs))
        assertTrue(ServerAccountPolicy.graceWriteBlocked(link, AppPermission.MANAGE_STAFF_USERS, 15L * dayMs))
        assertFalse(ServerAccountPolicy.graceWriteBlocked(link, AppPermission.SALE_AND_COLLECTION, 15L * dayMs))
    }

    // ---------------- H13 refresh (link + binding variants) ----------------

    @Test
    fun `verification refresh respects the throttle on both clocks`() {
        val throttle = ServerAccountPolicy.VERIFICATION_REFRESH_THROTTLE_MS
        assertTrue(ServerAccountPolicy.shouldRefreshVerification("verified", 0L, throttle))
        assertFalse(ServerAccountPolicy.shouldRefreshVerification("restored_unverified", 0L, throttle * 5))
        assertTrue(ServerAccountPolicy.shouldRefreshBindingVerification("active", 0L, throttle))
        assertFalse(ServerAccountPolicy.shouldRefreshBindingVerification("restored_unverified", 0L, throttle * 5))
        assertFalse(ServerAccountPolicy.shouldRefreshBindingVerification("active", null, throttle * 5))
    }

    // ---------------- A1': link states are display-only until the flag ships ----------------

    @Test
    fun `link state enforcement ships off`() {
        assertFalse(ServerAccountPolicy.ENFORCE_LINK_STATES)
        for (status in listOf("suspended", "revoked")) {
            assertFalse(ServerAccountPolicy.linkStateBlocked(status, expired = true))
        }
    }
}

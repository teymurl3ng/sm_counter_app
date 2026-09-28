package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.sync.BusinessClock
import com.saaf.milkshop.sync.BusinessDateGuard
import com.saaf.milkshop.sync.EventTimeTrust
import com.saaf.milkshop.sync.TimeTrustMonitor
import com.saaf.milkshop.sync.TrustedTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Phase 2: trusted-time engine. Wrong time is flagged and bounded — never a
 * blocked entry (D9): while unanchored the app behaves exactly as before,
 * and anchoring only ever adds flags, estimates, and a future-date bound.
 */
@RunWith(AndroidJUnit4::class)
class TrustedTimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun unanchoredDevice_fallsBackToDeviceClockWithNoBounds() {
        TrustedTime.resetForTest(context)
        try {
            assertFalse(TrustedTime.isAnchored())
            assertNull(TrustedTime.trustedNowEpochMs())
            assertEquals(LocalDate.now(), BusinessClock.today())
            assertEquals(EventTimeTrust.DEVICE_ONLY, EventTimeTrust.flagNow())
            assertEquals("", EventTimeTrust.trustedEstimateNow())
            // Unanchored: no future bound applies — entries never depend on time sync.
            BusinessDateGuard.requireHappenedWithinHorizon(LocalDate.now().plusDays(30).toString())
            BusinessDateGuard.requirePlannedWithinHorizon(LocalDate.now().plusDays(30).toString())
        } finally {
            TrustedTime.resetForTest(context)
        }
    }

    @Test
    fun anchoredClock_derivesTodayAndStampsAnchored() {
        TrustedTime.resetForTest(context)
        try {
            TrustedTime.warmUp(context)
            TrustedTime.anchor(System.currentTimeMillis())
            assertTrue(TrustedTime.isAnchored())
            val trusted = TrustedTime.trustedNowEpochMs()!!
            assertTrue(Math.abs(trusted - System.currentTimeMillis()) < 5_000)
            val expectedToday = Instant.ofEpochMilli(trusted)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            assertEquals(expectedToday, BusinessClock.today())
            assertEquals(EventTimeTrust.ANCHORED, EventTimeTrust.flagNow())
            assertTrue(EventTimeTrust.trustedEstimateNow().isNotEmpty())
        } finally {
            TrustedTime.resetForTest(context)
        }
    }

    @Test
    fun divergedClock_flagsRows_andBoundsOnlyFarFutureDates() {
        TrustedTime.resetForTest(context)
        try {
            TrustedTime.warmUp(context)
            TrustedTime.anchor(System.currentTimeMillis() + 2 * 60 * 60 * 1000L)
            assertEquals(EventTimeTrust.CLOCK_DIVERGED, EventTimeTrust.flagNow())
            val trustedToday = BusinessClock.today()

            // Happened-on dates: one day ahead is fine, two is not.
            BusinessDateGuard.requireHappenedWithinHorizon(trustedToday.plusDays(1).toString())
            assertTrue(rejected { BusinessDateGuard.requireHappenedWithinHorizon(trustedToday.plusDays(2).toString()) })

            // Planned deliveries get a week.
            BusinessDateGuard.requirePlannedWithinHorizon(trustedToday.plusDays(7).toString())
            assertTrue(rejected { BusinessDateGuard.requirePlannedWithinHorizon(trustedToday.plusDays(8).toString()) })

            // Deliberate backdating to an open day always stays allowed.
            BusinessDateGuard.requireHappenedWithinHorizon(trustedToday.minusDays(30).toString())
        } finally {
            TrustedTime.resetForTest(context)
        }
    }

    @Test
    fun reboot_invalidatesAnchorUntilNextServerContact() {
        TrustedTime.resetForTest(context)
        try {
            TrustedTime.warmUp(context)
            TrustedTime.anchor(System.currentTimeMillis())
            assertTrue(TrustedTime.isAnchored())

            // Simulate a reboot by aging the recorded boot count, then reload.
            val file = File(File(context.noBackupFilesDir, "sync"), "trusted_time_anchor.txt")
            val parts = file.readText().trim().split('\n')
            file.writeText("${parts[0].toInt() + 99}\n${parts[1]}\n${parts[2]}")
            TrustedTime.warmUp(context)

            assertFalse(TrustedTime.isAnchored())
            assertEquals(EventTimeTrust.DEVICE_ONLY, EventTimeTrust.flagNow())
        } finally {
            TrustedTime.resetForTest(context)
        }
    }

    @Test
    fun skewBeyondThreshold_writesRateLimitedAuditEntry() = runBlocking {
        TrustedTime.resetForTest(context)
        TimeTrustMonitor.resetForTest()
        val database = MilkShopDatabase.createInMemory()
        try {
            TrustedTime.warmUp(context)
            TrustedTime.anchor(System.currentTimeMillis() - 2 * 60 * 60 * 1000L)

            TimeTrustMonitor.check(context, database)
            val entries = database.backupDataDao().securityAuditEntries()
                .filter { it.action == TimeTrustMonitor.ACTION_TIME_TRUST }
            assertEquals(1, entries.size)
            assertEquals(TimeTrustMonitor.RESULT_ALERT, entries.single().result)
            assertEquals(TimeTrustMonitor.REASON_CLOCK_SKEW, entries.single().reasonCode)

            // Rate limit: an immediate re-check adds nothing.
            TimeTrustMonitor.check(context, database)
            assertEquals(
                1,
                database.backupDataDao().securityAuditEntries()
                    .count { it.action == TimeTrustMonitor.ACTION_TIME_TRUST },
            )
        } finally {
            database.close()
            TrustedTime.resetForTest(context)
        }
    }

    @Test
    fun shopZone_persistsAcrossReloads() {
        TrustedTime.resetForTest(context)
        try {
            TrustedTime.warmUp(context)
            BusinessClock.warmUp(context)
            BusinessClock.setZone("Asia/Karachi")
            assertEquals("Asia/Karachi", BusinessClock.currentZone().id)
            BusinessClock.warmUp(context)
            assertEquals("Asia/Karachi", BusinessClock.currentZone().id)
        } finally {
            BusinessClock.setZone(ZoneId.systemDefault().id)
            TrustedTime.resetForTest(context)
        }
    }

    private fun rejected(block: () -> Unit): Boolean = try {
        block()
        false
    } catch (_: IllegalArgumentException) {
        true
    }
}

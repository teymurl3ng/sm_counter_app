package com.saaf.milkshop.sync

import com.saaf.milkshop.data.SyncOutboxEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for the go-8 classified re-baseline decisions (audit rounds 2-3).
 * The pull-0 pass observes every server row's origin; each restored outbox
 * entry is classified: keep (server lacks it), drop (own previous or current
 * origin — server already holds a later or equal version), or hold (foreign
 * origin — probably doubly stale, owner rules).
 */
class SyncRebaselineDecisionsTest {

    private fun entry(table: String, uid: String) =
        SyncOutboxEntity(tableName = table, rowUid = uid, enqueuedAt = "2026-10-03T10:00:00.000Z")

    private val PREV = "prevdevice0000000000000000000000"
    private val CURRENT = "currentdevice00000000000000000000"
    private val FOREIGN = "foreigndevice00000000000000000000"

    @Test
    fun serverLacksRow_keep() {
        val decisions = SyncRebaseline.decide(
            entries = listOf(entry("events", "u1_new_event")),
            seen = emptyMap(),
            prevDeviceId = PREV,
            currentDeviceId = CURRENT,
        )
        assertEquals(SyncRebaseline.Action.KEEP, decisions.single().action)
    }

    @Test
    fun ownPreviousOrigin_drop() {
        // The v2-after-backup case: this installation pushed a later version
        // than its backup held — the restored entry must drop, server wins.
        val decisions = SyncRebaseline.decide(
            entries = listOf(entry("products", "u1_seed_products_Milk")),
            seen = mapOf(
                SyncRebaseline.seenKey("products", "u1_seed_products_Milk") to
                    SyncRebaseline.SeenServerRow(originDevice = PREV, serverSeq = 40),
            ),
            prevDeviceId = PREV,
            currentDeviceId = CURRENT,
        )
        assertEquals(SyncRebaseline.Action.DROP_OWN_ORIGIN, decisions.single().action)
    }

    @Test
    fun currentOrigin_drop() {
        val decisions = SyncRebaseline.decide(
            entries = listOf(entry("settings", "setting:milk_rate_vdr")),
            seen = mapOf(
                SyncRebaseline.seenKey("settings", "setting:milk_rate_vdr") to
                    SyncRebaseline.SeenServerRow(originDevice = CURRENT, serverSeq = 41),
            ),
            prevDeviceId = PREV,
            currentDeviceId = CURRENT,
        )
        assertEquals(SyncRebaseline.Action.DROP_CURRENT_ORIGIN, decisions.single().action)
    }

    @Test
    fun foreignOrigin_hold() {
        val decisions = SyncRebaseline.decide(
            entries = listOf(entry("deliveries", "u1_del_1")),
            seen = mapOf(
                SyncRebaseline.seenKey("deliveries", "u1_del_1") to
                    SyncRebaseline.SeenServerRow(originDevice = FOREIGN, serverSeq = 42),
            ),
            prevDeviceId = PREV,
            currentDeviceId = CURRENT,
        )
        assertEquals(SyncRebaseline.Action.HOLD_FOREIGN_ORIGIN, decisions.single().action)
    }

    @Test
    fun nullOrigin_holds_safest() {
        val decisions = SyncRebaseline.decide(
            entries = listOf(entry("accounts", "u1_acc_1")),
            seen = mapOf(
                SyncRebaseline.seenKey("accounts", "u1_acc_1") to
                    SyncRebaseline.SeenServerRow(originDevice = null, serverSeq = 43),
            ),
            prevDeviceId = PREV,
            currentDeviceId = CURRENT,
        )
        assertEquals(SyncRebaseline.Action.HOLD_FOREIGN_ORIGIN, decisions.single().action)
    }

    @Test
    fun nullPrevDeviceId_neverMatchesOwnOrigin() {
        // A format-4-or-older restore carries no prev id: nothing can claim
        // own-origin, so rows fall to keep/hold only.
        val decisions = SyncRebaseline.decide(
            entries = listOf(
                entry("events", "u1_new_event"),
                entry("products", "u1_seed_products_Milk"),
            ),
            seen = mapOf(
                SyncRebaseline.seenKey("products", "u1_seed_products_Milk") to
                    SyncRebaseline.SeenServerRow(originDevice = FOREIGN, serverSeq = 44),
            ),
            prevDeviceId = null,
            currentDeviceId = CURRENT,
        )
        assertEquals(SyncRebaseline.Action.KEEP, decisions[0].action)
        assertEquals(SyncRebaseline.Action.HOLD_FOREIGN_ORIGIN, decisions[1].action)
    }

    @Test
    fun mixedBatch_eachEntryClassifiedIndependently() {
        val entries = listOf(
            entry("events", "u1_new_event"),
            entry("products", "u1_seed_products_Milk"),
            entry("accounts", "u1_acc_1"),
            entry("deliveries", "u1_del_1"),
        )
        val seen = mapOf(
            SyncRebaseline.seenKey("products", "u1_seed_products_Milk") to
                SyncRebaseline.SeenServerRow(PREV, 40),
            SyncRebaseline.seenKey("accounts", "u1_acc_1") to
                SyncRebaseline.SeenServerRow(CURRENT, 41),
            SyncRebaseline.seenKey("deliveries", "u1_del_1") to
                SyncRebaseline.SeenServerRow(FOREIGN, 42),
        )
        val actions = SyncRebaseline.decide(entries, seen, PREV, CURRENT).map { it.action }
        assertEquals(
            listOf(
                SyncRebaseline.Action.KEEP,
                SyncRebaseline.Action.DROP_OWN_ORIGIN,
                SyncRebaseline.Action.DROP_CURRENT_ORIGIN,
                SyncRebaseline.Action.HOLD_FOREIGN_ORIGIN,
            ),
            actions,
        )
    }
}

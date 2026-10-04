package com.saaf.milkshop.sync

import com.saaf.milkshop.data.SyncOutboxEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the go-8 classified re-baseline decisions (audit rounds 2-3).
 * The pull-0 pass observes every server row's origin; each restored outbox
 * entry is classified: keep (server lacks it), drop (own previous or current
 * origin — server already holds a later or equal version), or hold (foreign
 * origin — probably doubly stale, owner rules).
 *
 * The second half covers the option-Z LIVE re-baseline (2026-10-04): a plain
 * enrollment's held entries classify against server row BODIES — release
 * what the server lacks, drop proven-equal rows, hold differing rows.
 */
class SyncRebaselineDecisionsTest {

    private fun entry(table: String, uid: String) =
        SyncOutboxEntity(tableName = table, rowUid = uid, enqueuedAt = "2026-10-03T10:00:00.000Z")

    private fun heldEntry(table: String, uid: String) =
        SyncOutboxEntity(tableName = table, rowUid = uid, enqueuedAt = "2026-10-03T10:00:00.000Z", held = true)

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

    // ---------------- option-Z live re-baseline ----------------

    private val SERVER = "serverdevice000000000000000000000000"

    @Test
    fun live_serverLacks_releases() {
        val decisions = SyncRebaseline.decideLive(
            entries = listOf(heldEntry("events", "u1_new_event")),
            devicePayloads = emptyMap(),
            seen = emptyMap(),
        )
        assertEquals(SyncRebaseline.LiveAction.RELEASE, decisions.single().action)
    }

    @Test
    fun live_equalIgnoringSyncUpdatedAt_drops() {
        // sync_updated_at is first-touch transport metadata (server
        // normalize_payload semantics) — never a content difference.
        val key = SyncRebaseline.seenKey("products", "u1_seed_products_Milk")
        val decisions = SyncRebaseline.decideLive(
            entries = listOf(heldEntry("products", "u1_seed_products_Milk")),
            devicePayloads = mapOf(
                key to JSONObject(
                    "{\"uid\":\"u1_seed_products_Milk\",\"name\":\"Milk\"," +
                        "\"sync_updated_at\":\"2026-01-01T00:00:00Z\"}",
                ),
            ),
            seen = mapOf(
                key to SyncRebaseline.SeenServerRow(
                    SERVER,
                    50,
                    JSONObject(
                        "{\"uid\":\"u1_seed_products_Milk\",\"name\":\"Milk\"," +
                            "\"sync_updated_at\":\"2026-10-04T09:00:00Z\"}",
                    ),
                ),
            ),
        )
        assertEquals(SyncRebaseline.LiveAction.DROP_EQUAL, decisions.single().action)
    }

    @Test
    fun live_contentDiffers_holds() {
        val key = SyncRebaseline.seenKey("products", "u1_seed_products_Milk")
        val decisions = SyncRebaseline.decideLive(
            entries = listOf(heldEntry("products", "u1_seed_products_Milk")),
            devicePayloads = mapOf(key to JSONObject("{\"uid\":\"u1_seed_products_Milk\",\"name\":\"Milk\"}")),
            seen = mapOf(
                key to SyncRebaseline.SeenServerRow(
                    SERVER,
                    51,
                    JSONObject("{\"uid\":\"u1_seed_products_Milk\",\"name\":\"Milk (server edit)\"}"),
                ),
            ),
        )
        assertEquals(SyncRebaseline.LiveAction.HOLD_DIFFER, decisions.single().action)
    }

    @Test
    fun live_pendingEntriesNeverClassified() {
        // Pre-enrollment pending edits push untouched (direction rule) —
        // they must not appear in the live decisions at all.
        val decisions = SyncRebaseline.decideLive(
            entries = listOf(entry("accounts", "u1_acc_1")),
            devicePayloads = emptyMap(),
            seen = emptyMap(),
        )
        assertTrue(decisions.isEmpty())
    }

    @Test
    fun live_missingPayloads_holdDefensively() {
        // Equality must be PROVEN to skip a push: a server row without a
        // captured body, or a held entry whose envelope could not be built,
        // holds for owner review instead of guessing.
        val key = SyncRebaseline.seenKey("settings", "setting:milk_rate_vdr")
        val decisions = SyncRebaseline.decideLive(
            entries = listOf(
                heldEntry("settings", "setting:milk_rate_vdr"),
                heldEntry("accounts", "u1_acc_1"),
            ),
            devicePayloads = emptyMap(),
            seen = mapOf(
                key to SyncRebaseline.SeenServerRow(SERVER, 52, JSONObject("{\"value\":1}")),
                SyncRebaseline.seenKey("accounts", "u1_acc_1") to
                    SyncRebaseline.SeenServerRow(SERVER, 53, null),
            ),
        )
        assertEquals(SyncRebaseline.LiveAction.HOLD_DIFFER, decisions[0].action)
        assertEquals(SyncRebaseline.LiveAction.HOLD_DIFFER, decisions[1].action)
    }
}

package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity
import com.saaf.milkshop.data.SyncRebaselineHoldEntity
import org.json.JSONObject
import org.json.JSONArray
import java.time.Instant

/**
 * go 8 classified re-baseline (audit rounds 2-3): after a format-5 restore
 * the next enrollment skips enqueueAll and the FIRST drain runs pull-first
 * with origin collection. Every pulled record's (table, uid) -> origin is
 * gathered during the pull-0 pass (local tables do not store origin), then
 * each restored outbox entry is classified against the server row:
 *
 *  - server lacks the row .......... KEEP   (new local work; pushes as new)
 *  - origin == prev_device_id ...... DROP   (this installation already pushed
 *                                            a later version — the
 *                                            v2-after-backup case; the park
 *                                            unblocks and the server wins)
 *  - origin == current device_id ... DROP   (already-pushed residue; no-op)
 *  - origin is foreign or null ..... HOLD   (probably doubly stale — never
 *                                            auto-push; owner rules in the
 *                                            sync-review surface)
 *
 * The hold KEEPS the outbox entry (flagged held=1, excluded from pushes) so
 * the pending-edit guard and the park stay active: the local row keeps its
 * backup-era values until the owner decides. keep-mine clears the flag (the
 * entry pushes, audited — origin differs); take-server deletes the entry
 * (the park unblocks and the server version applies).
 */
object SyncRebaseline {

    /** A server row observed during the re-baseline pull-0 pass. [payload]
     * is captured only for the live re-baseline — the equality decision
     * needs the server's row body, not just its origin. */
    data class SeenServerRow(
        val originDevice: String?,
        val serverSeq: Long,
        val payload: JSONObject? = null,
    )

    enum class Action { KEEP, DROP_OWN_ORIGIN, DROP_CURRENT_ORIGIN, HOLD_FOREIGN_ORIGIN }

    data class Decision(
        val entry: SyncOutboxEntity,
        val action: Action,
        val seen: SeenServerRow?,
    )

    data class Report(
        val kept: Int,
        val droppedOwn: Int,
        val droppedCurrent: Int,
        val heldForeign: Int,
    ) {
        fun describe(): String =
            "rebaseline: kept=$kept droppedOwn=$droppedOwn droppedCurrent=$droppedCurrent heldForeign=$heldForeign"
    }

    fun seenKey(tableName: String, rowUid: String): String = "$tableName|$rowUid"

    /**
     * Pure decision pass — JVM-tested. `prevDeviceId` may be null (backup
     * predates format 5's device_id metadata): such entries can never match
     * own-origin, so they fall to KEEP or HOLD by the seen map alone.
     */
    fun decide(
        entries: List<SyncOutboxEntity>,
        seen: Map<String, SeenServerRow>,
        prevDeviceId: String?,
        currentDeviceId: String?,
    ): List<Decision> = entries.map { entry ->
        val row = seen[seenKey(entry.tableName, entry.rowUid)]
        val action = when {
            row == null -> Action.KEEP
            prevDeviceId != null && row.originDevice == prevDeviceId -> Action.DROP_OWN_ORIGIN
            currentDeviceId != null && row.originDevice == currentDeviceId -> Action.DROP_CURRENT_ORIGIN
            else -> Action.HOLD_FOREIGN_ORIGIN
        }
        Decision(entry, action, row)
    }

    /**
     * Applies the decisions. MUST run after the pull-0 pass completed (the
     * pending-edit guard has parked every classified uid's server version by
     * then). Payload snapshots for holds come from the same envelope factory
     * the push uses — display material for the review surface only.
     */
    suspend fun classifyAndApply(
        database: MilkShopDatabase,
        seen: Map<String, SeenServerRow>,
        prevDeviceId: String?,
        currentDeviceId: String?,
    ): Report {
        val entries = database.syncOutboxDao().all()
        val decisions = decide(entries, seen, prevDeviceId, currentDeviceId)
        var kept = 0
        var droppedOwn = 0
        var droppedCurrent = 0
        var heldForeign = 0

        val holdDecisions = decisions.filter { it.action == Action.HOLD_FOREIGN_ORIGIN }
        val snapshots = if (holdDecisions.isEmpty()) {
            emptyMap()
        } else {
            SyncEnvelopeFactory(database)
                .build(holdDecisions.map { it.entry })
                .envelopes
                .associateBy { seenKey(it.optString("table"), it.optString("uid")) }
        }

        for (decision in decisions) {
            val entry = decision.entry
            when (decision.action) {
                Action.KEEP -> {
                    database.syncOutboxDao().setHeld(entry.tableName, entry.rowUid, false)
                    database.syncRebaselineHoldDao().delete(entry.tableName, entry.rowUid)
                    kept += 1
                }
                Action.DROP_OWN_ORIGIN -> {
                    database.syncOutboxDao().deleteEntry(entry.tableName, entry.rowUid)
                    database.syncRebaselineHoldDao().delete(entry.tableName, entry.rowUid)
                    droppedOwn += 1
                }
                Action.DROP_CURRENT_ORIGIN -> {
                    database.syncOutboxDao().deleteEntry(entry.tableName, entry.rowUid)
                    database.syncRebaselineHoldDao().delete(entry.tableName, entry.rowUid)
                    droppedCurrent += 1
                }
                Action.HOLD_FOREIGN_ORIGIN -> {
                    database.syncOutboxDao().setHeld(entry.tableName, entry.rowUid, true)
                    database.syncRebaselineHoldDao().upsert(
                        SyncRebaselineHoldEntity(
                            tableName = entry.tableName,
                            rowUid = entry.rowUid,
                            payloadJson = snapshots[seenKey(entry.tableName, entry.rowUid)]
                                ?.optJSONObject("payload")?.toString() ?: "",
                            originDevice = decision.seen?.originDevice,
                            serverSeq = decision.seen?.serverSeq ?: 0L,
                            heldAt = Instant.now().toString(),
                        ),
                    )
                    heldForeign += 1
                }
            }
        }
        return Report(kept, droppedOwn, droppedCurrent, heldForeign)
    }

    // ------------------------------------------------------------------
    // Live re-baseline (option Z decision, 2026-10-04): a PLAIN enrollment
    // enqueues the whole local history pre-held, and the first drain
    // classifies each held entry against the server row BODY, not its
    // origin (after a server rollback the device may be the newer party —
    // the restore path's origin rule points the wrong way there):
    //
    //  - server lacks the uid .............. RELEASE (new local work; pushes)
    //  - payloads equal (ignoring
    //    sync_updated_at) .................. DROP_EQUAL (in sync; nothing
    //                                        pushes, no seq burn, the park
    //                                        self-clears on the next apply)
    //  - payloads differ ................... HOLD_DIFFER (ambiguous
    //                                        direction — owner rules in the
    //                                        review surface instead of either
    //                                        side silently clobbering the
    //                                        other)
    //
    // Entries WITHOUT the held flag (pending edits captured before the
    // enrollment) are excluded here: they push untouched, per the direction
    // rule. Restore-origin enrollment keeps the origin classification above
    // — the device is provably stale there, so the server wins.
    enum class LiveAction { RELEASE, DROP_EQUAL, HOLD_DIFFER }

    data class LiveDecision(
        val entry: SyncOutboxEntity,
        val action: LiveAction,
        val seen: SeenServerRow?,
    )

    data class LiveReport(
        val released: Int,
        val droppedEqual: Int,
        val heldDiffer: Int,
    ) {
        fun describe(): String =
            "live_rebaseline: released=$released noopEqual=$droppedEqual heldDiffer=$heldDiffer"
    }

    /**
     * Pure live classification — JVM-tested. Only HELD entries decide;
     * equality must be PROVEN to skip a push, so a missing device or server
     * payload holds defensively.
     */
    fun decideLive(
        entries: List<SyncOutboxEntity>,
        devicePayloads: Map<String, JSONObject>,
        seen: Map<String, SeenServerRow>,
    ): List<LiveDecision> = entries.filter { it.held }.map { entry ->
        val key = seenKey(entry.tableName, entry.rowUid)
        val row = seen[key]
        val action = when {
            row == null -> LiveAction.RELEASE
            row.payload == null || !devicePayloads.containsKey(key) -> LiveAction.HOLD_DIFFER
            livePayloadsEqual(devicePayloads.getValue(key), row.payload) -> LiveAction.DROP_EQUAL
            else -> LiveAction.HOLD_DIFFER
        }
        LiveDecision(entry, action, row)
    }

    /**
     * Server normalize_payload semantics mirrored client-side:
     * sync_updated_at is first-touch transport metadata, never a content
     * difference.
     */
    fun livePayloadsEqual(device: JSONObject, server: JSONObject): Boolean =
        jsonStructurallyEqual(canonicalPayload(device), canonicalPayload(server))

    private val localIdentifiers = setOf(
        "product_id", "account_id", "event_id", "log_id", "budget_id", "target_id",
        "attendance_id", "employee_account_id", "supersedes_id", "delivery_id",
        "customer_account_id", "sale_event_id", "status_id", "count_id", "store_profile_id",
        "action_id", "actor_user_id", "corrects_event_id", "physical_cash_count_id",
    )

    /** UIDs and business values survive remapping; integer row IDs do not. */
    private fun canonicalPayload(value: JSONObject): JSONObject = JSONObject().apply {
        value.keys().asSequence().sorted().forEach { key ->
            if (key !in localIdentifiers && key != "sync_updated_at") {
                val item = value.get(key)
                put(key, when (item) {
                    is JSONObject -> canonicalPayload(item)
                    is JSONArray -> JSONArray((0 until item.length()).map { index ->
                        val child = item.get(index)
                        if (child is JSONObject) canonicalPayload(child) else child
                    }.sortedBy { it.toString() })
                    else -> item
                })
            }
        }
    }

    /**
     * Applies the live decisions. MUST run after the pull-0 pass completed —
     * every local uid had a held entry then, so the pending-edit guard
     * parked each server version and the local rows still carry the
     * pre-enroll values the device payloads are built from.
     */
    suspend fun classifyLiveAndApply(
        database: MilkShopDatabase,
        seen: Map<String, SeenServerRow>,
    ): LiveReport {
        val entries = database.syncOutboxDao().all().filter { it.held }
        if (entries.isEmpty()) return LiveReport(0, 0, 0)
        val devicePayloads = SyncEnvelopeFactory(database)
            .build(entries)
            .envelopes
            .mapNotNull { envelope ->
                envelope.optJSONObject("payload")?.let {
                    seenKey(envelope.optString("table"), envelope.optString("uid")) to it
                }
            }
            .toMap()
        val decisions = decideLive(entries, devicePayloads, seen)
        var released = 0
        var droppedEqual = 0
        var heldDiffer = 0
        for (decision in decisions) {
            val entry = decision.entry
            when (decision.action) {
                LiveAction.RELEASE -> {
                    database.syncOutboxDao().setHeld(entry.tableName, entry.rowUid, false)
                    // A stale hold row from an earlier classification round
                    // must not outlive its entry (ghost in the review UI).
                    database.syncRebaselineHoldDao().delete(entry.tableName, entry.rowUid)
                    released += 1
                }
                LiveAction.DROP_EQUAL -> {
                    database.syncOutboxDao().deleteEntry(entry.tableName, entry.rowUid)
                    database.syncRebaselineHoldDao().delete(entry.tableName, entry.rowUid)
                    droppedEqual += 1
                }
                LiveAction.HOLD_DIFFER -> {
                    database.syncRebaselineHoldDao().upsert(
                        SyncRebaselineHoldEntity(
                            tableName = entry.tableName,
                            rowUid = entry.rowUid,
                            payloadJson = devicePayloads[seenKey(entry.tableName, entry.rowUid)]
                                ?.toString() ?: "",
                            originDevice = decision.seen?.originDevice,
                            serverSeq = decision.seen?.serverSeq ?: 0L,
                            heldAt = Instant.now().toString(),
                        ),
                    )
                    heldDiffer += 1
                }
            }
        }
        return LiveReport(released, droppedEqual, heldDiffer)
    }
}

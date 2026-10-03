package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity
import com.saaf.milkshop.data.SyncRebaselineHoldEntity
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

    /** A server row observed during the re-baseline pull-0 pass. */
    data class SeenServerRow(
        val originDevice: String?,
        val serverSeq: Long,
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
                Action.KEEP -> kept += 1
                Action.DROP_OWN_ORIGIN -> {
                    database.syncOutboxDao().deleteEntry(entry.tableName, entry.rowUid)
                    droppedOwn += 1
                }
                Action.DROP_CURRENT_ORIGIN -> {
                    database.syncOutboxDao().deleteEntry(entry.tableName, entry.rowUid)
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
}

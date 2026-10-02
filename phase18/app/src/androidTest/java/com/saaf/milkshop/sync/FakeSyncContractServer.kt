package com.saaf.milkshop.sync

import org.json.JSONObject

/**
 * C1 test double: an in-memory server implementing the A′ sync contract the
 * SQL migration codifies (SYNC_GLOBAL_SEQ_C1_MIGRATION.md §3/§4) —
 *  - records keyed (table, uid); a push to an EXISTING row bumps its seq
 *    (updates propagate — the C1 fix);
 *  - append-only tables are insert-only: pushes of differing payloads for an
 *    existing append-only uid are recorded as refusals, never applied;
 *  - true-conflict audit: overwriting a version the pusher had not pulled
 *    (existing seq > puller cursor) with a DIFFERENT payload and different
 *    origin records the loser's payload;
 *  - pull delivers rows with seq > since, ordered by seq.
 * Single-threaded on purpose: determinism over realism (the advisory-lock
 * commit-ordering property is exercised in the SQL rehearsal, go 5).
 */
class FakeSyncContractServer {

    class Row(
        val table: String,
        val uid: String,
        var payload: JSONObject,
        var originDevice: String?,
        var seq: Long,
    )

    class Conflict(
        val table: String,
        val uid: String,
        val loserPayload: JSONObject,
        val loserDevice: String?,
        val winnerDevice: String?,
        val detectedBy: String,
    )

    val rows = LinkedHashMap<String, Row>() // key "table|uid" -> row
    val conflicts = mutableListOf<Conflict>()
    val appendOnlyRefusals = mutableListOf<String>()
    private val seqCounter = java.util.concurrent.atomic.AtomicLong(0)

    fun nextSeq(): Long = seqCounter.incrementAndGet()

    fun syncPush(
        envelopes: List<JSONObject>,
        deviceId: String?,
        pullCursor: Long,
    ): Int {
        var applied = 0
        for (env in envelopes) {
            val table = env.getString("table")
            val uid = env.getString("uid")
            val payload = env.getJSONObject("payload")
            val key = "$table|$uid"
            val existing = rows[key]
            if (existing != null) {
                // The real A′ SQL compares NORMALIZED payloads (volatile
                // per-device stamps stripped) — seed echoes from another
                // device are not changes and must not be conflicts.
                val changed = normalized(existing.payload).toString() != normalized(payload).toString()
                if (table in APPEND_ONLY_TABLES) {
                    if (changed) {
                        appendOnlyRefusals += "$table/$uid"
                    }
                    applied += 1 // insert-only: idempotent no-op either way
                    continue
                }
                if (changed &&
                    existing.originDevice != deviceId &&
                    (existing.seq > pullCursor || env.has("parked_seq"))
                ) {
                    conflicts += Conflict(
                        table, uid,
                        JSONObject(existing.payload.toString()),
                        existing.originDevice, deviceId,
                        detectedBy = if (env.has("parked_seq")) "parked_seq" else "pull_cursor",
                    )
                }
                if (changed) {
                    existing.seq = nextSeq()
                    existing.payload = payload
                    existing.originDevice = deviceId
                }
            } else {
                rows[key] = Row(table, uid, payload, deviceId, nextSeq())
            }
            applied += 1
        }
        return applied
    }

    private fun normalized(p: JSONObject): JSONObject =
        JSONObject(p.toString()).apply { remove("sync_updated_at") }

    /** Records with seq > sinceSeq, ordered by seq — the sync_pull contract. */
    fun syncPull(sinceSeq: Long): List<SyncPulledRecord> =
        rows.values.filter { it.seq > sinceSeq }
            .sortedBy { it.seq }
            .map {
                SyncPulledRecord(
                    serverSeq = it.seq,
                    shopId = null,
                    tableName = it.table,
                    rowUid = it.uid,
                    payload = JSONObject(it.payload.toString()),
                    originDevice = it.originDevice,
                )
            }

    companion object {
        val APPEND_ONLY_TABLES = setOf(
            "events", "delivery_status_history", "daily_close_actions", "physical_cash_counts",
        )
    }
}

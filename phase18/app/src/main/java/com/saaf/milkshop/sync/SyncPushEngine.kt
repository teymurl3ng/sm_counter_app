package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.data.SyncOutboxEntity
import org.json.JSONObject
import java.time.Instant

/**
 * Outcome of handing outbox rows to the server. Transient failures are
 * retried later; permanent failures carry a stable reason code and halt the
 * queue at the offending row.
 */
sealed interface SyncPushResult {
    data class Success(val pushedCount: Int) : SyncPushResult
    data class Transient(val reason: String) : SyncPushResult
    data class Permanent(val reasonCode: String, val message: String) : SyncPushResult
}

/**
 * Transport seam for the push engine. The engine owns envelope construction
 * (so it can re-serialize at ack time and structurally compare), the
 * transport owns the wire. Tests install fakes over both halves.
 */
fun interface SyncPushTransport {
    suspend fun push(envelopes: List<JSONObject>, rows: List<SyncOutboxEntity>): SyncPushResult
}

/**
 * Drains the sync outbox in strict enqueue order (Phase 3). Successful rows
 * are deleted from the head; a transient failure stops the run with the
 * queue intact, retried on the next kick; a permanent failure narrows down
 * to the offending row — everything ahead of it still leaves the queue, and
 * the queue HALTS at that row so no dependent record (a correction pointing
 * at it, the daily close covering it) can ever overtake it. One rate-limited
 * audit entry plus an optional listener (notification) alert the owner; the
 * next kick automatically retries the head, so fixing the cause resumes the
 * queue with nothing lost. The engine runs entirely in the background and
 * never touches entry paths (D9).
 */
class SyncPushEngine(
    private val database: MilkShopDatabase,
    private val transport: SyncPushTransport?,
    private val batchSize: Int = 50,
    private val haltedListener: (suspend (reasonCode: String, message: String, queueDepth: Int) -> Unit)? = null,
) {

    sealed interface DrainOutcome {
        data class Drained(val pushed: Int) : DrainOutcome
        data class RetryLater(val pushed: Int) : DrainOutcome
        data class Halted(val reasonCode: String, val pushed: Int) : DrainOutcome
        data object NotConfigured : DrainOutcome
    }

    private var lastHaltAuditEpochMs = 0L

    suspend fun drainOnce(): DrainOutcome {
        val activeTransport = transport ?: return DrainOutcome.NotConfigured
        var pushedTotal = 0
        while (true) {
            val batch = database.syncOutboxDao().nextBatch(batchSize)
            if (batch.isEmpty()) return DrainOutcome.Drained(pushedTotal)
            val (envelopes, effectiveRows) = buildFor(batch)
            attachParkHints(envelopes)
            when (val result = activeTransport.push(envelopes, effectiveRows)) {
                is SyncPushResult.Success -> {
                    val acked = ackedEntries(effectiveRows, envelopes)
                    delete(acked)
                    pushedTotal += acked.size
                    acked.forEach { row ->
                        // The local edit won and is now on the server; the held
                        // remote version is preserved in the sync_conflict audit.
                        database.syncParkedRowDao().delete(row.tableName, row.rowUid)
                    }
                }
                is SyncPushResult.Transient ->
                    return if (pushedTotal > 0) DrainOutcome.Drained(pushedTotal) else DrainOutcome.RetryLater(0)
                is SyncPushResult.Permanent -> {
                    // The batch contains at least one row the server refuses.
                    // Retry one by one, in order, so everything ahead of the
                    // offender still leaves — then halt exactly at its head.
                    for (row in effectiveRows) {
                        val (singleEnvelopes, singleRows) = buildFor(listOf(row))
                        if (singleEnvelopes.isEmpty()) {
                            delete(singleRows) // row's local record vanished mid-drain
                            continue
                        }
                        attachParkHints(singleEnvelopes)
                        when (val single = activeTransport.push(singleEnvelopes, singleRows)) {
                            is SyncPushResult.Success -> {
                                val acked = ackedEntries(singleRows, singleEnvelopes)
                                delete(acked)
                                pushedTotal += acked.size
                                acked.forEach { r ->
                                    database.syncParkedRowDao().delete(r.tableName, r.rowUid)
                                }
                            }
                            is SyncPushResult.Transient ->
                                return if (pushedTotal > 0) {
                                    DrainOutcome.Drained(pushedTotal)
                                } else {
                                    DrainOutcome.RetryLater(0)
                                }
                            is SyncPushResult.Permanent -> {
                                onHalted(single, database.syncOutboxDao().count().toInt())
                                return DrainOutcome.Halted(single.reasonCode, pushedTotal)
                            }
                        }
                    }
                    // Every row succeeded alone after a batch-level refusal:
                    // keep draining from the top.
                }
            }
        }
    }

    /**
     * Builds envelopes for the queued rows and drops orphans whose local
     * record has vanished (nothing to push; their queue slots must not block
     * dependents). Returns the wire envelopes with the rows that produced
     * them — the engine keeps both so it can re-serialize at ack time.
     */
    private suspend fun buildFor(rows: List<SyncOutboxEntity>): Pair<List<JSONObject>, List<SyncOutboxEntity>> {
        val build = SyncEnvelopeFactory(database).build(rows)
        if (build.skippedRows.isNotEmpty()) {
            database.syncOutboxDao().deleteByIds(build.skippedRows.map { it.outboxId })
        }
        val skippedIds = build.skippedRows.map { it.outboxId }.toSet()
        return build.envelopes to rows.filter { it.outboxId !in skippedIds }
    }

    private suspend fun delete(rows: List<SyncOutboxEntity>) {
        if (rows.isEmpty()) return
        database.syncOutboxDao().deleteByIds(rows.map { it.outboxId })
    }

    /**
     * C1 conflict evidence: an envelope for a uid holding a park (a remote
     * version we saw but refused to apply over an unpushed local edit)
     * carries the parked server_seq as a hint. The server then audits the
     * overwrite with the losing payload even though our pull cursor has
     * already advanced past that version. Stripped server-side before
     * storage; ignored by pre-migration servers.
     */
    private suspend fun attachParkHints(envelopes: List<JSONObject>) {
        if (envelopes.isEmpty()) return
        val parks = database.syncParkedRowDao().all()
        if (parks.isEmpty()) return
        val byKey = parks.associateBy { it.tableName to it.rowUid }
        envelopes.forEach { envelope ->
            val park = byKey[envelope.getString("table") to envelope.getString("uid")] ?: return@forEach
            envelope.put("parked_seq", park.serverSeq)
        }
    }

    /**
     * C1 ack equality: an outbox entry may leave the queue only when the
     * live row still structurally equals the envelope the server just
     * accepted. An edit that landed mid-push keeps its entry (the rebuilt
     * envelope differs), so it re-pushes with the newer content instead of
     * being silently acked away; a row that vanished at either build keeps
     * its entry one more cycle and is orphan-dropped by the next drain.
     */
    private suspend fun ackedEntries(rows: List<SyncOutboxEntity>, pushed: List<JSONObject>): List<SyncOutboxEntity> {
        val rebuilt = buildFor(rows)
        val rebuiltByUid = rebuilt.first.associateBy { it.getString("uid") }
        val pushedByUid = pushed.associateBy { it.getString("uid") }
        return rows.filter { row ->
            val original = pushedByUid[row.rowUid] ?: return@filter false
            val current = rebuiltByUid[row.rowUid] ?: return@filter false
            // parked_seq is transport-only conflict-evidence metadata (the
            // server strips it before storage); it must never break ack
            // equality, or hint-carrying rows would re-push forever.
            val originalState = if (original.has("parked_seq")) {
                JSONObject(original.toString()).apply { remove("parked_seq") }
            } else {
                original
            }
            jsonStructurallyEqual(originalState, current)
        }
    }
    private suspend fun onHalted(failure: SyncPushResult.Permanent, queueDepth: Int) {
        val now = System.currentTimeMillis()
        if (now - lastHaltAuditEpochMs >= HALT_AUDIT_INTERVAL_MS) {
            lastHaltAuditEpochMs = now
            runCatching {
                database.securityAuditDao().insert(
                    SecurityAuditEntryEntity(
                        auditId = 0,
                        occurredAt = Instant.now().toString(),
                        action = ACTION_SYNC_PUSH_HALTED,
                        result = "failure",
                        reasonCode = failure.reasonCode,
                        details = "queue_depth=$queueDepth;${failure.message}",
                    ),
                )
            }
        }
        haltedListener?.invoke(failure.reasonCode, failure.message, queueDepth)
    }

    companion object {
        const val ACTION_SYNC_PUSH_HALTED = "sync_push_halted"
        private const val HALT_AUDIT_INTERVAL_MS = 60L * 60 * 1000
    }
}

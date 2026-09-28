package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.data.SyncOutboxEntity
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
 * Transport seam for the push engine. The real HTTPS transport (sync Phase
 * 4/5) loads each row's payload by uid and POSTs it; tests install fakes.
 */
fun interface SyncPushTransport {
    suspend fun push(rows: List<SyncOutboxEntity>): SyncPushResult
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
            when (val result = activeTransport.push(batch)) {
                is SyncPushResult.Success -> {
                    delete(batch)
                    pushedTotal += batch.size
                }
                is SyncPushResult.Transient ->
                    return if (pushedTotal > 0) DrainOutcome.Drained(pushedTotal) else DrainOutcome.RetryLater(0)
                is SyncPushResult.Permanent -> {
                    // The batch contains at least one row the server refuses.
                    // Retry one by one, in order, so everything ahead of the
                    // offender still leaves — then halt exactly at its head.
                    for (row in batch) {
                        when (val single = activeTransport.push(listOf(row))) {
                            is SyncPushResult.Success -> {
                                delete(listOf(row))
                                pushedTotal += 1
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

    private suspend fun delete(rows: List<SyncOutboxEntity>) {
        if (rows.isEmpty()) return
        database.syncOutboxDao().deleteByIds(rows.map { it.outboxId })
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

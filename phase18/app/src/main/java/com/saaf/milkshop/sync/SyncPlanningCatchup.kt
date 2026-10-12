package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase

/** One-time replay for planning rows consumed by older receivers that skipped them. */
class SyncPlanningCatchup(private val database: MilkShopDatabase) {
    sealed interface Outcome {
        data class Done(val pages: Int, val planningRows: Int, val parked: Int) : Outcome
        data class RetryLater(val reason: String) : Outcome
    }

    suspend fun replay(fetch: suspend (Long) -> SyncPullOutcome): Outcome {
        val applyEngine = SyncApplyEngine(database)
        var sequence = 0L
        var pages = 0
        var planningRows = 0
        var parked = 0
        while (true) {
            when (val outcome = fetch(sequence)) {
                SyncPullOutcome.NotConfigured -> return Outcome.RetryLater("not_configured")
                is SyncPullOutcome.RetryLater -> return Outcome.RetryLater(outcome.reason)
                is SyncPullOutcome.Fetched -> {
                    val records = outcome.batch.records
                    if (records.isEmpty() && outcome.hasMore) return Outcome.RetryLater("malformed_response")
                    if (records.any { it.serverSeq <= sequence } ||
                        (records.isNotEmpty() && outcome.batch.latestServerSeq <= sequence)) {
                        return Outcome.RetryLater("malformed_response")
                    }
                    val planning = records.filter { it.tableName in PLANNING_TABLES }
                    if (planning.isNotEmpty()) {
                        val report = applyEngine.apply(planning)
                        if (report.skipped > 0 || report.conflicts > 0) {
                            return Outcome.RetryLater("planning_record_refused")
                        }
                        planningRows += planning.size
                        parked += report.parked
                    }
                    pages++
                    sequence = outcome.batch.latestServerSeq
                    if (!outcome.hasMore) {
                        val unresolved = database.syncParkedRowDao().all().any { park ->
                            park.tableName in PLANNING_TABLES &&
                                database.syncOutboxDao().countByTableAndUid(park.tableName, park.rowUid) == 0
                        }
                        return if (unresolved) Outcome.RetryLater("planning_dependency_unresolved")
                        else Outcome.Done(pages, planningRows, parked)
                    }
                }
            }
        }
    }

    private companion object {
        val PLANNING_TABLES = setOf("budget_plans", "collections_targets")
    }
}

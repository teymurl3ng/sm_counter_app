package com.saaf.milkshop.sync

import com.saaf.milkshop.data.MilkShopDatabase

/** Replays only current profile-bearing master rows, without writing the ordinary pull cursor. */
class SyncProfileCatchup(private val database: MilkShopDatabase) {
    sealed interface Outcome {
        data class Done(val pages: Int, val profileRows: Int, val parked: Int) : Outcome
        data class RetryLater(val reason: String) : Outcome
    }

    suspend fun replay(fetch: suspend (Long) -> SyncPullOutcome): Outcome {
        val applyEngine = SyncApplyEngine(database)
        var sequence = 0L
        var pages = 0
        var profileRows = 0
        var parked = 0
        while (true) {
            when (val outcome = fetch(sequence)) {
                SyncPullOutcome.NotConfigured -> return Outcome.RetryLater("not_configured")
                is SyncPullOutcome.RetryLater -> return Outcome.RetryLater(outcome.reason)
                is SyncPullOutcome.Fetched -> {
                    val records = outcome.batch.records
                    if (records.isEmpty()) {
                        return if (outcome.hasMore) Outcome.RetryLater("malformed_response")
                        else Outcome.Done(pages, profileRows, parked)
                    }
                    if (records.any { it.serverSeq <= sequence } ||
                        outcome.batch.latestServerSeq <= sequence) return Outcome.RetryLater("malformed_response")
                    val profiles = records.filter { it.tableName in setOf("products", "accounts") && it.payload.has("profile") }
                    if (profiles.isNotEmpty()) {
                        val report = applyEngine.apply(profiles)
                        if (report.skipped > 0 || report.conflicts > 0) {
                            return Outcome.RetryLater("profile_record_refused")
                        }
                        profileRows += profiles.size
                        parked += report.parked
                    }
                    pages++
                    sequence = outcome.batch.latestServerSeq
                    if (!outcome.hasMore) return Outcome.Done(pages, profileRows, parked)
                }
            }
        }
    }
}

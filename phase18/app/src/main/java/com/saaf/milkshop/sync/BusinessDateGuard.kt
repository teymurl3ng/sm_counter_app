package com.saaf.milkshop.sync

import java.time.Instant
import java.time.LocalDate

/**
 * Command-layer date sanity bound (P2e, concern 3 option i). Applies only
 * while a trusted anchor exists — an unanchored device keeps today's
 * behavior exactly (D9: wrong or missing time never blocks entry). While
 * anchored, a business date may not sit further ahead of trusted time than
 * the command's horizon: one day for things that already happened (events,
 * counts, closes, corrections, attendance) and one week for planned
 * deliveries. Deliberate backdating to any open day stays allowed; the
 * closed-day rules elsewhere remain the only other date gate.
 */
object BusinessDateGuard {

    /** Happened-on dates: at most one day ahead of trusted time. */
    const val HAPPENED_MAX_AHEAD_DAYS: Long = 1L

    /** Planned deliveries may be scheduled up to a week ahead. */
    const val PLANNING_MAX_AHEAD_DAYS: Long = 7L

    fun requireHappenedWithinHorizon(businessDate: String) =
        requireWithinHorizon(businessDate, HAPPENED_MAX_AHEAD_DAYS, "business date")

    fun requirePlannedWithinHorizon(businessDate: String) =
        requireWithinHorizon(businessDate, PLANNING_MAX_AHEAD_DAYS, "planned delivery date")

    private fun requireWithinHorizon(businessDate: String, maxAheadDays: Long, label: String) {
        val trustedMs = TrustedTime.trustedNowEpochMs() ?: return
        val limit = Instant.ofEpochMilli(trustedMs)
            .atZone(BusinessClock.currentZone())
            .toLocalDate()
            .plusDays(maxAheadDays)
        require(!LocalDate.parse(businessDate).isAfter(limit)) {
            "The $label cannot be more than $maxAheadDays day(s) ahead of trusted time."
        }
    }
}

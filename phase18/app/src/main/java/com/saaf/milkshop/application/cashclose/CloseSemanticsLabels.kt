package com.saaf.milkshop.application.cashclose

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


/**
 * H8/H9 close-semantics labels (pure, JVM-tested). Human phrasing for the
 * decisions locked 2026-10-06: absorbed-day enumeration in the close dock,
 * the multi-day coverage note on the variance screen, and the reopen
 * reverse-consequence line. Dates render as "Mon 5 Oct".
 */
object CloseSemanticsLabels {

    private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    fun humanizeDate(isoDate: String): String =
        runCatching { LocalDate.parse(isoDate).format(DAY_FORMAT) }.getOrDefault(isoDate)

    /** Dock line before the write: which earlier days this close absorbs. */
    fun absorbedDaysLine(days: List<Pair<String, Int>>): String? {
        if (days.isEmpty()) return null
        val named = days.joinToString(", ") { (date, count) ->
            "${humanizeDate(date)} ($count event${if (count == 1) "" else "s"})"
        }
        return "This close also seals: $named. They keep their events but get no count of their own."
    }

    /** One-line variance-screen note: which days this count's variance covers. */
    fun coverageLine(countedDate: String, absorbedDates: List<String>): String? {
        if (absorbedDates.isEmpty()) return null
        val all = (listOf(countedDate) + absorbedDates).joinToString(" + ") { humanizeDate(it) }
        return "This count covers $all — the variance is multi-day."
    }

    /** Reopen confirmation: the reverse consequence of unsealing. */
    fun reopenLine(reopenedDate: String, absorbedDates: List<String>): String? {
        if (absorbedDates.isEmpty()) return null
        val named = absorbedDates.joinToString(", ") { humanizeDate(it) }
        return "This also reopens $named — they were sealed by ${humanizeDate(reopenedDate)}'s close."
    }
}

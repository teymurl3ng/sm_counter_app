package com.saaf.milkshop.application

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Single source of truth for rendering stored timestamps.
 *
 * Storage conventions (load-bearing — do not change casually):
 *  - `created_at`-style columns hold UTC instant strings (`Instant.now()`).
 *  - business-date columns hold device-local date-only strings.
 *
 * Everything here converts to `ZoneId.systemDefault()` at render time.
 */
object AppTimeFormats {
    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
    private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM uuuu · h:mm a").withZone(ZoneId.systemDefault())
    private val dateFormat = DateTimeFormatter.ofPattern("d MMM uuuu").withZone(ZoneId.systemDefault())

    private fun instant(iso: String): Instant? = runCatching { Instant.parse(iso) }.getOrNull()

    /** UTC instant → device-zone time-of-day, e.g. "1:41 PM". Raw input if unparseable. */
    fun clock(iso: String): String {
        val value = instant(iso) ?: return iso
        return clockFormat.format(value)
    }

    /** UTC instant → device-zone date+time, e.g. "27 Sep 2026 · 1:41 PM". */
    fun dateTime(iso: String): String {
        val value = instant(iso) ?: return iso
        return dateTimeFormat.format(value)
    }

    /** Date-only or instant string → device-zone date, e.g. "27 Sep 2026". */
    fun date(value: String): String {
        val local = runCatching { LocalDate.parse(value) }.getOrNull()
        if (local != null) return dateFormat.format(local)
        val value2 = instant(value) ?: return value
        return dateFormat.format(value2)
    }
}

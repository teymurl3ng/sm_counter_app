package com.saaf.milkshop.nativeui.common

import com.saaf.milkshop.application.AppTimeFormats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMM uuuu")

private val RECORD_TIMESTAMP_FORMAT =
    DateTimeFormatter.ofPattern("d MMM uuuu · HH:mm").withZone(ZoneId.systemDefault())

/**
 * Renders a stored ISO-8601 UTC timestamp in the device's time zone, e.g.
 * "26 Sep 2026 · 13:41". Returns the input unchanged when it cannot be
 * parsed (defensive; stored values are always Instant strings).
 */
fun formatRecordTimestamp(iso: String): String {
    // Instants (created_at) get date+time in the device zone.
    runCatching { return RECORD_TIMESTAMP_FORMAT.format(Instant.parse(iso)) }
    // Date-only business values get a plain date.
    runCatching { return dateFormat.format(LocalDate.parse(iso)) }
    return iso
}

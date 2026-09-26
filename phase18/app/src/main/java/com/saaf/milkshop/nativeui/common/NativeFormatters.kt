package com.saaf.milkshop.nativeui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val RECORD_TIMESTAMP_FORMAT =
    DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm").withZone(ZoneId.systemDefault())

/**
 * Renders a stored ISO-8601 UTC timestamp in the device's time zone, e.g.
 * "26 Sep 2026 · 13:41". Returns the input unchanged when it cannot be
 * parsed (defensive; stored values are always Instant strings).
 */
fun formatRecordTimestamp(iso: String): String =
    runCatching { RECORD_TIMESTAMP_FORMAT.format(Instant.parse(iso)) }.getOrDefault(iso)

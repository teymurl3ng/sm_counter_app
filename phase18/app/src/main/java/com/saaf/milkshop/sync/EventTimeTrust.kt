package com.saaf.milkshop.sync

import java.time.Instant

/**
 * Time-trust stamp applied to every new event row at construction. Until the
 * trusted-time engine (sync Phase 2) provides a server-anchored estimate,
 * every row is honestly marked [DEVICE_ONLY] with an empty estimate and the
 * device clock as the raw timestamp. The anchor will replace these helpers'
 * internals without touching entity call sites.
 */
object EventTimeTrust {
    const val DEVICE_ONLY = "device_only"

    /** Device-clock timestamp captured when the row is constructed. */
    fun rawDeviceNow(): String = Instant.now().toString()

    /** Trusted estimate; empty until an anchor exists. */
    fun trustedEstimateNow(): String = ""

    fun flagNow(): String = DEVICE_ONLY
}

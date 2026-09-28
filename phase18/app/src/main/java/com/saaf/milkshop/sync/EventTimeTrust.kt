package com.saaf.milkshop.sync

import java.time.Instant

/**
 * Time-trust stamp applied to every new event row at construction (P2c).
 * Three honest states:
 *  - [DEVICE_ONLY]: no trusted anchor (fresh install or rebooted since the
 *    last server contact) — device clock, empty estimate;
 *  - [CLOCK_DIVERGED]: anchored, but the device clock disagrees with trusted
 *    time beyond the warning threshold — timestamps are suspect;
 *  - [ANCHORED]: trusted time available and the device clock agrees with it.
 *
 * None of these ever gates a write (D9); they are audit marks carried on the
 * row and in backups.
 */
object EventTimeTrust {
    const val DEVICE_ONLY = "device_only"
    const val ANCHORED = "anchored"
    const val CLOCK_DIVERGED = "clock_diverged"

    /** Device-clock timestamp captured when the row is constructed. */
    fun rawDeviceNow(): String = Instant.now().toString()

    /** Trusted estimate; empty while unanchored. */
    fun trustedEstimateNow(): String =
        TrustedTime.trustedNowEpochMs()?.let { epoch -> Instant.ofEpochMilli(epoch).toString() } ?: ""

    fun flagNow(): String {
        val skew = TrustedTime.deviceSkewMs() ?: return DEVICE_ONLY
        return if (skew > TrustedTime.warnThresholdMs) CLOCK_DIVERGED else ANCHORED
    }
}

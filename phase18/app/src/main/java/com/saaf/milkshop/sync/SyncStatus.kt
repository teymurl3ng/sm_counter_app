package com.saaf.milkshop.sync

/** Snapshot of this installation's sync state for the owner-facing screen. */
data class NativeSyncStatus(
    val environment: String,
    val shopName: String?,
    val role: String,
    val sessionPresent: Boolean,
    val trustedTimeAnchored: Boolean,
    val outboxCount: Long,
    val pullCursor: Long,
    // go 8: restored entries held for an owner decision in the sync-review
    // surface (they never auto-push).
    val holdCount: Int = 0,
    // Track 1 A2'/A3: the enrollment's identity + grace state, for the
    // sync screen's identity card. Display-only for SUSPENDED/REVOKED.
    val identityStatus: String? = null,
    val identityServerUsername: String? = null,
    val identityOwnerName: String? = null,
    val graceDaysLeft: Int? = null,
    val graceWarningLevel: Int = 0,
)

/** One held restored entry, for the sync-review list. */
data class NativeSyncHold(
    val tableName: String,
    val rowUid: String,
    val originDevice: String?,
    val heldAt: String,
    // Human-readable title derived from the hold's payload snapshot
    // (SyncHoldLabels); blank when the snapshot is missing, so callers
    // fall back to the table + short uid.
    val label: String = "",
)

data class NativeSyncShopChoice(
    val shopId: String,
    val shopName: String?,
)

/** One linkable local owner, for the enrollment owner chooser (Track 1 A2'). */
data class NativeSyncOwnerChoice(
    val userId: Long,
    val displayName: String,
)

/** Server endpoints per environment (spec §6: release builds are PROD-only). */
object SyncEndpoints {

    val DEV = SyncServerConfig(
        environment = SyncServerConfig.ENVIRONMENT_DEV,
        baseUrl = "https://tghzmofkkdsaasvnixps.supabase.co",
        publishableKey = "sb_publishable_agJulQ-YO1UXjGPpLkR3cA_Hqtgwbvn",
    )

    val PROD = SyncServerConfig(
        environment = SyncServerConfig.ENVIRONMENT_PROD,
        baseUrl = "https://tlsuxbghuhlqntkgtrxt.supabase.co",
        publishableKey = "sb_publishable_F_ZzS7OxeTPFIuZQiLyqPw_w0p5H7aH",
    )

    fun forEnvironment(environment: String): SyncServerConfig =
        if (environment == SyncServerConfig.ENVIRONMENT_DEV) DEV else PROD
}

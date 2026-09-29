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
)

data class NativeSyncShopChoice(
    val shopId: String,
    val shopName: String?,
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

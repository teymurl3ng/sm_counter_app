package com.saaf.milkshop.application.owner

import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudBackupState
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import java.time.Duration
import java.time.Instant

data class NativeOwnerBackupSnapshot(
    val configured: Boolean,
    val enabled: Boolean,
    val accountDisplay: String?,
    val scheduleDescription: String,
    val lastSuccessAt: String?,
    val lastAttemptAt: String?,
    val lastErrorMessage: String?,
    val stale: Boolean,
    val pending: Boolean,
    val statusHeadline: String,
)

/**
 * Owner Backup boundary: read Drive vault status from
 * [CloudBackupSettingsStore] the same way Overview does. Does not require
 * [com.saaf.milkshop.cloud.CloudBackupCoordinator], and does not execute
 * export / restore / connect.
 */
class NativeOwnerBackupFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val cloudBackupSettings: CloudBackupSettingsStore? = null,
) {
    suspend fun load(
        sessionHandle: String,
        now: Instant = Instant.now(),
    ): NativeOwnerBackupSnapshot {
        requireOwnerBackup(sessionHandle)
        val state = cloudBackupSettings?.read()
        if (state == null) {
            return NativeOwnerBackupSnapshot(
                configured = false,
                enabled = false,
                accountDisplay = null,
                scheduleDescription = "Google Drive vault is not configured on this device.",
                lastSuccessAt = null,
                lastAttemptAt = null,
                lastErrorMessage = null,
                stale = false,
                pending = false,
                statusHeadline = "Backup not configured",
            )
        }
        val stale = isStale(state, now)
        val pending = state.pending != null
        return NativeOwnerBackupSnapshot(
            configured = true,
            enabled = state.enabled,
            accountDisplay = state.accountDisplay.takeIf { it.isNotBlank() },
            scheduleDescription = "Encrypted Google Drive vault",
            lastSuccessAt = state.lastSuccessAt,
            lastAttemptAt = state.lastAttemptAt,
            lastErrorMessage = state.lastErrorMessage,
            stale = stale,
            pending = pending,
            statusHeadline = statusHeadline(state, stale, pending),
        )
    }

    private suspend fun requireOwnerBackup(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.CREATE_EXPORT_BACKUP)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Backup is only available to Owner."
        }
    }

    private fun isStale(state: CloudBackupState, now: Instant): Boolean {
        val protectedAt = state.lastSuccessAt ?: state.configuredAt
        return runCatching {
            Duration.between(Instant.parse(protectedAt), now) > STALE_AFTER
        }.getOrDefault(false)
    }

    private fun statusHeadline(
        state: CloudBackupState,
        stale: Boolean,
        pending: Boolean,
    ): String = when {
        !state.lastErrorMessage.isNullOrBlank() -> "Backup needs attention"
        stale -> "Recovery point is stale"
        pending -> "Backup pending upload"
        !state.enabled -> "Drive vault paused"
        state.lastSuccessAt != null -> "Drive vault protected"
        else -> "Drive vault configured"
    }

    companion object {
        private val STALE_AFTER = Duration.ofHours(48)
    }
}

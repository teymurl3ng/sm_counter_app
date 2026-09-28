package com.saaf.milkshop.sync

import android.content.Context
import android.provider.Settings
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import java.time.Instant

/**
 * Non-blocking time-trust observability (P2d). When the device clock
 * diverges from trusted time beyond the warn threshold the monitor appends
 * one rate-limited security-audit entry (alert severity past the alert
 * threshold); a disabled automatic date-and-time setting is reported the
 * same way. Warnings never gate anything — a sale records regardless.
 */
object TimeTrustMonitor {

    const val ACTION_TIME_TRUST = "time_trust_status"
    const val RESULT_WARNING = "warning"
    const val RESULT_ALERT = "alert"
    const val REASON_CLOCK_SKEW = "clock_skew"
    const val REASON_AUTO_TIME_OFF = "auto_time_off"

    private const val MIN_AUDIT_INTERVAL_MS: Long = 60L * 60 * 1000

    data class Status(
        val anchored: Boolean,
        val deviceSkewMs: Long?,
        val autoTimeEnabled: Boolean,
        val severity: String?,
        val reasonCode: String?,
    )

    @Volatile private var lastAuditEpochMs: Long = 0L

    fun readStatus(context: Context): Status {
        val skew = TrustedTime.deviceSkewMs()
        val autoTime = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) != 0
        }.getOrDefault(true)
        val severity = when {
            skew != null && skew > TrustedTime.alertThresholdMs -> RESULT_ALERT
            skew != null && skew > TrustedTime.warnThresholdMs -> RESULT_WARNING
            !autoTime -> RESULT_WARNING
            else -> null
        }
        val reason = when {
            skew != null && skew > TrustedTime.warnThresholdMs -> REASON_CLOCK_SKEW
            !autoTime -> REASON_AUTO_TIME_OFF
            else -> null
        }
        return Status(TrustedTime.isAnchored(), skew, autoTime, severity, reason)
    }

    /**
     * Appends an audit row when the status warrants one and the rate limit
     * allows. Called at app start and after every anchor refresh.
     */
    suspend fun check(context: Context, database: MilkShopDatabase) {
        val status = readStatus(context)
        if (status.severity == null) return
        val now = System.currentTimeMillis()
        if (now - lastAuditEpochMs < MIN_AUDIT_INTERVAL_MS) return
        lastAuditEpochMs = now
        val details = buildString {
            append("anchored=").append(status.anchored)
            append(";skew_ms=").append(status.deviceSkewMs ?: -1)
            append(";auto_time=").append(if (status.autoTimeEnabled) 1 else 0)
        }
        runCatching {
            database.securityAuditDao().insert(
                SecurityAuditEntryEntity(
                    auditId = 0,
                    occurredAt = Instant.now().toString(),
                    action = ACTION_TIME_TRUST,
                    result = status.severity,
                    reasonCode = status.reasonCode,
                    details = details,
                ),
            )
        }
    }

    /** Test-only: clears the rate limit. */
    fun resetForTest() {
        lastAuditEpochMs = 0L
    }
}

package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "app_users",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeProfileEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["employee_account_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["created_by_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["disabled_by_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["login_name"], unique = true),
        Index(value = ["employee_account_id"], unique = true),
        Index(value = ["created_by_user_id"]),
        Index(value = ["disabled_by_user_id"]),
    ],
)
data class AppUserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    @ColumnInfo(name = "login_name")
    val loginName: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val role: String,
    @ColumnInfo(name = "employee_account_id")
    val employeeAccountId: Long? = null,
    val status: String = "active",
    @ColumnInfo(name = "credential_setup_required")
    val credentialSetupRequired: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "created_by_user_id")
    val createdByUserId: Long? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "disabled_at")
    val disabledAt: String? = null,
    @ColumnInfo(name = "disabled_by_user_id")
    val disabledByUserId: Long? = null,
)

@Entity(
    tableName = "user_credentials",
    foreignKeys = [
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
)
data class UserCredentialEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val algorithm: String,
    val salt: ByteArray,
    val verifier: ByteArray,
    @ColumnInfo(name = "memory_kib")
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
    @ColumnInfo(name = "changed_at")
    val changedAt: String,
    @ColumnInfo(name = "must_change")
    val mustChange: Boolean = false,
    @ColumnInfo(name = "failed_attempt_count")
    val failedAttemptCount: Int = 0,
    @ColumnInfo(name = "last_failed_at_epoch_ms")
    val lastFailedAtEpochMs: Long? = null,
    @ColumnInfo(name = "locked_until_epoch_ms")
    val lockedUntilEpochMs: Long? = null,
)

@Entity(
    tableName = "saved_recovery_codes",
    foreignKeys = [
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["user_id"])],
)
data class SavedRecoveryCodeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "recovery_code_id")
    val recoveryCodeId: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val salt: ByteArray,
    val verifier: ByteArray,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "used_at")
    val usedAt: String? = null,
    @ColumnInfo(name = "failed_attempt_count")
    val failedAttemptCount: Int = 0,
    @ColumnInfo(name = "locked_until_epoch_ms")
    val lockedUntilEpochMs: Long? = null,
)

@Entity(
    tableName = "security_audit_entries",
    foreignKeys = [
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["actor_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["target_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["occurred_at"]),
        Index(value = ["actor_user_id"]),
        Index(value = ["target_user_id"]),
    ],
)
data class SecurityAuditEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "audit_id")
    val auditId: Long = 0,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: String,
    val action: String,
    val result: String,
    @ColumnInfo(name = "reason_code")
    val reasonCode: String? = null,
    @ColumnInfo(name = "actor_user_id")
    val actorUserId: Long? = null,
    @ColumnInfo(name = "actor_name_snapshot")
    val actorNameSnapshot: String? = null,
    @ColumnInfo(name = "actor_role_snapshot")
    val actorRoleSnapshot: String? = null,
    @ColumnInfo(name = "target_user_id")
    val targetUserId: Long? = null,
    val details: String? = null,
)

@Entity(
    tableName = "event_actor_links",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["actor_user_id"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["actor_user_id"])],
)
data class EventActorLinkEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: Long,
    @ColumnInfo(name = "actor_user_id")
    /** Null for a remote snapshot; a foreign device's integer ID is not a local identity. */
    val actorUserId: Long?,
    @ColumnInfo(name = "actor_name_snapshot")
    val actorNameSnapshot: String,
    @ColumnInfo(name = "actor_role_snapshot")
    val actorRoleSnapshot: String,
    @ColumnInfo(name = "attributed_at")
    val attributedAt: String,
)

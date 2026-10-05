package com.saaf.milkshop.data

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.Update
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.ColumnInfo

/**
 * Account Bridge schema-9 records (ACCOUNT_BRIDGE_PHASE1_SPEC.md §4).
 *
 * These tables cache server-derived identity state only. The Android
 * installation never invents server UUIDs and never treats a server role
 * as anything but an entitlement label; Kotlin keeps enforcing the local
 * role-to-permission matrix. `verified_at` is persisted as epoch millis
 * (spec `verified_at`) so offline-grace arithmetic never re-parses text.
 */
@Entity(
    tableName = "account_installation_binding",
    indices = [Index(value = ["environment", "business_id", "shop_id"], unique = true)],
)
data class AccountInstallationBindingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "binding_id")
    val bindingId: Long = 0,
    val environment: String,
    @ColumnInfo(name = "business_id")
    val businessId: String,
    @ColumnInfo(name = "shop_id")
    val shopId: String,
    @ColumnInfo(name = "shop_name_snapshot")
    val shopNameSnapshot: String,
    val status: String,
    @ColumnInfo(name = "verified_at_epoch_ms")
    val verifiedAtEpochMs: Long? = null,
    @ColumnInfo(name = "contract_version")
    val contractVersion: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)

@Entity(
    tableName = "server_identity_links",
    indices = [Index(value = ["server_staff_id"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["local_user_id"],
        ),
    ],
)
data class ServerIdentityLinkEntity(
    @PrimaryKey
    @ColumnInfo(name = "local_user_id")
    val localUserId: Long,
    @ColumnInfo(name = "server_staff_id")
    val serverStaffId: String,
    @ColumnInfo(name = "business_id")
    val businessId: String,
    @ColumnInfo(name = "username_snapshot")
    val usernameSnapshot: String? = null,
    @ColumnInfo(name = "role_snapshot")
    val roleSnapshot: String? = null,
    val status: String,
    @ColumnInfo(name = "verified_at_epoch_ms")
    val verifiedAtEpochMs: Long? = null,
    @ColumnInfo(name = "server_changed_at_epoch_ms")
    val serverChangedAtEpochMs: Long? = null,
    @ColumnInfo(name = "last_error_code")
    val lastErrorCode: String? = null,
)

@Entity(
    tableName = "server_shop_snapshots",
    indices = [
        Index(value = ["server_shop_id", "local_user_id"], unique = true),
        Index(value = ["local_user_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AppUserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["local_user_id"],
        ),
    ],
)
data class ServerShopSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: Long = 0,
    @ColumnInfo(name = "server_shop_id")
    val serverShopId: String,
    @ColumnInfo(name = "shop_name_snapshot")
    val shopNameSnapshot: String,
    @ColumnInfo(name = "observed_at_epoch_ms")
    val observedAtEpochMs: Long,
    @ColumnInfo(name = "local_user_id")
    val localUserId: Long,
)

@Dao
interface ServerAccountDao {
    @Query("SELECT * FROM account_installation_binding LIMIT 1")
    suspend fun findBinding(): AccountInstallationBindingEntity?

    @Query("SELECT * FROM account_installation_binding")
    suspend fun bindings(): List<AccountInstallationBindingEntity>

    @Insert
    suspend fun insertBinding(binding: AccountInstallationBindingEntity): Long

    @Query("UPDATE account_installation_binding SET status = :status, verified_at_epoch_ms = :verifiedAtEpochMs, updated_at = :updatedAt WHERE binding_id = :bindingId")
    suspend fun updateBindingStatus(bindingId: Long, status: String, verifiedAtEpochMs: Long?, updatedAt: String)

    @Query("DELETE FROM account_installation_binding")
    suspend fun deleteBindings()

    @Query("SELECT * FROM server_identity_links")
    suspend fun identityLinks(): List<ServerIdentityLinkEntity>

    @Query("SELECT * FROM server_identity_links WHERE local_user_id = :localUserId")
    suspend fun findIdentityLink(localUserId: Long): ServerIdentityLinkEntity?

    @Insert
    suspend fun insertIdentityLink(link: ServerIdentityLinkEntity): Long

    @Query("UPDATE server_identity_links SET status = :status, verified_at_epoch_ms = :verifiedAtEpochMs, last_error_code = :lastErrorCode WHERE local_user_id = :localUserId")
    suspend fun updateIdentityLinkStatus(localUserId: Long, status: String, verifiedAtEpochMs: Long?, lastErrorCode: String?)

    /** H13: full-row refresh of a verified link (all snapshots + fresh clock). */
    @Update
    suspend fun updateIdentityLink(link: ServerIdentityLinkEntity)

    @Query("DELETE FROM server_identity_links")
    suspend fun deleteIdentityLinks()

    @Query("SELECT * FROM server_shop_snapshots")
    suspend fun serverShopSnapshots(): List<ServerShopSnapshotEntity>

    @Insert
    suspend fun insertServerShopSnapshots(snapshots: List<ServerShopSnapshotEntity>)

    @Query("DELETE FROM server_shop_snapshots")
    suspend fun deleteServerShopSnapshots()
}

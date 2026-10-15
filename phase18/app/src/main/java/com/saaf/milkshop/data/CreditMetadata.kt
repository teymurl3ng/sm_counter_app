package com.saaf.milkshop.data

import androidx.room3.*

@Entity(tableName = "credit_term_revisions", foreignKeys = [ForeignKey(entity = AccountEntity::class,
    parentColumns = ["uid"], childColumns = ["customer_uid"])], indices = [Index("customer_uid")])
data class CreditTermRevisionEntity(
    @PrimaryKey @ColumnInfo(name = "revision_uid") val revisionUid: String,
    @ColumnInfo(name = "customer_uid") val customerUid: String,
    @ColumnInfo(name = "prior_revision_uid") val priorRevisionUid: String,
    @ColumnInfo(name = "term_kind") val termKind: String,
    @ColumnInfo(name = "term_value") val termValue: Int,
    @ColumnInfo(name = "local_actor_uid") val localActorUid: String,
    @ColumnInfo(name = "accepted_at") val acceptedAt: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 1,
)

@Entity(tableName = "credit_term_heads", foreignKeys = [ForeignKey(entity = AccountEntity::class,
    parentColumns = ["uid"], childColumns = ["customer_uid"]), ForeignKey(entity = CreditTermRevisionEntity::class,
    parentColumns = ["revision_uid"], childColumns = ["revision_uid"])], indices = [Index("revision_uid")])
data class CreditTermHeadEntity(
    @PrimaryKey @ColumnInfo(name = "customer_uid") val customerUid: String,
    @ColumnInfo(name = "revision_uid") val revisionUid: String,
)

@Entity(tableName = "credit_sale_agreements", foreignKeys = [ForeignKey(entity = EventEntity::class,
    parentColumns = ["event_uid"], childColumns = ["root_sale_uid"]), ForeignKey(entity = AccountEntity::class,
    parentColumns = ["uid"], childColumns = ["customer_uid"])], indices = [Index("customer_uid")])
data class CreditSaleAgreementEntity(
    @PrimaryKey @ColumnInfo(name = "root_sale_uid") val rootSaleUid: String,
    @ColumnInfo(name = "customer_uid") val customerUid: String,
    val evidence: String,
    @ColumnInfo(name = "term_kind") val termKind: String,
    @ColumnInfo(name = "term_value") val termValue: Int,
    @ColumnInfo(name = "revision_uid") val revisionUid: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 1,
)

@Entity(tableName = "credit_review_actions", foreignKeys = [ForeignKey(entity = AccountEntity::class,
    parentColumns = ["uid"], childColumns = ["customer_uid"])], indices = [Index("customer_uid"), Index("clears_set_uid")])
data class CreditReviewActionEntity(
    @PrimaryKey @ColumnInfo(name = "action_uid") val actionUid: String,
    @ColumnInfo(name = "customer_uid") val customerUid: String,
    val kind: String,
    @ColumnInfo(name = "clears_set_uid") val clearsSetUid: String?,
    val reason: String,
    @ColumnInfo(name = "local_actor_uid") val localActorUid: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: String,
    @ColumnInfo(name = "business_date") val businessDate: String,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 1,
)

/** Dedicated queue: never fed to the old global sync allowlist. */
@Entity(tableName = "credit_metadata_pending")
data class CreditMetadataPendingEntity(
    @PrimaryKey @ColumnInfo(name = "operation_uid") val operationUid: String,
    val kind: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    val held: Boolean = false,
)

@Entity(tableName = "credit_metadata_sync_state")
data class CreditMetadataSyncStateEntity(
    @PrimaryKey @ColumnInfo(name = "state_id") val stateId: Int = 1,
    @ColumnInfo(name = "shop_uid") val shopUid: String,
    @ColumnInfo(name = "last_server_seq") val lastServerSeq: Long = 0,
    @ColumnInfo(name = "contract_version") val contractVersion: Int = 0,
    val enabled: Boolean = false,
)

@Dao
interface CreditMetadataDao {
    @Query("SELECT * FROM credit_term_revisions ORDER BY revision_uid") suspend fun revisions(): List<CreditTermRevisionEntity>
    @Query("SELECT * FROM credit_term_revisions WHERE revision_uid = :uid") suspend fun revision(uid: String): CreditTermRevisionEntity?
    @Query("SELECT * FROM credit_term_heads ORDER BY customer_uid") suspend fun heads(): List<CreditTermHeadEntity>
    @Query("SELECT * FROM credit_term_heads WHERE customer_uid = :uid") suspend fun head(uid: String): CreditTermHeadEntity?
    @Query("SELECT * FROM credit_sale_agreements ORDER BY root_sale_uid") suspend fun agreements(): List<CreditSaleAgreementEntity>
    @Query("SELECT * FROM credit_sale_agreements WHERE customer_uid = :uid ORDER BY root_sale_uid") suspend fun agreementsFor(uid: String): List<CreditSaleAgreementEntity>
    @Query("SELECT * FROM credit_sale_agreements WHERE root_sale_uid = :uid") suspend fun agreement(uid: String): CreditSaleAgreementEntity?
    @Query("SELECT * FROM credit_review_actions ORDER BY action_uid") suspend fun actions(): List<CreditReviewActionEntity>
    @Query("SELECT * FROM credit_review_actions WHERE customer_uid = :uid ORDER BY action_uid") suspend fun actionsFor(uid: String): List<CreditReviewActionEntity>
    @Query("SELECT * FROM credit_review_actions WHERE action_uid = :uid") suspend fun action(uid: String): CreditReviewActionEntity?
    @Query("SELECT * FROM credit_metadata_pending ORDER BY operation_uid") suspend fun pending(): List<CreditMetadataPendingEntity>
    @Query("SELECT * FROM credit_metadata_pending WHERE operation_uid = :uid") suspend fun pending(uid: String): CreditMetadataPendingEntity?
    @Query("SELECT * FROM credit_metadata_sync_state WHERE state_id = 1") suspend fun state(): CreditMetadataSyncStateEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRevision(row: CreditTermRevisionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAgreement(row: CreditSaleAgreementEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAction(row: CreditReviewActionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPending(row: CreditMetadataPendingEntity)
    @Upsert suspend fun saveHead(row: CreditTermHeadEntity)
    @Upsert suspend fun saveState(row: CreditMetadataSyncStateEntity)
    @Query("DELETE FROM credit_metadata_pending WHERE operation_uid = :uid") suspend fun deletePending(uid: String)
    @Query("DELETE FROM credit_metadata_pending") suspend fun clearPending()
    @Query("DELETE FROM credit_metadata_sync_state") suspend fun clearState()
    @Query("DELETE FROM credit_term_heads") suspend fun clearHeads()
    @Query("DELETE FROM credit_sale_agreements") suspend fun clearAgreements()
    @Query("DELETE FROM credit_review_actions") suspend fun clearActions()
    @Query("DELETE FROM credit_term_revisions") suspend fun clearRevisions()
}

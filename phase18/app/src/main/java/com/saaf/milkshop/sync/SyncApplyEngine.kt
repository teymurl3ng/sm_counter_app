package com.saaf.milkshop.sync

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.SyncMigrationSql
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.AttendanceRecordEntity
import com.saaf.milkshop.data.DailyCloseActionEntity
import com.saaf.milkshop.data.DeliveryEntity
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.PhysicalCashCountEntity
import com.saaf.milkshop.data.ProductEntity
import com.saaf.milkshop.data.SettingEntity
import com.saaf.milkshop.data.SpoilageDetailEntity
import com.saaf.milkshop.domain.InventoryCostProjectionService
import org.json.JSONObject
import java.time.Instant

/**
 * Phase 7 apply engine: turns pulled server records into local Room rows.
 *
 * Invariants:
 *  - Idempotent — applying the same batch twice changes nothing (uid keying).
 *  - Origin-local integer ids in payloads are NEVER trusted; every cross-row
 *    reference is remapped through its `*_uid` companion field.
 *  - Records apply in server_seq order inside one write transaction with the
 *    sync capture triggers dropped (pulled rows are not device-originated and
 *    must not echo back into the outbox).
 *  - A correction whose target already has a different child correction is a
 *    sibling conflict (P1): it is skipped and audited, never inserted twice —
 *    the read models would double-count.
 *  - A record that cannot resolve (missing target, closed-day refusal) is
 *    skipped and audited; the cursor still advances so one bad row can never
 *    wedge the queue.
 *  - The costing projection is refreshed after the batch; derived views
 *    recompute on read.
 */
class SyncApplyEngine(private val database: MilkShopDatabase) {

    data class ApplyReport(
        val applied: Int,
        val updated: Int,
        val skipped: Int,
        val parked: Int,
        val conflicts: Int,
        val latestServerSeq: Long,
    )

    suspend fun apply(records: List<SyncPulledRecord>): ApplyReport = database.withWriteTransaction {
        var applied = 0
        var updated = 0
        var skipped = 0
        var parked = 0
        var conflicts = 0

        // Capture triggers stay down for the whole batch: pulled rows are
        // server-originated history, not new device writes.
        SyncMigrationSql.triggerNames.forEach { name -> executeSQL("DROP TRIGGER IF EXISTS $name") }
        try {
            for (record in records) {
                // Per-record savepoint: a mid-record failure rolls back to a
                // clean boundary so a half-applied record (header without its
                // ledger children) can never commit — idempotent re-apply of
                // a skipped uid would otherwise never repair it.
                executeSQL("SAVEPOINT record_apply")
                val outcome: PerRecord = try {
                    val result = applyOne(record)
                    executeSQL("RELEASE record_apply")
                    result
                } catch (failure: Throwable) {
                    executeSQL("ROLLBACK TO record_apply")
                    executeSQL("RELEASE record_apply")
                    auditSkip(record, failure.message ?: failure.javaClass.simpleName)
                    PerRecord.SKIPPED
                }
                when (outcome) {
                    PerRecord.APPLIED -> applied += 1
                    PerRecord.UPDATED -> updated += 1
                    PerRecord.PRESENT, PerRecord.SKIPPED -> skipped += 1
                    PerRecord.PARKED -> parked += 1
                    PerRecord.CONFLICT -> conflicts += 1
                }
            }
            // C1: parked rows from earlier pulls get one recovery attempt per
            // batch — parents that arrived since may have closed the gap. The
            // cursor already advanced past them; this pass is the only thing
            // standing between a parked row and permanent loss.
            applied += retryParksPass()
        } finally {
            SyncMigrationSql.captureTriggers.forEach { create -> executeSQL(create) }
        }

        runCatching { InventoryCostProjectionService(database).rebuild() }

        ApplyReport(
            applied = applied,
            updated = updated,
            skipped = skipped,
            parked = parked,
            conflicts = conflicts,
            latestServerSeq = records.maxOfOrNull { it.serverSeq } ?: 0L,
        )
    }

    private enum class PerRecord { APPLIED, UPDATED, PRESENT, SKIPPED, PARKED, CONFLICT }

    private suspend fun applyOne(record: SyncPulledRecord): PerRecord {
        val payload = record.payload
        // C1 pending-edit guard: a pulled row whose uid still has an unpushed
        // local edit is parked, never applied — the local edit must reach the
        // server first. Pushing it later clears the park (and sends the
        // parked_seq conflict hint so the overwritten remote version is
        // audited server-side).
        if (database.syncOutboxDao().countByTableAndUid(record.tableName, record.rowUid) > 0) {
            return park(record, "pending_local_edit")
        }
        return try {
            when (record.tableName) {
                "products" -> applyProduct(record.rowUid, payload)
                "accounts" -> applyAccount(record.rowUid, payload)
                "settings" -> applySetting(payload)
                "events" -> applyEvent(record)
                "deliveries" -> applyDelivery(record)
                "attendance_records" -> applyAttendance(record)
                "budget_plans", "collections_targets" -> PerRecord.SKIPPED // planning rows: pulled but not applied until planning sync lands
                "delivery_status_history" -> applyDeliveryStatus(record)
                "physical_cash_counts" -> applyCashCount(record)
                "daily_close_actions" -> applyCloseAction(record)
                "store_profile" -> applyStoreProfile(payload)
                else -> PerRecord.SKIPPED
            }
        } catch (failure: Throwable) {
            auditSkip(record, failure.message ?: failure.javaClass.simpleName)
            PerRecord.SKIPPED
        }
    }

    /**
     * C1 parking: keep a pulled row the engine cannot apply YET, so the pull
     * cursor can advance without losing it. Newest pulled version wins;
     * applying any version for the uid, or its dependency resolving, clears
     * the park (see [retryParksPass] and the apply paths).
     */
    private suspend fun park(record: SyncPulledRecord, reason: String): PerRecord {
        val existing = database.syncParkedRowDao().find(record.tableName, record.rowUid)
        if (existing == null || record.serverSeq >= existing.serverSeq) {
            database.syncParkedRowDao().upsert(
                com.saaf.milkshop.data.SyncParkedRowEntity(
                    tableName = record.tableName,
                    rowUid = record.rowUid,
                    payloadJson = record.payload.toString(),
                    serverSeq = record.serverSeq,
                    retryCount = existing?.retryCount ?: 0,
                    parkedAt = Instant.now().toString(),
                ),
            )
        }
        runCatching {
            database.securityAuditDao().insert(
                com.saaf.milkshop.data.SecurityAuditEntryEntity(
                    auditId = 0,
                    occurredAt = Instant.now().toString(),
                    action = "sync_apply_parked",
                    result = "warning",
                    reasonCode = reason,
                    details = "table=${record.tableName};uid=${record.rowUid};seq=${record.serverSeq}",
                ),
            )
        }
        return PerRecord.PARKED
    }

    /**
     * One recovery attempt per parked row per batch. Any resolved outcome
     * (applied, updated, already-present, sibling conflict) clears the park;
     * a re-park counts a retry, with a bounded number of attempts before the
     * park stops consuming effort (the row stays as evidence, audited once).
     * A park whose uid still has a pending local outbox edit defers without
     * consuming retries — only its push resolves it.
     */
    private suspend fun retryParksPass(): Int {
        var recovered = 0
        for (park in database.syncParkedRowDao().all()) {
            if (park.retryCount >= MAX_PARK_RETRIES) continue
            if (database.syncOutboxDao().countByTableAndUid(park.tableName, park.rowUid) > 0) continue
            val record = SyncPulledRecord(
                serverSeq = park.serverSeq,
                shopId = null,
                tableName = park.tableName,
                rowUid = park.rowUid,
                payload = JSONObject(park.payloadJson),
                originDevice = null,
            )
            val outcome = applyOne(record)
            when (outcome) {
                PerRecord.APPLIED, PerRecord.UPDATED, PerRecord.PRESENT, PerRecord.CONFLICT -> {
                    database.syncParkedRowDao().delete(park.tableName, park.rowUid)
                    if (outcome != PerRecord.PRESENT) recovered += 1
                }
                PerRecord.PARKED -> {
                    val attempts = park.retryCount + 1
                    database.syncParkedRowDao().upsert(park.copy(retryCount = attempts))
                    if (attempts == MAX_PARK_RETRIES) {
                        runCatching {
                            database.securityAuditDao().insert(
                                com.saaf.milkshop.data.SecurityAuditEntryEntity(
                                    auditId = 0,
                                    occurredAt = Instant.now().toString(),
                                    action = "sync_park_unresolved",
                                    result = "warning",
                                    reasonCode = "retries_exhausted",
                                    details = "table=${park.tableName};uid=${park.rowUid};seq=${park.serverSeq}",
                                ),
                            )
                        }
                    }
                }
                PerRecord.SKIPPED -> Unit // semantic refusal: keep the park as evidence, stop retrying it
            }
        }
        return recovered
    }

    private suspend fun applyProduct(uid: String, payload: JSONObject): PerRecord {
        val existing = database.syncEnvelopeDao().productsByUid(listOf(uid)).firstOrNull()
        val name = payload.getString("name")
        val unit = payload.getString("unit")
        return if (existing == null) {
            database.productDao().insert(ProductEntity(name = name, unit = unit, uid = uid))
            PerRecord.APPLIED
        } else {
            val stamp = payload.optString("sync_updated_at").ifEmpty { Instant.now().toString() }
            database.syncApplyDao().updateProductByUid(uid, name, unit, stamp)
            applyProfileChild(payload)
            PerRecord.UPDATED
        }
    }

    private suspend fun applyAccount(uid: String, payload: JSONObject): PerRecord {
        val existing = database.syncEnvelopeDao().accountsByUid(listOf(uid)).firstOrNull()
        val name = payload.getString("name")
        val type = payload.getString("type")
        val category = payload.getString("category")
        return if (existing == null) {
            val id = database.accountDao().insert(
                AccountEntity(name = name, type = type, category = category, uid = uid),
            )
            applyProfileChild(payload, id)
            PerRecord.APPLIED
        } else {
            val stamp = payload.optString("sync_updated_at").ifEmpty { Instant.now().toString() }
            database.syncApplyDao().updateAccountByUid(uid, name, type, category, stamp)
            applyProfileChild(payload, existing.accountId)
            PerRecord.UPDATED
        }
    }

    private suspend fun applySetting(payload: JSONObject): PerRecord {
        val key = payload.getString("setting_key")
        val value = payload.getDouble("setting_value")
        val stamp = payload.optString("updated_at").ifEmpty { Instant.now().toString() }
        val changed = database.syncApplyDao().updateSettingByKey(key, value, stamp)
        return if (changed == 0) {
            database.backupDataDao().insertSettings(listOf(SettingEntity(key, value, stamp)))
            PerRecord.APPLIED
        } else {
            PerRecord.UPDATED
        }
    }

    private suspend fun applyEvent(record: SyncPulledRecord): PerRecord {
        val uid = record.rowUid
        val payload = record.payload
        val existing = database.syncEnvelopeDao().eventsByUid(listOf(uid)).firstOrNull()
        if (existing != null) return PerRecord.PRESENT // events are immutable history

        val correctsUid = payload.optNullableString("corrects_event_uid")
        var correctsLocalId: Long? = null
        if (correctsUid != null) {
            val target = database.syncEnvelopeDao().eventsByUid(listOf(correctsUid)).firstOrNull()
                ?: return park(record, "correction target $correctsUid not local yet")
            // P1 sibling guard: the target already carries a different
            // correction — never double-count; leave the owner to resolve.
            val existingChild = database.syncEnvelopeDao().correctionsOf(target.eventId)
                .firstOrNull { it.eventUid != uid }
            if (existingChild != null) {
                auditConflict(uid, target.eventUid, existingChild.eventUid)
                return PerRecord.CONFLICT
            }
            correctsLocalId = target.eventId
        }

        // C1 parse-first: every ledger child resolves BEFORE anything is
        // inserted, so a missing product/account parks the whole event —
        // a header without its ledger rows can never be committed, and an
        // unresolvable child can never be silently dropped.
        val inventoryRows = ArrayList<Pair<Long, Double>>()
        val financialRows = ArrayList<Pair<Long, Double>>()
        val spoilageRows = ArrayList<Triple<Long, Double, String>>()
        payload.optJSONArray("inventory_logs")?.let { rows ->
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val productUid = row.optString("product_uid")
                val productId = resolveProduct(productUid)
                    ?: return park(record, "inventory product $productUid not local yet")
                inventoryRows += productId to row.getDouble("quantity")
            }
        }
        payload.optJSONArray("financial_logs")?.let { rows ->
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val accountUid = row.optString("account_uid")
                val accountId = resolveAccount(accountUid)
                    ?: return park(record, "financial account $accountUid not local yet")
                financialRows += accountId to row.getDouble("amount")
            }
        }
        payload.optJSONArray("spoilage_details")?.let { rows ->
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val productUid = row.optString("product_uid")
                val productId = resolveProduct(productUid)
                    ?: return park(record, "spoilage product $productUid not local yet")
                spoilageRows += Triple(productId, row.getDouble("quantity"), row.getString("reason"))
            }
        }

        val event = EventEntity(
            createdAt = payload.getString("created_at"),
            occurredAt = payload.getString("occurred_at"),
            eventType = payload.getString("event_type"),
            channel = payload.optNullableString("channel"),
            correctsEventId = correctsLocalId,
            rawLiters = payload.optDoubleOrNullCompat("raw_liters"),
            fatPercentage = payload.optDoubleOrNullCompat("fat_percentage"),
            lrReading = payload.optDoubleOrNullCompat("lr_reading"),
            snfPercentage = payload.optDoubleOrNullCompat("snf_percentage"),
            tsPercentage = payload.optDoubleOrNullCompat("ts_percentage"),
            adjustedLiters = payload.optDoubleOrNullCompat("adjusted_liters"),
            amountOwed = payload.optDoubleOrNullCompat("amount_owed"),
            milkLitersUsed = payload.optDoubleOrNullCompat("milk_liters_used"),
            yogurtKgProduced = payload.optDoubleOrNullCompat("yogurt_kg_produced"),
            amount = payload.optDoubleOrNullCompat("amount"),
            milkRateUsed = payload.optDoubleOrNullCompat("milk_rate_used"),
            deliveryFeeUsed = payload.optDoubleOrNullCompat("delivery_fee_used"),
            tsStandardUsed = payload.optDoubleOrNullCompat("ts_standard_used"),
            saleQuantity = payload.optDoubleOrNullCompat("sale_quantity"),
            saleValue = payload.optDoubleOrNullCompat("sale_value"),
            unitRate = payload.optDoubleOrNullCompat("unit_rate"),
            surchargeApplied = payload.optDouble("surcharge_applied", 0.0),
            notes = payload.optNullableString("notes"),
            eventUid = uid,
            originDeviceId = payload.optNullableString("origin_device_id") ?: "",
            rawDeviceTs = payload.optNullableString("raw_device_ts") ?: payload.getString("created_at"),
            trustedEstimatedAt = payload.optNullableString("trusted_estimated_at") ?: "",
            timeTrust = payload.optNullableString("time_trust") ?: EventTimeTrust.DEVICE_ONLY,
        )
        val eventId = database.eventDao().insert(event)

        inventoryRows.forEach { (productId, quantity) ->
            database.inventoryLogDao().insert(
                InventoryLogEntity(eventId = eventId, productId = productId, quantity = quantity),
            )
        }
        financialRows.forEach { (accountId, amount) ->
            database.financialLogDao().insert(
                FinancialLogEntity(eventId = eventId, accountId = accountId, amount = amount),
            )
        }
        spoilageRows.forEach { (productId, quantity, reason) ->
            database.spoilageDao().insert(
                SpoilageDetailEntity(
                    eventId = eventId,
                    productId = productId,
                    quantity = quantity,
                    reason = reason,
                ),
            )
        }

        // Actor attribution is best-effort cross-device: app users are
        // device-local, so a receiving device may have no matching user row.
        // The event and its ledger apply regardless; attribution appears
        // once the staff account exists locally (all-staff linking).
        payload.optJSONObject("actor_link")?.let { link ->
            runCatching {
                database.eventActorLinkDao().insert(
                    EventActorLinkEntity(
                        eventId = eventId,
                        actorUserId = 0L,
                        actorNameSnapshot = link.optNullableString("actor_name_snapshot") ?: "synced",
                        actorRoleSnapshot = link.optNullableString("actor_role_snapshot") ?: "synced",
                        attributedAt = link.optNullableString("attributed_at") ?: Instant.now().toString(),
                    ),
                )
            }
        }
        return PerRecord.APPLIED
    }

    private suspend fun resolveProduct(uid: String): Long? =
        if (uid.isEmpty()) null
        else database.syncEnvelopeDao().productsByUid(listOf(uid)).firstOrNull()?.productId

    private suspend fun resolveAccount(uid: String): Long? =
        if (uid.isEmpty()) null
        else database.syncEnvelopeDao().accountsByUid(listOf(uid)).firstOrNull()?.accountId

    private suspend fun resolveEvent(uid: String): Long? =
        if (uid.isEmpty()) null
        else database.syncEnvelopeDao().eventsByUid(listOf(uid)).firstOrNull()?.eventId

    private suspend fun resolveDelivery(uid: String): Long? =
        if (uid.isEmpty()) null
        else database.syncEnvelopeDao().deliveriesByUid(listOf(uid)).firstOrNull()?.deliveryId

    private suspend fun resolveCashCount(uid: String): Long? =
        if (uid.isEmpty()) null
        else database.syncEnvelopeDao().physicalCashCountsByUid(listOf(uid)).firstOrNull()?.countId

    private suspend fun applyDelivery(record: SyncPulledRecord): PerRecord {
        val uid = record.rowUid
        val payload = record.payload
        val customerUid = payload.optString("customer_uid")
        val productUid = payload.optString("product_uid")
        val customerId = resolveAccount(customerUid) ?: return park(record, "delivery customer $customerUid not local yet")
        val productId = resolveProduct(productUid) ?: return park(record, "delivery product $productUid not local yet")
        val saleId = payload.optNullableString("sale_event_uid")?.let { resolveEvent(it) }
        val existing = database.syncEnvelopeDao().deliveriesByUid(listOf(uid)).firstOrNull()
        return if (existing == null) {
            database.deliveryDao().insert(
                DeliveryEntity(
                    customerAccountId = customerId,
                    productId = productId,
                    quantity = payload.getDouble("quantity"),
                    route = payload.optNullableString("route"),
                    businessDate = payload.getString("business_date"),
                    saleEventId = saleId,
                    notes = payload.optNullableString("notes"),
                    createdAt = payload.getString("created_at"),
                    archived = payload.optBoolean("archived", false),
                    uid = uid,
                ),
            )
            PerRecord.APPLIED
        } else {
            database.syncApplyDao().updateDeliveryByUid(
                uid = uid,
                customerAccountId = customerId,
                productId = productId,
                quantity = payload.getDouble("quantity"),
                route = payload.optNullableString("route"),
                businessDate = payload.getString("business_date"),
                saleEventId = saleId,
                notes = payload.optNullableString("notes"),
                archived = payload.optBoolean("archived", false),
                stamp = payload.optString("sync_updated_at").ifEmpty { Instant.now().toString() },
            )
            PerRecord.UPDATED
        }
    }

    private suspend fun applyAttendance(record: SyncPulledRecord): PerRecord {
        val uid = record.rowUid
        val payload = record.payload
        val employeeUid = payload.optString("employee_uid")
        val employeeId = resolveAccount(employeeUid) ?: return park(record, "attendance employee $employeeUid not local yet")
        val supersedesUid = payload.optNullableString("supersedes_uid")
        // A present-but-unresolvable supersedes is a dependency, not a null
        // (C1: silent nulls made correction chains diverge across devices).
        val supersedes = supersedesUid?.let {
            database.syncEnvelopeDao().attendanceByUid(listOf(it)).firstOrNull()?.attendanceId
                ?: return park(record, "attendance supersedes $it not local yet")
        }
        val existing = database.syncEnvelopeDao().attendanceByUid(listOf(uid)).firstOrNull()
        return if (existing == null) {
            database.attendanceDao().insert(
                AttendanceRecordEntity(
                    employeeAccountId = employeeId,
                    businessDate = payload.getString("business_date"),
                    status = payload.getString("status"),
                    notes = payload.optNullableString("notes"),
                    createdAt = payload.getString("created_at"),
                    supersedesId = supersedes,
                    uid = uid,
                ),
            )
            PerRecord.APPLIED
        } else {
            database.syncApplyDao().updateAttendanceByUid(
                uid = uid,
                employeeAccountId = employeeId,
                businessDate = payload.getString("business_date"),
                status = payload.getString("status"),
                notes = payload.optNullableString("notes"),
                supersedesId = supersedes,
                archived = payload.optBoolean("archived", false),
                stamp = payload.optString("sync_updated_at").ifEmpty { Instant.now().toString() },
            )
            PerRecord.UPDATED
        }
    }

    private suspend fun applyDeliveryStatus(record: SyncPulledRecord): PerRecord {
        val uid = record.rowUid
        val payload = record.payload
        val deliveryUid = payload.optString("delivery_uid")
        val deliveryId = resolveDelivery(deliveryUid) ?: return park(record, "status delivery $deliveryUid not local yet")
        val existing = database.syncEnvelopeDao().deliveryStatusesByUid(listOf(uid)).firstOrNull()
        if (existing != null) return PerRecord.PRESENT
        database.deliveryDao().insertStatus(
            com.saaf.milkshop.data.DeliveryStatusEntity(
                deliveryId = deliveryId,
                status = payload.getString("status"),
                occurredAt = payload.getString("occurred_at"),
                notes = payload.optNullableString("notes"),
                uid = uid,
            ),
        )
        return PerRecord.APPLIED
    }

    private suspend fun applyCashCount(record: SyncPulledRecord): PerRecord {
        val uid = record.rowUid
        val payload = record.payload
        if (database.syncEnvelopeDao().physicalCashCountsByUid(listOf(uid)).isNotEmpty()) return PerRecord.PRESENT
        val accountUid = payload.optString("account_uid")
        val accountId = resolveAccount(accountUid) ?: return park(record, "cash count account $accountUid not local yet")
        database.backupDataDao().insertPhysicalCashCount(
            PhysicalCashCountEntity(
                accountId = accountId,
                businessDate = payload.getString("business_date"),
                countedAmount = payload.getDouble("counted_amount"),
                countedAt = payload.getString("counted_at"),
                notes = payload.optNullableString("notes"),
                uid = uid,
            ),
        )
        return PerRecord.APPLIED
    }

    private suspend fun applyCloseAction(record: SyncPulledRecord): PerRecord {
        val uid = record.rowUid
        val payload = record.payload
        if (database.syncEnvelopeDao().dailyCloseActionsByUid(listOf(uid)).isNotEmpty()) return PerRecord.PRESENT
        // A present-but-unresolvable count link is a dependency, not a null
        // (C1: silent nulls made close/count associations diverge).
        val countUid = payload.optNullableString("count_uid")
        val countId = countUid?.let {
            resolveCashCount(it) ?: return park(record, "close action count $it not local yet")
        }
        val supersedesUid = payload.optNullableString("supersedes_action_uid")
        val supersedesId = supersedesUid?.let {
            database.syncEnvelopeDao().dailyCloseActionsByUid(listOf(it)).firstOrNull()?.actionId
                ?: return park(record, "close action supersedes $it not local yet")
        }
        database.backupDataDao().insertDailyCloseAction(
            DailyCloseActionEntity(
                businessDate = payload.getString("business_date"),
                action = payload.getString("action"),
                physicalCashCountId = countId,
                actorName = payload.optString("actor_name").ifEmpty { "synced" },
                actorRole = payload.optString("actor_role").ifEmpty { "synced" },
                reason = payload.optNullableString("reason"),
                createdAt = payload.getString("created_at"),
                supersedesActionId = supersedesId,
                uid = uid,
            ),
        )
        return PerRecord.APPLIED
    }

    private suspend fun applyStoreProfile(payload: JSONObject): PerRecord {
        val existing = database.syncEnvelopeDao().storeProfile()
        val entity = com.saaf.milkshop.data.StoreProfileEntity(
            storeProfileId = existing?.storeProfileId ?: 1,
            storeName = payload.getString("store_name"),
            currencyCode = payload.getString("currency_code"),
            phone = payload.optNullableString("phone"),
            address = payload.optNullableString("address"),
            updatedAt = payload.getString("updated_at"),
        )
        database.storeProfileDao().save(entity)
        return if (existing == null) PerRecord.APPLIED else PerRecord.UPDATED
    }

    private suspend fun applyProfileChild(payload: JSONObject, accountId: Long? = null) {
        val profile = payload.optJSONObject("profile") ?: return
        when (payload.optString("category")) {
            "Customer" -> {
                val existing = database.customerProfileDao().findByAccountId(accountId ?: return)
                if (existing == null) {
                    database.customerProfileDao().save(
                        com.saaf.milkshop.data.CustomerProfileEntity(
                            accountId = accountId,
                            phone = profile.optNullableString("phone"),
                            notes = profile.optNullableString("notes"),
                            createdAt = profile.getString("created_at"),
                        ),
                    )
                }
            }
            "Vendor", "Salary", "Operating Expense" -> {
                // Profile satellites for these categories ride along later;
                // applying the account row is what the ledger needs.
            }
        }
    }

    private suspend fun auditSkip(record: SyncPulledRecord, reason: String) {
        runCatching {
            database.securityAuditDao().insert(
                com.saaf.milkshop.data.SecurityAuditEntryEntity(
                    auditId = 0,
                    occurredAt = Instant.now().toString(),
                    action = "sync_apply_skipped",
                    result = "warning",
                    reasonCode = "skipped",
                    details = "table=${record.tableName};uid=${record.rowUid};seq=${record.serverSeq};reason=$reason",
                ),
            )
        }
    }

    private suspend fun auditConflict(incomingUid: String, targetUid: String, existingUid: String) {
        runCatching {
            database.securityAuditDao().insert(
                com.saaf.milkshop.data.SecurityAuditEntryEntity(
                    auditId = 0,
                    occurredAt = Instant.now().toString(),
                    action = "sync_apply_conflict",
                    result = "warning",
                    reasonCode = "sibling_correction",
                    details = "incoming=$incomingUid;target=$targetUid;existing_child=$existingUid",
                ),
            )
        }
    }

    private companion object {
        const val MAX_PARK_RETRIES = 5
    }
}

private fun JSONObject.optDoubleOrNullCompat(name: String): Double? =
    if (!has(name) || isNull(name)) null else getDouble(name)

/** org.json's optString returns the literal "null" for JSON null; read honestly. */
private fun JSONObject.optNullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }

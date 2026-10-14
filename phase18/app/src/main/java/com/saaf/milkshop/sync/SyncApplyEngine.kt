package com.saaf.milkshop.sync

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.SyncMigrationSql
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.CollectionsTargetEntity
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
import com.saaf.milkshop.data.ProductProfileEntity
import com.saaf.milkshop.data.CustomerProfileEntity
import com.saaf.milkshop.data.VendorProfileEntity
import com.saaf.milkshop.data.EmployeeProfileEntity
import com.saaf.milkshop.data.ExpenseCategoryProfileEntity
import com.saaf.milkshop.data.SettingEntity
import com.saaf.milkshop.data.SpoilageDetailEntity
import com.saaf.milkshop.domain.InventoryCostProjectionService
import com.saaf.milkshop.domain.OwnerFundsTruth
import com.saaf.milkshop.data.OwnerFundsDetailEntity
import com.saaf.milkshop.data.OpeningBatchEntity
import com.saaf.milkshop.data.OpeningInventoryEntryEntity
import com.saaf.milkshop.data.OpeningAccountEntryEntity
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.LocalDate
import java.time.DayOfWeek
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.OPERATING_BUDGET_CATEGORIES

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
        val present: Int,
    )

    suspend fun apply(records: List<SyncPulledRecord>): ApplyReport = database.withWriteTransaction {
        var applied = 0
        var updated = 0
        var skipped = 0
        var parked = 0
        var conflicts = 0
        var present = 0

        // Capture triggers stay down for the whole batch: pulled rows are
        // server-originated history, not new device writes.
        SyncMigrationSql.triggerNames.forEach { name -> executeSQL("DROP TRIGGER IF EXISTS $name") }
        try {
            for (record in records) {
                // Per-record savepoint: a mid-record failure rolls back to a
                // clean boundary so a half-applied record (header without its
                // ledger children) can never commit — idempotent re-apply of
                // a skipped uid would otherwise never repair it.
                val outcome = applyAtomically(record) { executeSQL(it) }
                when (outcome) {
                    PerRecord.APPLIED -> applied += 1
                    PerRecord.UPDATED -> updated += 1
                    PerRecord.PRESENT -> present += 1
                    PerRecord.SKIPPED -> skipped += 1
                    PerRecord.PARKED -> parked += 1
                    PerRecord.CONFLICT -> conflicts += 1
                }
            }
            // C1: parked rows from earlier pulls get one recovery attempt per
            // batch — parents that arrived since may have closed the gap. The
            // cursor already advanced past them; this pass is the only thing
            // standing between a parked row and permanent loss.
            applied += retryParksPass { executeSQL(it) }
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
            present = present,
        )
    }

    private enum class PerRecord { APPLIED, UPDATED, PRESENT, SKIPPED, PARKED, CONFLICT }

    /** Errors must cross the savepoint boundary before being turned into refusals. */
    private suspend fun applyAtomically(record: SyncPulledRecord, execute: suspend (String) -> Unit): PerRecord {
        execute("SAVEPOINT record_apply")
        return try {
            val result = applyOne(record, execute)
            if ((record.tableName == "daily_close_actions" ||
                (record.tableName == "events" && record.payload.optString("event_type") in setOf("OWNER_DRAWING", "OWNER_FUNDS_IN")))
                && result in listOf(PerRecord.APPLIED, PerRecord.UPDATED, PerRecord.PRESENT)) {
                // A verified current body supersedes an older retained refusal of this UID.
                // Never discard a later server version while accepting an earlier one.
                val retained = database.syncParkedRowDao().find(record.tableName, record.rowUid)
                if (retained != null && retained.serverSeq <= record.serverSeq) {
                    database.syncParkedRowDao().delete(record.tableName, record.rowUid)
                }
            }
            execute("RELEASE record_apply")
            result
        } catch (failure: Throwable) {
            execute("ROLLBACK TO record_apply")
            execute("RELEASE record_apply")
            if (failure is kotlinx.coroutines.CancellationException || failure is AuditPersistenceFailure) throw failure
            if (record.tableName == "daily_close_actions") {
                return park(record, "daily_close_refused: ${failure.message ?: failure.javaClass.simpleName}")
            }
            if (record.tableName == "events" && (record.payload.optString("event_type") in setOf("OWNER_DRAWING", "OWNER_FUNDS_IN")
                    || record.payload.has("owner_funds_contract_version") || record.payload.has("owner_funds_details"))) {
                return park(record, "owner_funds_refused: ${failure.message ?: failure.javaClass.simpleName}")
            }
            auditSkip(record, failure.message ?: failure.javaClass.simpleName)
            PerRecord.SKIPPED
        }
    }

    private suspend fun applyOne(record: SyncPulledRecord, execute: suspend (String) -> Unit): PerRecord {
        val payload = record.payload
        // C1 pending-edit guard: a pulled row whose uid still has an unpushed
        // local edit is parked, never applied — the local edit must reach the
        // server first. Pushing it later clears the park (and sends the
        // parked_seq conflict hint so the overwritten remote version is
        // audited server-side).
        if (database.syncOutboxDao().countByTableAndUid(record.tableName, record.rowUid) > 0) {
            return park(record, "pending_local_edit")
        }
        return when (record.tableName) {
                "products" -> applyProduct(record.rowUid, payload)
                "accounts" -> applyAccount(record.rowUid, payload)
                "settings" -> applySetting(payload)
                "events" -> applyEvent(record)
                "deliveries" -> applyDelivery(record)
                "attendance_records" -> applyAttendance(record)
                "budget_plans" -> applyBudget(record)
                "collections_targets" -> applyCollectionsTarget(record)
                "delivery_status_history" -> applyDeliveryStatus(record)
                "physical_cash_counts" -> applyCashCount(record)
                "daily_close_actions" -> applyCloseAction(record, execute)
                "store_profile" -> applyStoreProfile(payload)
                else -> PerRecord.SKIPPED
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
                    // Review finding (go 7): a re-park is a FRESH pulled chance
                    // to resolve — an exhausted older park must not strand its
                    // newer version above the retry ceiling. The bounded-retry
                    // accounting itself lives in retryParksPass, which
                    // overwrites this via park.copy(retryCount = attempts).
                    retryCount = 0,
                    parkedAt = Instant.now().toString(),
                ),
            )
        }
        persistAudit(
                com.saaf.milkshop.data.SecurityAuditEntryEntity(
                    auditId = 0,
                    occurredAt = Instant.now().toString(),
                    action = "sync_apply_parked",
                    result = "warning",
                    reasonCode = reason,
                    details = "table=${record.tableName};uid=${record.rowUid};seq=${record.serverSeq}",
                ),
        )
        return PerRecord.PARKED
    }

    /**
     * One recovery attempt per parked row per batch. Any resolved outcome
     * (applied, updated, already-present, sibling conflict) clears the park;
     * a re-park counts a retry up to the diagnostic ceiling (audited once).
     * Later batches still attempt recovery, including after a parent arrives.
     * A park whose uid still has a pending local outbox edit defers without
     * consuming retries — only its push resolves it.
     */
    private suspend fun retryParksPass(execute: suspend (String) -> Unit): Int {
        var recovered = 0
        for (park in database.syncParkedRowDao().all()) {
            if (database.syncOutboxDao().countByTableAndUid(park.tableName, park.rowUid) > 0) continue
            val record = SyncPulledRecord(
                serverSeq = park.serverSeq,
                shopId = null,
                tableName = park.tableName,
                rowUid = park.rowUid,
                payload = JSONObject(park.payloadJson),
                originDevice = null,
            )
            val outcome = applyAtomically(record, execute)
            when (outcome) {
                PerRecord.APPLIED, PerRecord.UPDATED, PerRecord.PRESENT, PerRecord.CONFLICT -> {
                    database.syncParkedRowDao().delete(park.tableName, park.rowUid)
                    if (outcome != PerRecord.PRESENT) recovered += 1
                }
                PerRecord.PARKED -> {
                    // Bound each pass to one attempt, not the row's lifetime:
                    // a dependency can arrive after any number of other batches.
                    val attempts = (park.retryCount + 1).coerceAtMost(MAX_PARK_RETRIES)
                    database.syncParkedRowDao().upsert(park.copy(retryCount = attempts))
                    if (park.retryCount < MAX_PARK_RETRIES && attempts == MAX_PARK_RETRIES) {
                        persistAudit(
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
                PerRecord.SKIPPED -> Unit // semantic refusal: keep the park as evidence, stop retrying it
            }
        }
        return recovered
    }

    private suspend fun applyProduct(uid: String, payload: JSONObject): PerRecord {
        val existing = database.syncEnvelopeDao().productsByUid(listOf(uid)).firstOrNull()
        val name = payload.getString("name")
        val unit = payload.getString("unit")
        require(name.isNotBlank() && !name.contains('\n')) { "Invalid product name" }
        require(unit == "Liters" || unit == "Kg") { "Product unit must be Liters or Kg" }
        return if (existing == null) {
            val id = database.productDao().insert(ProductEntity(name = name, unit = unit, uid = uid))
            applyProductProfile(payload, id)
            PerRecord.APPLIED
        } else {
            val stamp = payload.optString("sync_updated_at").ifEmpty { Instant.now().toString() }
            database.syncApplyDao().updateProductByUid(uid, name, unit, stamp)
            applyProductProfile(payload, existing.productId)
            PerRecord.UPDATED
        }
    }

    private suspend fun applyAccount(uid: String, payload: JSONObject): PerRecord {
        val existing = database.syncEnvelopeDao().accountsByUid(listOf(uid)).firstOrNull()
        val name = payload.getString("name")
        val type = payload.getString("type")
        val category = payload.getString("category")
        val expectedType = when (category) {
            "Cash/Bank", "Customer" -> "Asset"
            "Vendor" -> "Liability"
            "Operating Expense", "Salary" -> "Expense"
            else -> error("Invalid account category")
        }
        require(type == expectedType) { "Account type/category combination is invalid" }
        return if (existing == null) {
            val id = database.accountDao().insert(
                AccountEntity(name = name, type = type, category = category, uid = uid),
            )
            applyAccountProfile(payload, category, id)
            PerRecord.APPLIED
        } else {
            val stamp = payload.optString("sync_updated_at").ifEmpty { Instant.now().toString() }
            database.syncApplyDao().updateAccountByUid(uid, name, type, category, stamp)
            applyAccountProfile(payload, category, existing.accountId)
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
                if (payload.optString("event_type") in setOf("OWNER_DRAWING", "OWNER_FUNDS_IN")) return park(record, "owner_funds_sibling_correction")
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
                val row = rows.getJSONObject(index)
                val productUid = row.optString("product_uid")
                val productId = resolveProduct(productUid)
                    ?: return park(record, "inventory product $productUid not local yet")
                inventoryRows += productId to row.getDouble("quantity")
            }
        }
        payload.optJSONArray("financial_logs")?.let { rows ->
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val accountUid = row.optString("account_uid")
                val accountId = resolveAccount(accountUid)
                    ?: return park(record, "financial account $accountUid not local yet")
                financialRows += accountId to row.getDouble("amount")
            }
        }
        payload.optJSONArray("spoilage_details")?.let { rows ->
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val productUid = row.optString("product_uid")
                val productId = resolveProduct(productUid)
                    ?: return park(record, "spoilage product $productUid not local yet")
                spoilageRows += Triple(productId, row.getDouble("quantity"), row.getString("reason"))
            }
        }

        // Legacy bodies may lack details; explicit new bodies must be complete.
        val openingObject = payload.optJSONObject("opening_state")
        require(!payload.has("opening_state") || openingObject != null) { "Opening state must be an object" }
        require((openingObject == null && !payload.has("opening_state_contract_version")) ||
            (openingObject != null && payload.getInt("opening_state_contract_version") == 1)) { "Unsupported opening state contract" }
        require(openingObject == null || payload.getString("event_type") == "OPENING_STATE") { "Opening details belong to an opening event" }
        val openingInventory = ArrayList<Pair<Long, Double>>()
        val openingAccounts = ArrayList<Pair<Long, Double>>()
        openingObject?.let { details ->
            val inventory = details.getJSONArray("inventory_entries")
            for (index in 0 until inventory.length()) {
                val row = inventory.getJSONObject(index)
                val productUid = row.requiredText("product_uid")
                val id = resolveProduct(productUid) ?: return park(record, "opening product $productUid not local yet")
                val quantity = row.requiredFiniteNumber("quantity")
                require(quantity >= 0 && openingInventory.none { it.first == id }) { "Invalid or repeated opening product" }
                openingInventory += id to quantity
            }
            val accounts = details.getJSONArray("account_entries")
            for (index in 0 until accounts.length()) {
                val row = accounts.getJSONObject(index)
                val accountUid = row.requiredText("account_uid")
                val id = resolveAccount(accountUid) ?: return park(record, "opening account $accountUid not local yet")
                val balance = row.requiredFiniteNumber("balance")
                require(balance >= 0 && openingAccounts.none { it.first == id }) { "Invalid or repeated opening account" }
                require(database.accountDao().findById(id)!!.category in setOf("Cash/Bank", "Customer", "Vendor")) { "Invalid opening account category" }
                openingAccounts += id to balance
            }
        }
        val actorObject = payload.optJSONObject("actor_link")
        require(!payload.has("actor_link") || actorObject != null) { "Actor snapshot must be an object" }
        val actor = actorObject?.let { link ->
            val time = link.requiredText("attributed_at").also { Instant.parse(it) }
            EventActorLinkEntity(0, null, link.requiredText("actor_name_snapshot"), link.requiredText("actor_role_snapshot"), time)
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
        var ownerDetail: OwnerFundsDetailEntity? = null
        var ownerParent: EventEntity? = null
        if (event.eventType in setOf("OWNER_DRAWING", "OWNER_FUNDS_IN")) {
            ownerParent = correctsLocalId?.let { database.eventDao().findById(it)!! }
            val prior = ownerParent?.let { OwnerFundsTruth(database).resolve(it) }
            val explicit = payload.optJSONObject("owner_funds_details")
            require(!payload.has("owner_funds_details") || explicit != null) { "Owner funds details are malformed" }
            require((explicit == null && !payload.has("owner_funds_contract_version")) ||
                (explicit != null && payload.getInt("owner_funds_contract_version") in 1..2)) { "Unsupported owner funds contract" }
            require(event.eventType != "OWNER_FUNDS_IN" ||
                (explicit != null && payload.getInt("owner_funds_contract_version") == 2)) { "Receipt details need contract version 2" }
            ownerDetail = if (explicit != null) {
                val accountUid = explicit.getString("payment_account_uid")
                val accountId = resolveAccount(accountUid) ?: return park(record, "withdrawal account $accountUid not local yet")
                val rootUid = explicit.getString("withdrawal_root_event_uid")
                val rootId = if (rootUid == uid && ownerParent == null && event.eventType == "OWNER_DRAWING") existing?.eventId ?: 0L
                    else resolveEvent(rootUid) ?: return park(record, "withdrawal root $rootUid not local yet")
                OwnerFundsDetailEntity(existing?.eventId ?: 0L, accountId, rootId,
                    explicit.optDoubleOrNullCompat("reviewed_withdrawal_amount"),
                    explicit.optDoubleOrNullCompat("reviewed_received_total"),
                    explicit.optBoolean("excess_acknowledged", false))
            } else if (prior != null) prior.copy(eventId = existing?.eventId ?: 0L)
            else OwnerFundsDetailEntity(existing?.eventId ?: 0L, financialRows.singleOrNull()?.first
                ?: error("Legacy withdrawal account is ambiguous"), existing?.eventId ?: 0L)
            OwnerFundsTruth(database).validate(event, ownerDetail, ownerParent, prior, financialRows,
                inventoryRows.isNotEmpty() || spoilageRows.isNotEmpty())
        } else require(!payload.has("owner_funds_details") && !payload.has("owner_funds_contract_version")) {
            "Owner funds details cannot belong to another event type"
        }
        val storedOwnerDetail = existing?.let { database.ownerFundsDao().find(it.eventId) }
        if (storedOwnerDetail != null) require(storedOwnerDetail == ownerDetail) { "Existing withdrawal details differ from server history" }
        // Prior builds could commit a header or a prefix of children after
        // a child failed. Repair only an identical header and matching child
        // subset; never replace immutable history with a different server body.
        if (existing != null) {
            require(existing.copy(eventId = 0) == event) { "Existing event header differs from server history" }
            subtractExisting(inventoryRows, database.inventoryLogDao().findByEventId(existing.eventId).map { it.productId to it.quantity })
            subtractExisting(financialRows, database.financialLogDao().findByEventId(existing.eventId).map { it.accountId to it.amount })
            subtractExisting(spoilageRows, database.syncEnvelopeDao().spoilageDetailsForEvents(listOf(existing.eventId)).map { Triple(it.productId, it.quantity, it.reason) })
            if (inventoryRows.isEmpty() && financialRows.isEmpty() && spoilageRows.isEmpty()
                && (ownerDetail == null || storedOwnerDetail != null)) {
                val repaired = persistEventDetails(existing.eventId, openingObject != null, openingInventory, openingAccounts, actor)
                return if (repaired) PerRecord.UPDATED else PerRecord.PRESENT
            }
        }
        val eventId = existing?.eventId ?: database.eventDao().insert(event)
        ownerDetail?.let { detail ->
            ownerParent?.let { OwnerFundsTruth(database).ensureDetails(it) }
            if (storedOwnerDetail == null) database.ownerFundsDao().insert(detail.copy(eventId = eventId,
                withdrawalRootEventId = if (ownerParent == null && event.eventType == "OWNER_DRAWING") eventId else detail.withdrawalRootEventId))
        }

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
        if (ownerDetail != null) OwnerFundsTruth(database).resolve(database.eventDao().findById(eventId)!!)
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

        persistEventDetails(eventId, openingObject != null, openingInventory, openingAccounts, actor)
        return PerRecord.APPLIED
    }

    /** Details share the event savepoint. Replay repairs missing subsets but never rewrites history. */
    private suspend fun persistEventDetails(
        eventId: Long,
        hasOpening: Boolean,
        inventory: List<Pair<Long, Double>>,
        accounts: List<Pair<Long, Double>>,
        actor: EventActorLinkEntity?,
    ): Boolean {
        var changed = false
        if (hasOpening) {
            // Full restated values must agree with cumulative opening ledger
            // deltas, including corrections whose older bodies had no details.
            val ledgerInventory = mutableMapOf<Long, Double>()
            val ledgerAccounts = mutableMapOf<Long, Double>()
            var id: Long? = eventId
            val seen = mutableSetOf<Long>()
            while (id != null) {
                require(seen.add(id)) { "Opening correction cycle" }
                val event = requireNotNull(database.eventDao().findById(id))
                require(event.eventType == "OPENING_STATE") { "Opening correction needs an opening parent" }
                database.inventoryLogDao().findByEventId(id).forEach { row ->
                    ledgerInventory[row.productId] = (ledgerInventory[row.productId] ?: 0.0) + row.quantity
                }
                database.financialLogDao().findByEventId(id).forEach { row ->
                    ledgerAccounts[row.accountId] = (ledgerAccounts[row.accountId] ?: 0.0) + row.amount
                }
                id = event.correctsEventId
            }
            fun agrees(details: Map<Long, Double>, ledger: Map<Long, Double>): Boolean =
                (details.keys + ledger.keys).all { key ->
                    val a = details[key] ?: 0.0
                    val b = ledger[key] ?: 0.0
                    a.isFinite() && b.isFinite() && kotlin.math.abs(a - b) <=
                        maxOf(1e-9, maxOf(Math.ulp(a), Math.ulp(b)) * maxOf(4, seen.size * 2))
                }
            require(agrees(inventory.toMap(), ledgerInventory) && agrees(accounts.toMap(), ledgerAccounts)) {
                "Opening details differ from ledger history"
            }
            val dao = database.openingStateDao()
            val missingInventory = inventory.toMutableList()
            val missingAccounts = accounts.toMutableList()
            subtractExisting(missingInventory, dao.inventoryEntries(eventId).map { it.productId to it.quantity })
            subtractExisting(missingAccounts, dao.accountEntries(eventId).map { it.accountId to it.balance })
            if (dao.findBatch(eventId) == null) {
                dao.insertBatch(OpeningBatchEntity(eventId))
                changed = true
            }
            if (missingInventory.isNotEmpty()) {
                dao.insertInventoryEntries(missingInventory.map { OpeningInventoryEntryEntity(eventId, it.first, it.second) })
                changed = true
            }
            if (missingAccounts.isNotEmpty()) {
                dao.insertAccountEntries(missingAccounts.map { OpeningAccountEntryEntity(eventId, it.first, it.second) })
                changed = true
            }
        }
        if (actor != null) {
            val incoming = actor.copy(eventId = eventId)
            val stored = database.eventActorLinkDao().find(eventId)
            if (stored == null) {
                database.eventActorLinkDao().insert(incoming)
                changed = true
            } else {
                require(stored.copy(actorUserId = null) == incoming) { "Existing actor snapshot differs from server history" }
            }
        }
        return changed
    }

    private fun <T> subtractExisting(expected: MutableList<T>, existing: List<T>) {
        existing.forEach { child ->
            require(expected.remove(child)) { "Existing ledger child differs from server history" }
        }
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
        val saleId = payload.optNullableString("sale_event_uid")?.let {
            resolveEvent(it) ?: return park(record, "delivery sale $it not local yet")
        }
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

    private suspend fun applyBudget(record: SyncPulledRecord): PerRecord {
        val payload = record.payload
        val uid = record.rowUid
        val category = payload.getString("category")
        require(category == BUDGET_CATEGORY_MILK_PURCHASES || category in OPERATING_BUDGET_CATEGORIES) {
            "Unknown budget category"
        }
        val start = LocalDate.parse(payload.getString("period_start"))
        val end = LocalDate.parse(payload.getString("period_end_exclusive"))
        require(if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
            start.dayOfWeek == DayOfWeek.MONDAY && end == start.plusDays(7)
        } else {
            start.dayOfMonth == 1 && end == start.plusMonths(1)
        }) { "Invalid budget period" }
        val liters = payload.optionalFiniteNumber("planned_liters")
        val amount = payload.optionalFiniteNumber("planned_amount")
        val vendorRate = payload.optionalFiniteNumber("vendor_rate_snapshot")
        val retailRate = payload.optionalFiniteNumber("retail_rate_snapshot")
        require(listOfNotNull(liters, amount, vendorRate, retailRate).all { it >= 0.0 }) {
            "Negative budget amount or rate"
        }
        require(if (category == BUDGET_CATEGORY_MILK_PURCHASES) liters != null && amount != null
            else amount != null) { "Incomplete budget values" }
        val parentUid = payload.planningParentUid()
        val parent = parentUid?.let {
            database.syncEnvelopeDao().budgetsByUid(listOf(it)).firstOrNull()
                ?: return park(record, "budget supersedes $it not local yet")
        }
        if (parent != null) {
            require(parent.category == category && parent.periodStart == start.toString()) {
                "Budget revision changes category or period"
            }
            if (!parent.archived) return park(record, "budget parent $parentUid not archived yet")
            if (database.syncEnvelopeDao().otherBudgetRevision(parent.budgetId, uid) != null) {
                auditPlanningConflict(record, parentUid!!)
                return PerRecord.CONFLICT
            }
        }
        val existing = database.syncEnvelopeDao().budgetsByUid(listOf(uid)).firstOrNull()
        val incoming = BudgetPlanEntity(
            budgetId = existing?.budgetId ?: 0,
            category = category,
            periodStart = start.toString(),
            periodEndExclusive = end.toString(),
            plannedLiters = liters,
            plannedAmount = amount,
            vendorRateSnapshot = vendorRate,
            retailRateSnapshot = retailRate,
            createdAt = payload.getString("created_at").also { Instant.parse(it) },
            supersedesId = parent?.budgetId,
            archived = payload.getBoolean("archived"),
            uid = uid,
            syncUpdatedAt = payload.optString("sync_updated_at"),
        )
        if (!incoming.archived && database.budgetPlanDao().findActive(category, start.toString())
                ?.uid?.let { it != uid } == true) {
            auditPlanningConflict(record, parentUid ?: "independent_root")
            return PerRecord.CONFLICT
        }
        if (existing == null) {
            database.budgetPlanDao().insert(incoming)
            return PerRecord.APPLIED
        }
        require(existing.copy(archived = incoming.archived, syncUpdatedAt = incoming.syncUpdatedAt) == incoming) {
            "Budget history changed in place"
        }
        if (existing == incoming) return PerRecord.PRESENT
        database.syncApplyDao().updateBudgetStateByUid(uid, incoming.archived, incoming.syncUpdatedAt)
        return PerRecord.UPDATED
    }

    private suspend fun applyCollectionsTarget(record: SyncPulledRecord): PerRecord {
        val payload = record.payload
        val uid = record.rowUid
        val start = LocalDate.parse(payload.getString("week_start"))
        val end = LocalDate.parse(payload.getString("week_end_exclusive"))
        require(start.dayOfWeek == DayOfWeek.MONDAY && end == start.plusDays(7)) { "Invalid collections week" }
        val amount = payload.requiredFiniteNumber("target_amount")
        require(amount >= 0.0) { "Negative collections target" }
        val parentUid = payload.planningParentUid()
        val parent = parentUid?.let {
            database.syncEnvelopeDao().targetsByUid(listOf(it)).firstOrNull()
                ?: return park(record, "collections target supersedes $it not local yet")
        }
        if (parent != null) {
            require(parent.weekStart == start.toString()) { "Collections revision changes week" }
            if (!parent.archived) return park(record, "collections parent $parentUid not archived yet")
            if (database.syncEnvelopeDao().otherCollectionsRevision(parent.targetId, uid) != null) {
                auditPlanningConflict(record, parentUid!!)
                return PerRecord.CONFLICT
            }
        }
        val existing = database.syncEnvelopeDao().targetsByUid(listOf(uid)).firstOrNull()
        val incoming = CollectionsTargetEntity(
            targetId = existing?.targetId ?: 0,
            weekStart = start.toString(),
            weekEndExclusive = end.toString(),
            targetAmount = amount,
            createdAt = payload.getString("created_at").also { Instant.parse(it) },
            supersedesId = parent?.targetId,
            archived = payload.getBoolean("archived"),
            uid = uid,
            syncUpdatedAt = payload.optString("sync_updated_at"),
        )
        if (!incoming.archived && database.collectionsTargetDao().findActive(start.toString())
                ?.uid?.let { it != uid } == true) {
            auditPlanningConflict(record, parentUid ?: "independent_root")
            return PerRecord.CONFLICT
        }
        if (existing == null) {
            database.collectionsTargetDao().insert(incoming)
            return PerRecord.APPLIED
        }
        require(existing.copy(archived = incoming.archived, syncUpdatedAt = incoming.syncUpdatedAt) == incoming) {
            "Collections target history changed in place"
        }
        if (existing == incoming) return PerRecord.PRESENT
        database.syncApplyDao().updateCollectionsTargetStateByUid(uid, incoming.archived, incoming.syncUpdatedAt)
        return PerRecord.UPDATED
    }

    private suspend fun auditPlanningConflict(record: SyncPulledRecord, parentUid: String) {
        persistAudit(com.saaf.milkshop.data.SecurityAuditEntryEntity(
            auditId = 0,
            occurredAt = Instant.now().toString(),
            action = "sync_apply_conflict",
            result = "warning",
            reasonCode = "sibling_planning_revision",
            details = "table=${record.tableName};uid=${record.rowUid};parent=$parentUid",
        ))
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
                    archived = payload.optBoolean("archived", false),
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

    private suspend fun applyCloseAction(record: SyncPulledRecord, execute: suspend (String) -> Unit): PerRecord {
        val uid = record.rowUid
        val payload = record.payload
        val existing = database.syncEnvelopeDao().dailyCloseActionsByUid(listOf(uid)).firstOrNull()
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
        val received = DailyCloseActionEntity(
                businessDate = payload.getString("business_date"),
                action = payload.getString("action"),
                physicalCashCountId = countId,
                actorName = payload.optString("actor_name").ifEmpty { "synced" },
                actorRole = payload.optString("actor_role").ifEmpty { "synced" },
                reason = payload.optNullableString("reason"),
                createdAt = payload.getString("created_at"),
                supersedesActionId = supersedesId,
                uid = uid,
                moneyRoundingVersion = payload.optInt("money_rounding_version", 1),
            )
        if (existing != null) {
            require(existing.copy(actionId = 0, actorUserId = null) == received) {
                "Remote daily close differs from immutable local history"
            }
            return PerRecord.PRESENT
        }
        // Another phone's actor is a snapshot, never a local identity or a
        // permission grant. Only the local-login guard is suspended, inside
        // this record's writer savepoint; all history/count/role guards remain.
        // A rollback (including cancellation) restores the original DDL too.
        val actorGuard = requireNotNull(database.syncApplyDao().verifiedCloseActorTriggerSql()) {
            "Verified daily close actor guard is missing"
        }
        execute("DROP TRIGGER validate_verified_daily_close_actor")
        try {
            database.backupDataDao().insertDailyCloseAction(received)
        } finally {
            execute(actorGuard)
        }
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

    private suspend fun applyProductProfile(payload: JSONObject, productId: Long) {
        val profile = payload.receivedProfile() ?: return
        val rate = profile.nullableFiniteNumber("default_sale_rate")
        require(rate == null || rate > 0.0) { "Invalid product sale rate" }
        database.productProfileDao().save(ProductProfileEntity(
            productId = productId,
            defaultSaleRate = rate,
            inventoryTracking = profile.requiredBoolean("inventory_tracking"),
            archived = profile.requiredBoolean("archived"),
            notes = profile.nullableText("notes"),
            createdAt = profile.requiredCreatedAt(),
        ))
    }

    private suspend fun applyAccountProfile(payload: JSONObject, category: String, accountId: Long) {
        val profile = payload.receivedProfile() ?: return
        when (category) {
            "Customer" -> {
                val creditLimit = profile.nullableFiniteNumber("credit_limit")
                val dailyQuota = profile.nullableFiniteNumber("daily_quota")
                require(creditLimit == null || creditLimit >= 0.0) { "Invalid customer credit limit" }
                require(dailyQuota == null || dailyQuota >= 0.0) { "Invalid customer daily quota" }
                database.customerProfileDao().save(CustomerProfileEntity(
                    accountId = accountId,
                    phone = profile.nullableText("phone"),
                    areaRoute = profile.nullableText("area_route"),
                    address = profile.nullableText("address"),
                    notes = profile.nullableText("notes"),
                    creditLimit = creditLimit,
                    dailyQuota = dailyQuota,
                    active = profile.requiredBoolean("active"),
                    createdAt = profile.requiredCreatedAt(),
                ))
            }
            "Vendor" -> database.vendorProfileDao().save(VendorProfileEntity(
                accountId = accountId,
                phone = profile.nullableText("phone"),
                location = profile.nullableText("location"),
                notes = profile.nullableText("notes"),
                active = profile.requiredBoolean("active"),
                createdAt = profile.requiredCreatedAt(),
            ))
            "Salary" -> {
                val salary = profile.nullableFiniteNumber("reference_salary")
                require(salary == null || salary >= 0.0) { "Invalid reference salary" }
                val joinDate = profile.nullableText("join_date")
                joinDate?.let(LocalDate::parse)
                database.employeeProfileDao().save(EmployeeProfileEntity(
                    accountId = accountId,
                    role = profile.nullableText("role"),
                    phone = profile.nullableText("phone"),
                    referenceSalary = salary,
                    salaryPeriod = profile.nullableText("salary_period"),
                    joinDate = joinDate,
                    active = profile.requiredBoolean("active"),
                    createdAt = profile.requiredCreatedAt(),
                ))
            }
            "Operating Expense" -> {
                val displayName = profile.requiredText("display_name")
                require(!displayName.contains('\n')) { "Invalid expense category name" }
                val active = profile.requiredBoolean("active")
                val createdAt = profile.requiredCreatedAt()
                if (database.expenseCategoryProfileDao().update(accountId, displayName, active, createdAt) == 0) {
                    database.expenseCategoryProfileDao().insert(ExpenseCategoryProfileEntity(
                        accountId = accountId, displayName = displayName, active = active, createdAt = createdAt,
                    ))
                }
            }
            else -> error("Profile is invalid for account category $category")
        }
    }

    private suspend fun auditSkip(record: SyncPulledRecord, reason: String) {
        persistAudit(
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

    private suspend fun auditConflict(incomingUid: String, targetUid: String, existingUid: String) {
        persistAudit(
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

    private class AuditPersistenceFailure(cause: Exception) : IllegalStateException("Sync audit could not be persisted", cause)

    private suspend fun persistAudit(entry: com.saaf.milkshop.data.SecurityAuditEntryEntity) {
        try {
            database.securityAuditDao().insert(entry)
        } catch (failure: Exception) {
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            // This is a failed batch, not a semantic refusal that may advance
            // the cursor and permanently lose the incoming record.
            throw AuditPersistenceFailure(failure)
        }
    }

    private companion object {
        const val MAX_PARK_RETRIES = 5
    }
}

private fun JSONObject.optDoubleOrNullCompat(name: String): Double? =
    if (!has(name) || isNull(name)) null else getDouble(name)

private fun JSONObject.optionalFiniteNumber(name: String): Double? =
    if (!has(name) || isNull(name)) null else requiredFiniteNumber(name)

private fun JSONObject.requiredFiniteNumber(name: String): Double =
    (get(name) as? Number)?.toDouble()?.also { require(it.isFinite()) { "$name must be finite" } }
        ?: error("$name must be a number")

private fun JSONObject.planningParentUid(): String? {
    val uid = optNullableString("supersedes_uid")
    require(uid != null || !has("supersedes_id") || isNull("supersedes_id")) {
        "Planning revision has no supersedes_uid"
    }
    return uid
}

/** org.json's optString returns the literal "null" for JSON null; read honestly. */
private fun JSONObject.optNullableString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }

/** A missing satellite is a legacy parent-only envelope; explicit null is not a delete command. */
private fun JSONObject.receivedProfile(): JSONObject? {
    if (!has("profile")) return null
    return get("profile") as? JSONObject ?: error("Profile must be a complete object")
}

private fun JSONObject.requiredText(name: String): String =
    (get(name) as? String)?.takeIf { it.isNotBlank() }
        ?: error("Profile $name must be text")

    private fun JSONObject.requiredCreatedAt(): String = requiredText("created_at").also { value ->
        // Historical seed/backfill dates have no zone. Validate their calendar
        // fields without assigning a zone or changing the stored representation.
        if (SQLITE_PROFILE_DATE.matches(value)) {
            LocalDateTime.parse(value, SQLITE_PROFILE_FORMAT)
        } else {
            Instant.parse(value)
        }
    }

    private val SQLITE_PROFILE_DATE = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
    private val SQLITE_PROFILE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        .withResolverStyle(ResolverStyle.STRICT)

private fun JSONObject.nullableText(name: String): String? = when (val value = get(name)) {
    JSONObject.NULL -> null
    is String -> value
    else -> error("Profile $name must be text or null")
}

private fun JSONObject.requiredBoolean(name: String): Boolean =
    get(name) as? Boolean ?: error("Profile $name must be boolean")

private fun JSONObject.nullableFiniteNumber(name: String): Double? = when (val value = get(name)) {
    JSONObject.NULL -> null
    is Number -> value.toDouble().also { require(it.isFinite()) { "Profile $name must be finite" } }
    else -> error("Profile $name must be a number or null")
}

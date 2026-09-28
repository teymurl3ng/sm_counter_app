package com.saaf.milkshop.sync

import com.saaf.milkshop.accountJson
import com.saaf.milkshop.attendanceJson
import com.saaf.milkshop.budgetJson
import com.saaf.milkshop.collectionJson
import com.saaf.milkshop.customerProfileJson
import com.saaf.milkshop.dailyCloseActionJson
import com.saaf.milkshop.deliveryJson
import com.saaf.milkshop.deliveryStatusJson
import com.saaf.milkshop.employeeProfileJson
import com.saaf.milkshop.eventJson
import com.saaf.milkshop.expenseCategoryProfileJson
import com.saaf.milkshop.financialJson
import com.saaf.milkshop.inventoryJson
import com.saaf.milkshop.physicalCashCountJson
import com.saaf.milkshop.productJson
import com.saaf.milkshop.productProfileJson
import com.saaf.milkshop.settingJson
import com.saaf.milkshop.spoilageDetailJson
import com.saaf.milkshop.storeProfileJson
import com.saaf.milkshop.vendorProfileJson
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the wire envelopes for a set of outbox rows. Each envelope is
 * `{table, uid, enqueued_at, payload}`; an event's payload embeds its ledger
 * rows, spoilage detail, and actor link; a master row's payload embeds its
 * profile satellite — the same JSON shapes backup format 4 uses, so the
 * future pull-apply path can share one codec. Rows whose local record has
 * vanished are returned separately (nothing to push; the caller drops the
 * orphan queue entries).
 */
class SyncEnvelopeFactory(private val database: MilkShopDatabase) {

    data class BuildResult(
        val envelopes: List<JSONObject>,
        val skippedRows: List<SyncOutboxEntity>,
    )

    suspend fun build(rows: List<SyncOutboxEntity>): BuildResult {
        val envelopes = ArrayList<JSONObject>(rows.size)
        val skipped = ArrayList<SyncOutboxEntity>()
        val dao = database.syncEnvelopeDao()

        rows.groupBy { it.tableName }.forEach { (table, tableRows) ->
            when (table) {
                "events" -> {
                    val byUid = dao.eventsByUid(tableRows.map { it.rowUid }).associateBy { it.eventUid }
                    val eventIds = byUid.values.map { it.eventId }
                    val inventory = dao.inventoryLogsForEvents(eventIds).groupBy { it.eventId }
                    val financial = dao.financialLogsForEvents(eventIds).groupBy { it.eventId }
                    val spoilage = dao.spoilageDetailsForEvents(eventIds).groupBy { it.eventId }
                    val actors = dao.actorLinksForEvents(eventIds).associateBy { it.eventId }
                    tableRows.forEach { row ->
                        val event = byUid[row.rowUid]
                        if (event == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = eventJson(event).apply {
                            put("inventory_logs", JSONArray(inventory[event.eventId].orEmpty().map(::inventoryJson)))
                            put("financial_logs", JSONArray(financial[event.eventId].orEmpty().map(::financialJson)))
                            put("spoilage_details", JSONArray(spoilage[event.eventId].orEmpty().map(::spoilageDetailJson)))
                            actors[event.eventId]?.let { link ->
                                put(
                                    "actor_link",
                                    JSONObject()
                                        .put("actor_user_id", link.actorUserId)
                                        .put("actor_name_snapshot", link.actorNameSnapshot)
                                        .put("actor_role_snapshot", link.actorRoleSnapshot)
                                        .put("attributed_at", link.attributedAt),
                                )
                            }
                        }
                        envelopes += envelope(row, payload)
                    }
                }

                "products" -> {
                    val byUid = dao.productsByUid(tableRows.map { it.rowUid }).associateBy { it.uid }
                    val profiles = dao.productProfilesFor(byUid.values.map { it.productId }).associateBy { it.productId }
                    tableRows.forEach { row ->
                        val product = byUid[row.rowUid]
                        if (product == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = productJson(product)
                        profiles[product.productId]?.let { payload.put("profile", productProfileJson(it)) }
                        envelopes += envelope(row, payload)
                    }
                }

                "accounts" -> {
                    val byUid = dao.accountsByUid(tableRows.map { it.rowUid }).associateBy { it.uid }
                    val accountIds = byUid.values.map { it.accountId }
                    val customers = dao.customerProfilesFor(accountIds).associateBy { it.accountId }
                    val vendors = dao.vendorProfilesFor(accountIds).associateBy { it.accountId }
                    val employees = dao.employeeProfilesFor(accountIds).associateBy { it.accountId }
                    val expenseCategories = dao.expenseCategoryProfilesFor(accountIds).associateBy { it.accountId }
                    tableRows.forEach { row ->
                        val account = byUid[row.rowUid]
                        if (account == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = accountJson(account)
                        val id = account.accountId
                        customers[id]?.let { payload.put("profile", customerProfileJson(it)) }
                        vendors[id]?.let { payload.put("profile", vendorProfileJson(it)) }
                        employees[id]?.let { payload.put("profile", employeeProfileJson(it)) }
                        expenseCategories[id]?.let { payload.put("profile", expenseCategoryProfileJson(it)) }
                        envelopes += envelope(row, payload)
                    }
                }

                "budget_plans" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.budgetsByUid(listOf(row.rowUid)).firstOrNull()?.let { envelope(row, budgetJson(it)) }
                }

                "collections_targets" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.targetsByUid(listOf(row.rowUid)).firstOrNull()?.let { envelope(row, collectionJson(it)) }
                }

                "deliveries" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.deliveriesByUid(listOf(row.rowUid)).firstOrNull()?.let { envelope(row, deliveryJson(it)) }
                }

                "attendance_records" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.attendanceByUid(listOf(row.rowUid)).firstOrNull()?.let { envelope(row, attendanceJson(it)) }
                }

                "delivery_status_history" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.deliveryStatusesByUid(listOf(row.rowUid)).firstOrNull()
                        ?.let { envelope(row, deliveryStatusJson(it)) }
                }

                "daily_close_actions" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.dailyCloseActionsByUid(listOf(row.rowUid)).firstOrNull()
                        ?.let { envelope(row, dailyCloseActionJson(it, includeSecurity = true)) }
                }

                "physical_cash_counts" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.physicalCashCountsByUid(listOf(row.rowUid)).firstOrNull()
                        ?.let { envelope(row, physicalCashCountJson(it)) }
                }

                "settings" -> {
                    val keys = tableRows.map { it.rowUid.removePrefix(SETTINGS_UID_PREFIX) }
                    val byKey = dao.settingsByKeys(keys).associateBy { it.key }
                    tableRows.forEach { row ->
                        val setting = byKey[row.rowUid.removePrefix(SETTINGS_UID_PREFIX)]
                        if (setting == null) {
                            skipped += row
                            return@forEach
                        }
                        envelopes += envelope(row, settingJson(setting))
                    }
                }

                "store_profile" -> {
                    val profile = dao.storeProfile()
                    if (profile == null) {
                        skipped += tableRows
                    } else {
                        tableRows.forEach { row -> envelopes += envelope(row, storeProfileJson(profile)) }
                    }
                }

                else -> skipped += tableRows
            }
        }

        return BuildResult(envelopes, skipped)
    }

    private suspend fun emit(
        rows: List<SyncOutboxEntity>,
        skipped: ArrayList<SyncOutboxEntity>,
        envelopes: ArrayList<JSONObject>,
        build: suspend (SyncOutboxEntity) -> JSONObject?,
    ) {
        rows.forEach { row ->
            val envelope = build(row)
            if (envelope == null) skipped += row else envelopes += envelope
        }
    }

    private fun envelope(row: SyncOutboxEntity, payload: JSONObject): JSONObject = JSONObject()
        .put("table", row.tableName)
        .put("uid", row.rowUid)
        .put("enqueued_at", row.enqueuedAt)
        .put("payload", payload)

    private companion object {
        const val SETTINGS_UID_PREFIX = "setting:"
    }
}

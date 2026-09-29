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
                    // Corrections reference their original by uid, never by the
                    // local integer id (D3): resolve every corrects_event_id in
                    // the batch before serializing.
                    val correctedUids = dao.eventsByIds(
                        byUid.values.mapNotNull { it.correctsEventId }.distinct(),
                    ).associate { it.eventId to it.eventUid }
                    // Children reference parents/products/accounts by uid so a
                    // receiving device can remap to its own local ids.
                    val productUids = dao.productsByIds(
                        inventory.values.flatten().map { it.productId }.distinct() +
                            spoilage.values.flatten().map { it.productId },
                    ).associate { it.productId to it.uid }
                    val accountUids = dao.accountsByIds(
                        financial.values.flatten().map { it.accountId }.distinct(),
                    ).associate { it.accountId to it.uid }
                    tableRows.forEach { row ->
                        val event = byUid[row.rowUid]
                        if (event == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = eventJson(event).apply {
                            event.correctsEventId?.let { original ->
                                correctedUids[original]?.let { uid -> put("corrects_event_uid", uid) }
                            }
                            put("inventory_logs", JSONArray(inventory[event.eventId].orEmpty().map { entry ->
                                inventoryJson(entry).apply {
                                    productUids[entry.productId]?.let { uid -> put("product_uid", uid) }
                                }
                            }))
                            put("financial_logs", JSONArray(financial[event.eventId].orEmpty().map { entry ->
                                financialJson(entry).apply {
                                    accountUids[entry.accountId]?.let { uid -> put("account_uid", uid) }
                                }
                            }))
                            put("spoilage_details", JSONArray(spoilage[event.eventId].orEmpty().map { entry ->
                                spoilageDetailJson(entry).apply {
                                    productUids[entry.productId]?.let { uid -> put("product_uid", uid) }
                                }
                            }))
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

                "deliveries" -> {
                    val byUid = dao.deliveriesByUid(tableRows.map { it.rowUid }).associateBy { it.uid }
                    val linkedSaleUids = dao.eventsByIds(
                        byUid.values.mapNotNull { it.saleEventId }.distinct(),
                    ).associate { it.eventId to it.eventUid }
                    val customerUids = dao.accountsByIds(
                        byUid.values.map { it.customerAccountId }.distinct(),
                    ).associate { it.accountId to it.uid }
                    val productUids = dao.productsByIds(
                        byUid.values.map { it.productId }.distinct(),
                    ).associate { it.productId to it.uid }
                    tableRows.forEach { row ->
                        val delivery = byUid[row.rowUid]
                        if (delivery == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = deliveryJson(delivery).apply {
                            delivery.saleEventId?.let { saleId ->
                                linkedSaleUids[saleId]?.let { uid -> put("sale_event_uid", uid) }
                            }
                            customerUids[delivery.customerAccountId]?.let { put("customer_uid", it) }
                            productUids[delivery.productId]?.let { put("product_uid", it) }
                        }
                        envelopes += envelope(row, payload)
                    }
                }

                "attendance_records" -> {
                    val byUid = dao.attendanceByUid(tableRows.map { it.rowUid }).associateBy { it.uid }
                    val supersedesUids = dao.attendanceByIds(
                        byUid.values.mapNotNull { it.supersedesId }.distinct(),
                    ).associate { it.attendanceId to it.uid }
                    val employeeUids = dao.accountsByIds(
                        byUid.values.map { it.employeeAccountId }.distinct(),
                    ).associate { it.accountId to it.uid }
                    tableRows.forEach { row ->
                        val record = byUid[row.rowUid]
                        if (record == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = attendanceJson(record).apply {
                            record.supersedesId?.let { id ->
                                supersedesUids[id]?.let { uid -> put("supersedes_uid", uid) }
                            }
                            employeeUids[record.employeeAccountId]?.let { put("employee_uid", it) }
                        }
                        envelopes += envelope(row, payload)
                    }
                }

                "delivery_status_history" -> {
                    val byUid = dao.deliveryStatusesByUid(tableRows.map { it.rowUid }).associateBy { it.uid }
                    val deliveryUids = dao.deliveriesByIds(
                        byUid.values.map { it.deliveryId }.distinct(),
                    ).associate { it.deliveryId to it.uid }
                    tableRows.forEach { row ->
                        val status = byUid[row.rowUid]
                        if (status == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = deliveryStatusJson(status).apply {
                            deliveryUids[status.deliveryId]?.let { put("delivery_uid", it) }
                        }
                        envelopes += envelope(row, payload)
                    }
                }

                "budget_plans" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.budgetsByUid(listOf(row.rowUid)).firstOrNull()?.let { budget ->
                        val payload = budgetJson(budget)
                        budget.supersedesId?.let { id ->
                            dao.budgetsByIds(listOf(id)).firstOrNull()
                                ?.let { previous -> payload.put("supersedes_uid", previous.uid) }
                        }
                        envelope(row, payload)
                    }
                }

                "collections_targets" -> emit(tableRows, skipped, envelopes) { row ->
                    dao.targetsByUid(listOf(row.rowUid)).firstOrNull()?.let { target ->
                        val payload = collectionJson(target)
                        target.supersedesId?.let { id ->
                            dao.targetsByIds(listOf(id)).firstOrNull()
                                ?.let { previous -> payload.put("supersedes_uid", previous.uid) }
                        }
                        envelope(row, payload)
                    }
                }

                "daily_close_actions" -> {
                    val byUid = dao.dailyCloseActionsByUid(tableRows.map { it.rowUid }).associateBy { it.uid }
                    val supersedesUids = dao.dailyCloseActionsByIds(
                        byUid.values.mapNotNull { it.supersedesActionId }.distinct(),
                    ).associate { it.actionId to it.uid }
                    tableRows.forEach { row ->
                        val action = byUid[row.rowUid]
                        if (action == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = dailyCloseActionJson(action, includeSecurity = true).apply {
                            action.supersedesActionId?.let { id ->
                                supersedesUids[id]?.let { uid -> put("supersedes_action_uid", uid) }
                            }
                            action.physicalCashCountId?.let { id ->
                                dao.physicalCashCountsByIds(listOf(id)).firstOrNull()
                                    ?.let { count -> put("count_uid", count.uid) }
                            }
                        }
                        envelopes += envelope(row, payload)
                    }
                }

                "physical_cash_counts" -> {
                    val byUid = dao.physicalCashCountsByUid(tableRows.map { it.rowUid }).associateBy { it.uid }
                    val accountUids = dao.accountsByIds(
                        byUid.values.map { it.accountId }.distinct(),
                    ).associate { it.accountId to it.uid }
                    tableRows.forEach { row ->
                        val count = byUid[row.rowUid]
                        if (count == null) {
                            skipped += row
                            return@forEach
                        }
                        val payload = physicalCashCountJson(count).apply {
                            accountUids[count.accountId]?.let { put("account_uid", it) }
                        }
                        envelopes += envelope(row, payload)
                    }
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

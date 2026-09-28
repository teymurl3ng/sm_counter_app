package com.saaf.milkshop.sync

import java.security.MessageDigest
import java.util.UUID

/**
 * Canonical sync identity for rows that predate the uid columns (Room schema 9
 * and earlier). Values are derived only from fields that never change after
 * the row is created, so the same historical row always maps to the same uid —
 * across repeat migrations, across devices importing the same legacy backup,
 * and across restores. Rows created after schema 10 never use these helpers;
 * they take [randomUid] at construction time.
 *
 * The seed rows shipped with every fresh install (DatabaseSql.seedStatements)
 * are the one deliberate exception: they get fixed well-known uids so every
 * device in every lineage already agrees on their identity before the first
 * sync ever runs.
 */
object LegacyUid {
    private const val DOMAIN = "saaf-milk.uid.v1"

    /** Fresh identity for rows created on this device. */
    fun randomUid(): String = "u1_" + UUID.randomUUID().toString().replace("-", "")

    fun event(eventId: Long, eventType: String, createdAt: String): String =
        deterministic("events", eventId, eventType, createdAt)

    fun product(productId: Long, name: String, unit: String): String =
        seedProductUid(name, unit)
            ?: deterministic("products", productId, name, unit)

    fun account(accountId: Long, name: String, category: String): String =
        seedAccountUid(name, category)
            ?: deterministic("accounts", accountId, name, category)

    fun budget(budgetId: Long, category: String, periodStart: String, createdAt: String): String =
        deterministic("budget_plans", budgetId, category, periodStart, createdAt)

    fun collectionsTarget(targetId: Long, weekStart: String, createdAt: String): String =
        deterministic("collections_targets", targetId, weekStart, createdAt)

    fun delivery(deliveryId: Long, businessDate: String, createdAt: String): String =
        deterministic("deliveries", deliveryId, businessDate, createdAt)

    fun attendance(attendanceId: Long, businessDate: String, createdAt: String): String =
        deterministic("attendance_records", attendanceId, businessDate, createdAt)

    fun deliveryStatus(statusId: Long, deliveryId: Long, occurredAt: String): String =
        deterministic("delivery_status_history", statusId, deliveryId, occurredAt)

    fun dailyCloseAction(actionId: Long, businessDate: String, action: String, createdAt: String): String =
        deterministic("daily_close_actions", actionId, businessDate, action, createdAt)

    fun physicalCashCount(countId: Long, businessDate: String, countedAt: String): String =
        deterministic("physical_cash_counts", countId, businessDate, countedAt)

    /** Fixed well-known uid for the seeded Milk/Yogurt products, else null. */
    fun seedProductUid(name: String, unit: String): String? =
        if (seedProducts.any { it.first == name && it.second == unit }) seedUid("products", name) else null

    /** Fixed well-known uid for a seeded ledger account, else null. */
    fun seedAccountUid(name: String, category: String): String? =
        if (seedAccounts[name] == category) seedUid("accounts", name) else null

    private val seedProducts = setOf(
        "Milk" to "Liters",
        "Yogurt" to "Kg",
    )

    private val seedAccounts = mapOf(
        "Cash" to "Cash/Bank",
        "JazzCash" to "Cash/Bank",
        "Bank" to "Cash/Bank",
        "Expense: Rent" to "Operating Expense",
        "Expense: Electricity" to "Operating Expense",
        "Expense: Internet" to "Operating Expense",
        "Expense: Water" to "Operating Expense",
        "Expense: Fuel" to "Operating Expense",
        "Expense: Poly bags" to "Operating Expense",
        "Expense: LPG" to "Operating Expense",
        "Expense: Other" to "Operating Expense",
    )

    private fun seedUid(table: String, name: String): String =
        "u1_seed_" + table + "_" + name.map { if (it.isLetterOrDigit()) it else '_' }

    private fun deterministic(table: String, vararg fields: Any?): String {
        val canonical = buildString {
            append(DOMAIN).append('|').append(table)
            for (field in fields) append('|').append(field?.toString() ?: "")
        }
        val hex = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "u1_" + hex.take(32)
    }
}

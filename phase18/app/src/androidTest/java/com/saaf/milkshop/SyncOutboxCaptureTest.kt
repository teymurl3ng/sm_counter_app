package com.saaf.milkshop

import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.AttendanceRecordEntity
import com.saaf.milkshop.data.BudgetPlanEntity
import com.saaf.milkshop.data.CustomerProfileEntity
import com.saaf.milkshop.data.DailyCloseActionEntity
import com.saaf.milkshop.data.DeliveryEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.PhysicalCashCountEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Phase 1b: capture triggers turn every synced write into a sync_outbox row
 * keyed by row uid, in first-change order, and re-enqueues move to the tail.
 * Restore behavior (empty outbox, recreated triggers, preserved pull cursor)
 * is exercised by the backup suites against the same machinery.
 */
@RunWith(AndroidJUnit4::class)
class SyncOutboxCaptureTest {

    private fun openDatabase(): MilkShopDatabase = MilkShopDatabase.createInMemory()

    @Test
    fun freshDatabase_capturesSeedRows() = runBlocking {
        val database = openDatabase()
        try {
            val outbox = database.syncOutboxDao().nextBatch(1000)
            assertEquals(19L, database.syncOutboxDao().count())
            assertEquals(2, outbox.count { it.tableName == "products" })
            assertEquals(11, outbox.count { it.tableName == "accounts" })
            assertEquals(6, outbox.count { it.tableName == "settings" && it.rowUid.startsWith("setting:") })
            // Seed identities are the shared well-known uids every install agrees on.
            assertTrue(outbox.any { it.tableName == "products" && it.rowUid == "u1_seed_products_Milk" })
            assertTrue(outbox.any { it.tableName == "accounts" && it.rowUid == "u1_seed_accounts_Cash" })
        } finally {
            database.close()
        }
    }

    @Test
    fun eventInsert_isCapturedWithItsUid_andPrecedesItsClose() = runBlocking {
        val database = openDatabase()
        try {
            val event = EventEntity(
                createdAt = "2026-09-28T10:00:00Z",
                occurredAt = "2026-09-28",
                eventType = "SALE",
                saleQuantity = 2.0,
                saleValue = 380.0,
                unitRate = 190.0,
            )
            val eventId = database.eventDao().insert(event)
            val countId = database.physicalCashCountDao().insert(
                PhysicalCashCountEntity(
                    accountId = 1L,
                    businessDate = "2026-09-28",
                    countedAmount = 500.0,
                    countedAt = "2026-09-28T20:00:00Z",
                ),
            )
            database.dailyCloseDao().insert(
                DailyCloseActionEntity(
                    businessDate = "2026-09-28",
                    action = "close",
                    physicalCashCountId = countId,
                    actorName = "Owner",
                    actorRole = "owner",
                    reason = "Test variance between counted and expected cash",
                    createdAt = "2026-09-28T20:00:00Z",
                ),
            )

            val eventRows = database.syncOutboxDao().nextBatch(1000).filter { it.tableName == "events" }
            assertEquals(1, eventRows.size)
            assertEquals(event.eventUid, eventRows.single().rowUid)

            val ordered = database.syncOutboxDao().nextBatch(1000)
                .filter { it.tableName == "events" || it.tableName == "daily_close_actions" }
            val lastEventIndex = ordered.indexOfLast { it.tableName == "events" }
            val closeIndex = ordered.indexOfFirst { it.tableName == "daily_close_actions" }
            assertTrue(closeIndex > lastEventIndex)

            val stored = database.eventDao().findById(eventId)!!
            assertTrue(stored.rawDeviceTs.isNotBlank())
            assertEquals("device_only", stored.timeTrust)
        } finally {
            database.close()
        }
    }

    @Test
    fun mutableUpdate_reenqueuesSingleRowAtTail_andStampsRow() = runBlocking {
        val database = openDatabase()
        try {
            val accountId = database.accountDao().insert(
                AccountEntity(name = "Customer: Capture Test", type = "Asset", category = "Customer"),
            )
            val uid = database.backupDataDao().accounts().single { it.accountId == accountId }.uid
            val idAfterInsert = database.syncOutboxDao().nextBatch(1000).single { it.rowUid == uid }.outboxId

            database.accountDao().updateName(accountId, "Customer: Renamed")

            val rows = database.syncOutboxDao().nextBatch(1000).filter { it.rowUid == uid }
            assertEquals(1, rows.size)
            assertTrue(rows.single().outboxId > idAfterInsert)
            val renamed = database.backupDataDao().accounts().single { it.accountId == accountId }
            assertEquals("Customer: Renamed", renamed.name)
            assertTrue(renamed.syncUpdatedAt.isNotBlank())
        } finally {
            database.close()
        }
    }

    @Test
    fun deliveryLink_attendanceArchive_budgetArchive_areCaptured() = runBlocking {
        val database = openDatabase()
        try {
            val customerAccountId = database.accountDao().insert(
                AccountEntity(name = "Customer: Delivery", type = "Asset", category = "Customer"),
            )
            val employeeAccountId = database.accountDao().insert(
                AccountEntity(name = "Employee: Attendance", type = "Expense", category = "Salary"),
            )

            val deliveryId = database.deliveryDao().insert(
                DeliveryEntity(
                    customerAccountId = customerAccountId,
                    productId = 1L,
                    quantity = 2.0,
                    businessDate = "2026-09-28",
                    createdAt = "2026-09-28T08:00:00Z",
                ),
            )
            val saleEventId = database.eventDao().insert(
                EventEntity(
                    createdAt = "2026-09-28T09:00:00Z",
                    occurredAt = "2026-09-28",
                    eventType = "SALE",
                    saleQuantity = 2.0,
                    saleValue = 380.0,
                    unitRate = 190.0,
                ),
            )
            assertEquals(1, database.deliveryDao().linkSale(deliveryId, saleEventId))

            val attendanceId = database.attendanceDao().insert(
                AttendanceRecordEntity(
                    employeeAccountId = employeeAccountId,
                    businessDate = "2026-09-28",
                    status = "present",
                    createdAt = "2026-09-28T08:00:00Z",
                ),
            )
            assertEquals(1, database.attendanceDao().archive(attendanceId))

            val budgetId = database.budgetPlanDao().insert(
                BudgetPlanEntity(
                    category = "vendor",
                    periodStart = "2026-09-01",
                    periodEndExclusive = "2026-10-01",
                    createdAt = "2026-09-28T08:00:00Z",
                ),
            )
            assertEquals(1, database.budgetPlanDao().archive(budgetId))

            val outbox = database.syncOutboxDao().nextBatch(1000)
            val deliveryUid = database.backupDataDao().deliveries().single { it.deliveryId == deliveryId }.uid
            assertEquals(1, outbox.count { it.tableName == "deliveries" && it.rowUid == deliveryUid })
            val attendanceUid = database.backupDataDao().attendanceRecords().single { it.attendanceId == attendanceId }.uid
            assertEquals(1, outbox.count { it.tableName == "attendance_records" && it.rowUid == attendanceUid })
            val budgetUid = database.backupDataDao().budgets().single { it.budgetId == budgetId }.uid
            assertEquals(1, outbox.count { it.tableName == "budget_plans" && it.rowUid == budgetUid })
        } finally {
            database.close()
        }
    }

    @Test
    fun profileSave_reenqueuesParentMasterRow() = runBlocking {
        val database = openDatabase()
        try {
            val accountId = database.accountDao().insert(
                AccountEntity(name = "Customer: Profile", type = "Asset", category = "Customer"),
            )
            val uid = database.backupDataDao().accounts().single { it.accountId == accountId }.uid
            val idBefore = database.syncOutboxDao().nextBatch(1000).single { it.rowUid == uid }.outboxId

            database.customerProfileDao().save(
                CustomerProfileEntity(accountId = accountId, createdAt = "2026-09-28T08:00:00Z"),
            )

            val idAfter = database.syncOutboxDao().nextBatch(1000).single { it.rowUid == uid }.outboxId
            assertTrue(idAfter > idBefore)
        } finally {
            database.close()
        }
    }
}

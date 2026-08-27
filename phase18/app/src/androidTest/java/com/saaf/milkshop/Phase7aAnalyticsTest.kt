package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.domain.AnalyticsService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class Phase7aAnalyticsTest {

    @Test
    fun periodSummary_usesEffectiveCorrectionTruth_andAggregatesReadOnlyMetrics() {
        runBlocking {
            val app = InstrumentationRegistry.getInstrumentation().targetContext
                .applicationContext as MilkShopApplication
            val database = app.database
            val eventDao = database.eventDao()

            val originalSaleId = eventDao.insert(
                EventEntity(
                    createdAt = "2099-08-20T00:00:00Z",
                    occurredAt = "2099-08-10",
                    eventType = "SALE",
                    saleQuantity = 10.0,
                    saleValue = 100.0,
                    unitRate = 10.0,
                    surchargeApplied = 0.0,
                )
            )

            eventDao.insert(
                EventEntity(
                    createdAt = "2099-08-21T00:00:00Z",
                    occurredAt = "2099-08-10",
                    eventType = "SALE",
                    correctsEventId = originalSaleId,
                    saleQuantity = 13.0,
                    saleValue = 130.0,
                    unitRate = 10.0,
                    surchargeApplied = 0.0,
                )
            )

            eventDao.insert(
                EventEntity(
                    createdAt = "2099-08-20T01:00:00Z",
                    occurredAt = "2099-08-10",
                    eventType = "EXPENSE",
                    amount = 20.0,
                )
            )

            eventDao.insert(
                EventEntity(
                    createdAt = "2099-08-20T02:00:00Z",
                    occurredAt = "2099-09-02",
                    eventType = "SALE",
                    saleQuantity = 7.0,
                    saleValue = 70.0,
                    unitRate = 10.0,
                    surchargeApplied = 0.0,
                )
            )

            val summary = AnalyticsService(database).periodSummary(
                startDateInclusive = "2099-08-01",
                endDateExclusive = "2099-09-01",
            )

            assertEquals(2L, summary.eventCount)
            assertEquals(130.0, summary.salesValue, 1e-9)
            assertEquals(13.0, summary.saleQuantity, 1e-9)
            assertEquals(20.0, summary.operatingExpenses, 1e-9)
        }
    }

    @Test
    fun currentPosition_readsCanonicalModels_withoutWritingEvents() {
        runBlocking {
            val app = InstrumentationRegistry.getInstrumentation().targetContext
                .applicationContext as MilkShopApplication
            val database = app.database
            val beforeEvents = database.eventDao().count()

            val position = AnalyticsService(database).currentPosition()

            assertTrue(position.stock.isNotEmpty())
            assertTrue(position.accountBalances.isNotEmpty())
            assertEquals(beforeEvents, database.eventDao().count())
        }
    }

    @Test
    fun periodSummary_rejectsEmptyOrBackwardsRange() {
        runBlocking {
            val app = InstrumentationRegistry.getInstrumentation().targetContext
                .applicationContext as MilkShopApplication
            val database = app.database
            val service = AnalyticsService(database)

            var threw = false
            try {
                service.periodSummary(
                    startDateInclusive = "2026-09-01",
                    endDateExclusive = "2026-09-01",
                )
            } catch (_: IllegalArgumentException) {
                threw = true
            }

            assertTrue(threw)
        }
    }
}

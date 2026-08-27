package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AttendanceInput
import com.saaf.milkshop.domain.AttendanceService
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.DailyCloseInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.DailyReopenInput
import com.saaf.milkshop.domain.DeliveryInput
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3DailyCloseTest {
    private val database = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun close_requiresLatestCashCount_andLocksClosedAndEarlierDates() = runBlocking {
        val cashId = cashId()
        val count = PhysicalCashCountService(database).record(
            PhysicalCashCountInput(cashId, DATE, 0.0)
        )
        val close = DailyCloseService(database).close(
            DailyCloseInput(DATE, count.count.countId, "Closing Cashier", "cashier")
        )
        assertEquals(DailyCloseService.STATUS_CLOSED, close.status)

        val vendorId = VendorManagementService(database).create("Locked Vendor")
        assertRejected {
            VendorPurchaseService(database).record(
                VendorPurchaseInput(vendorId, DATE, 10.0, 4.0, 28.0)
            )
        }
        assertRejected {
            VendorPurchaseService(database).record(
                VendorPurchaseInput(vendorId, "2026-08-26", 10.0, 4.0, 28.0)
            )
        }
        assertRejected {
            PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, DATE, 0.0))
        }
        assertEquals(0, database.eventDao().count())
    }

    @Test
    fun ownerReopen_allowsLinkedCorrection_thenRequiresNewCountForReclose() = runBlocking {
        val vendorId = VendorManagementService(database).create("Close Vendor")
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, DATE, 10.0, 4.0, 28.0))
        val milkId = database.productDao().findByName("Milk")!!.productId
        val cashId = cashId()
        val sale = SaleService(database).record(
            SaleInput(milkId, DATE, "retail", 190.0, quantity = 1.0, payment = SalePayment.Account(cashId))
        )
        val firstCount = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, DATE, 190.0))
        val closeService = DailyCloseService(database)
        closeService.close(DailyCloseInput(DATE, firstCount.count.countId, "Day Cashier", "cashier"))

        assertRejected {
            CorrectionService(database).correct(
                CorrectionInput.Sale(sale.eventId, DATE, 190.0, quantity = 2.0, channel = "retail")
            )
        }
        assertRejected {
            closeService.reopen(DailyReopenInput(DATE, "Ordinary Cashier", "cashier", "Correction needed"))
        }

        closeService.reopen(DailyReopenInput(DATE, "Shop Owner", "owner", "Correct sale quantity"))
        val correction = CorrectionService(database).correct(
            CorrectionInput.Sale(sale.eventId, DATE, 190.0, quantity = 2.0, channel = "retail")
        )
        assertEquals(sale.eventId, database.eventDao().findById(correction.eventId)!!.correctsEventId)

        assertRejected {
            closeService.close(DailyCloseInput(DATE, firstCount.count.countId, "Day Cashier", "cashier"))
        }
        val secondCount = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, DATE, 380.0))
        val reclose = closeService.close(DailyCloseInput(DATE, secondCount.count.countId, "Day Cashier", "cashier"))
        assertEquals(0.0, reclose.variance!!, 0.0)
        assertEquals(3, closeService.history(DATE).size)
        assertEquals(DailyCloseService.STATUS_CLOSED, closeService.current(DATE)!!.status)
        assertEquals(8.0, database.readModelDao().currentStock().single { it.productId == milkId }.quantityOnHand, 0.0)
    }

    @Test
    fun nonZeroVariance_requiresExplanation_butNeverPostsLedgerMovement() = runBlocking {
        val cashId = cashId()
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, DATE, 100.0))
        val closeService = DailyCloseService(database)
        assertRejected {
            closeService.close(DailyCloseInput(DATE, count.count.countId, "Shop Owner", "owner"))
        }
        val closed = closeService.close(
            DailyCloseInput(DATE, count.count.countId, "Shop Owner", "owner", "Unexplained drawer overage under review")
        )
        assertEquals(100.0, closed.variance!!, 0.0)
        assertEquals(0.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 0.0)
        assertEquals(0, database.eventDao().count())
    }

    @Test
    fun attendanceAndDeliveryChanges_requireReopen() = runBlocking {
        val employeeId = EmployeeManagementService(database).create("Close Employee")
        val customerId = CustomerManagementService(database).create("Close Customer")
        val milkId = database.productDao().findByName("Milk")!!.productId
        val cashId = cashId()
        val count = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, DATE, 0.0))
        val closeService = DailyCloseService(database)
        closeService.close(DailyCloseInput(DATE, count.count.countId, "Shop Manager", "manager"))

        assertRejected { AttendanceService(database).record(AttendanceInput(employeeId, DATE, "present")) }
        assertRejected { DeliveryService(database).create(DeliveryInput(customerId, milkId, 1.0, businessDate = DATE)) }

        closeService.reopen(DailyReopenInput(DATE, "Shop Admin", "admin", "Enter omitted operational records"))
        AttendanceService(database).record(AttendanceInput(employeeId, DATE, "present"))
        DeliveryService(database).create(DeliveryInput(customerId, milkId, 1.0, businessDate = DATE))
        assertEquals("present", AttendanceService(database).current(employeeId, DATE)!!.status)
    }

    private suspend fun cashId(): Long =
        database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

    private suspend fun assertRejected(block: suspend () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: Exception) {
            rejected = true
        }
        assertTrue(rejected)
    }

    private companion object {
        const val DATE = "2026-08-27"
    }
}

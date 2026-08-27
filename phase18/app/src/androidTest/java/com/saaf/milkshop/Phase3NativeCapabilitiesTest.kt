package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AttendanceInput
import com.saaf.milkshop.domain.AttendanceService
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.CustomerProfileService
import com.saaf.milkshop.domain.DeliveryInput
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.EmployeeProfileService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SpoilageInput
import com.saaf.milkshop.domain.SpoilageService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3NativeCapabilitiesTest {
    private val database = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun productAndPeopleProfiles_preserveCanonicalIdentity_andArchiveWithoutDeleting() = runBlocking {
        val productService = ProductCatalogService(database)
        val productId = productService.create(
            ProductCatalogInput("Cream", "Kg", defaultSaleRate = 500.0, notes = "Fresh")
        )
        productService.setArchived(productId, true)
        assertNotNull(database.productDao().findById(productId))
        assertTrue(productService.list(includeArchived = true).single { it.productId == productId }.archived)
        assertFalse(productService.list().any { it.productId == productId })

        val customerId = CustomerManagementService(database).create("Profile Customer")
        CustomerProfileService(database).save(
            customerId,
            CustomerProfileInput(phone = "0300", areaRoute = "Route A", creditLimit = 1_000.0),
        )
        val profile = CustomerProfileService(database).get(customerId)!!
        assertEquals("0300", profile.phone)
        assertEquals("Route A", profile.areaRoute)
        assertEquals(1_000.0, profile.creditLimit!!, 0.0)
        assertEquals("Customer", database.accountDao().findById(customerId)!!.category)

        val vendorId = VendorManagementService(database).create("Profile Vendor")
        assertNotNull(database.vendorProfileDao().findByAccountId(vendorId))
        val employeeId = EmployeeManagementService(database).create("Profile Employee")
        assertNotNull(database.employeeProfileDao().findByAccountId(employeeId))
    }

    @Test
    fun attendanceRevision_isAppendOnly_andSupersedesPreviousRecord() = runBlocking {
        val employeeId = EmployeeManagementService(database).create("Attendance Employee")
        val service = AttendanceService(database)
        val originalId = service.record(AttendanceInput(employeeId, "2026-08-27", "present"))
        val revisionId = service.record(AttendanceInput(employeeId, "2026-08-27", "half_day", "Left early"))

        val current = service.current(employeeId, "2026-08-27")!!
        assertEquals(revisionId, current.attendanceId)
        assertEquals(originalId, current.supersedesId)
        assertEquals("half_day", current.status)
        val history = service.history(employeeId)
        assertEquals(2, history.size)
        assertTrue(history.single { it.attendanceId == originalId }.archived)
    }

    @Test
    fun deliveryLifecycle_linksCanonicalSale_withoutCreatingRevenue() = runBlocking {
        val vendorId = VendorManagementService(database).create("Delivery Vendor")
        val customerId = CustomerManagementService(database).create("Delivery Customer")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-27", 20.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        val sale = SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = "2026-08-27",
                channel = "credit-retail",
                unitRate = 190.0,
                quantity = 2.0,
                payment = SalePayment.OnCredit(customerId),
            )
        )
        val eventCountBeforeDelivery = database.eventDao().count()
        val deliveryService = DeliveryService(database)
        val deliveryId = deliveryService.create(
            DeliveryInput(customerId, milk.productId, 2.0, "Route A", "2026-08-27", sale.eventId)
        )
        deliveryService.transition(deliveryId, DeliveryService.STATUS_DELIVERED)

        val delivery = deliveryService.get(deliveryId)
        assertEquals(DeliveryService.STATUS_DELIVERED, delivery.status)
        assertEquals(2, delivery.statusHistory.size)
        assertEquals(eventCountBeforeDelivery, database.eventDao().count())
        val customerBalance = database.readModelDao().accountBalances().single { it.accountId == customerId }
        assertEquals(380.0, customerBalance.balance, 0.0)
    }

    @Test
    fun spoilageAndCorrection_useInventoryEventAndDeltaSemantics() = runBlocking {
        val vendorId = VendorManagementService(database).create("Spoilage Vendor")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-27", 100.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        val original = SpoilageService(database).record(
            SpoilageInput(milk.productId, 10.0, "2026-08-27", "Spillage")
        )
        val correction = CorrectionService(database).correct(
            CorrectionInput.Spoilage(original.eventId, "2026-08-27", 6.0, "Measured again")
        )

        val stock = database.readModelDao().currentStock().single { it.productId == milk.productId }
        assertEquals(94.0, stock.quantityOnHand, 0.0)
        assertEquals(4.0, database.inventoryLogDao().findByEventId(correction.eventId).single().quantity, 0.0)
        assertEquals(6.0, database.spoilageDao().findByEventId(correction.eventId)!!.quantity, 0.0)
        assertEquals(original.eventId, database.eventDao().findById(correction.eventId)!!.correctsEventId)
        val currentHistory = SpoilageService(database).history()
        assertEquals(listOf(correction.eventId), currentHistory.map { it.eventId })
        val fullHistory = SpoilageService(database).history(includeSuperseded = true)
        assertEquals(2, fullHistory.size)
        assertTrue(fullHistory.single { it.eventId == original.eventId }.superseded)
    }

    @Test
    fun physicalCashCounts_areAppendOnly_andVarianceUsesCanonicalBalance() = runBlocking {
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val service = PhysicalCashCountService(database)
        service.record(PhysicalCashCountInput(cashId, "2026-08-27", 100.0))
        service.record(PhysicalCashCountInput(cashId, "2026-08-27", 90.0))

        val latest = service.latest(cashId, "2026-08-27")!!
        assertEquals(90.0, latest.count.countedAmount, 0.0)
        assertEquals(0.0, latest.expectedBalance, 0.0)
        assertEquals(90.0, latest.variance, 0.0)
        assertEquals(2, database.physicalCashCountDao().all().size)

        val vendorId = VendorManagementService(database).create("Cash Count Vendor")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-27", 10.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        SaleService(database).record(
            SaleInput(
                productId = milk.productId,
                occurredAt = "2026-08-27",
                channel = "retail",
                unitRate = 190.0,
                quantity = 1.0,
                payment = SalePayment.Account(cashId),
            )
        )
        val historical = service.latest(cashId, "2026-08-27")!!
        assertEquals(0.0, historical.expectedBalance, 0.0)
        assertEquals(90.0, historical.variance, 0.0)
        assertEquals(190.0, database.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 0.0)
    }

    @Test
    fun customExpenseCategory_isAccountBacked_andArchivedCategoryCannotPost() = runBlocking {
        val management = ExpenseCategoryManagementService(database)
        val categoryAccountId = management.create("Cleaning")
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val expense = ExpenseService(database).record(
            ExpenseInput("Cleaning", cashId, 250.0, "2026-08-27")
        )
        assertEquals(250.0, database.financialLogDao().findByEventId(expense.eventId).single { it.accountId == categoryAccountId }.amount, 0.0)

        management.setArchived(categoryAccountId, true)
        var rejected = false
        try {
            ExpenseService(database).record(ExpenseInput("Cleaning", cashId, 10.0, "2026-08-27"))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun archivedAndInactiveProfiles_blockNewOperations_withoutDeletingHistory() = runBlocking {
        val vendorId = VendorManagementService(database).create("Lifecycle Vendor")
        VendorPurchaseService(database).record(
            VendorPurchaseInput(vendorId, "2026-08-27", 20.0, 4.0, 28.0)
        )
        val milk = database.productDao().findByName("Milk")!!
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val products = ProductCatalogService(database)
        products.setArchived(milk.productId, true)

        var archivedProductRejected = false
        try {
            SaleService(database).record(
                SaleInput(milk.productId, "2026-08-27", "retail", 190.0, quantity = 1.0, payment = SalePayment.Account(cashId))
            )
        } catch (_: IllegalArgumentException) {
            archivedProductRejected = true
        }
        assertTrue(archivedProductRejected)
        products.setArchived(milk.productId, false)

        val customerId = CustomerManagementService(database).create("Inactive Customer")
        CustomerProfileService(database).save(customerId, CustomerProfileInput(active = false))
        var inactiveCustomerRejected = false
        try {
            SaleService(database).record(
                SaleInput(milk.productId, "2026-08-27", "credit-retail", 190.0, quantity = 1.0, payment = SalePayment.OnCredit(customerId))
            )
        } catch (_: IllegalArgumentException) {
            inactiveCustomerRejected = true
        }
        assertTrue(inactiveCustomerRejected)

        val employeeId = EmployeeManagementService(database).create("Inactive Employee")
        EmployeeProfileService(database).save(employeeId, EmployeeProfileInput(active = false))
        var inactiveEmployeeRejected = false
        try {
            AttendanceService(database).record(AttendanceInput(employeeId, "2026-08-27", "present"))
        } catch (_: IllegalArgumentException) {
            inactiveEmployeeRejected = true
        }
        assertTrue(inactiveEmployeeRejected)
    }
}

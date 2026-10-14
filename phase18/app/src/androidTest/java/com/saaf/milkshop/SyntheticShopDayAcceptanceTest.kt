package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BridgeCommandConflict
import com.saaf.milkshop.application.bridge.BridgeCommandDayClosed
import com.saaf.milkshop.application.cashclose.*
import com.saaf.milkshop.application.collection.*
import com.saaf.milkshop.application.commands.*
import com.saaf.milkshop.application.expense.*
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.application.vendorpayment.*
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.SyncScheduler
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Literal oracle: audit/2026-10-13/shop-day-scope/EXPECTED.json, prepared before app execution. */
@RunWith(AndroidJUnit4::class)
class SyntheticShopDayAcceptanceTest {
    private lateinit var db: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var operations: OperationsBridgeCommandService
    private lateinit var history: HistoryBridgeCommandService
    private lateinit var deliveries: DeliveryBridgeCommandService
    private lateinit var close: NativeCashCloseFacade
    private lateinit var sales: NativeSaleFacade
    private lateinit var owner: String
    private lateinit var cashier: String
    private lateinit var operator: String
    private var ownerId = 0L
    private var cashierId = 0L
    private var operatorId = 0L
    private var cashId = 0L
    private var customerId = 0L
    private var vendorId = 0L
    private var milkId = 0L
    private var yogurtId = 0L
    private var fuelId = 0L
    private lateinit var day: LocalDate

    @Before fun setup() = runBlocking {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName == "com.saaf.milkshop.qa.tests")
        SyncScheduler.suppressSchedulingForTest()
        db = MilkShopDatabase.createInMemory()
        security = UserSecurityService(db)
        operations = OperationsBridgeCommandService(db, security)
        history = HistoryBridgeCommandService(db, security)
        deliveries = DeliveryBridgeCommandService(db, security)
        close = NativeCashCloseFacade(db, security)
        sales = NativeSaleFacade(db, security)
        // Explicit shop dates; no persisted/global clock changes or midnight simulation.
        day = LocalDate.now(ZoneId.of("Asia/Karachi")).minusDays(1)
        val boot = security.bootstrapOwner("day.owner", "Day Owner", OWNER_PHRASE.toCharArray())
        owner = boot.session.sessionId
        ownerId = boot.user.userId
        cashier = staff("cashier", "Day Cashier")
        operator = staff("operator", "Day Operator")
        cashierId = security.sessionManager().require(cashier).userId
        operatorId = security.sessionManager().require(operator).userId
        cashId = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        milkId = db.productDao().findByName("Milk")!!.productId
        yogurtId = db.productDao().findByName("Yogurt")!!.productId
        fuelId = db.expenseCategoryProfileDao().findByDisplayName("Fuel")!!.accountId
        vendorId = VendorManagementService(db).create("Day Supplier")
        customerId = CustomerManagementService(db).create("Day Customer")
        StoreIdentityService(db).save(StoreIdentityInput("Shop Day QA"))
        BusinessSettingsService(db).update(BusinessSettings(170.0, 11.0, 13.0, 190.0, 280.0, 50.0))
        OpeningStateService(db).record(OpeningStateInput(
            day.toString(), accountBalances = listOf(OpeningAccountBalance(cashId, 10_000.0)),
        ))
        security.reauthenticate(owner, OWNER_PHRASE.toCharArray())
        val policy = CostingPolicyService(db, security).approve(owner, CostingPolicyInput(
            calendarMonth = YearMonth.from(day).toString(), normalCapacityKg = BigDecimal("1000"),
            reviewReason = "monthly", components = listOf(
                component("culture", 100_000), component("packaging", 200_000),
                component("variable_energy", 300_000), component("direct_labor", 400_000),
                component("fixed_production_overhead", 500_000),
            ), notes = "Synthetic allocation inputs; no new cash expense",
        ))
        assertEquals(0, BigDecimal("1500").compareTo(policy.conversionCostPaisaPerKg))
        balances(0)
    }

    @After fun cleanup() {
        if (::db.isInitialized) db.close()
    }

    @Test fun authenticatedShopDayMatchesIndependentLedgerAndCost() = runBlocking {
        completeDay()
    }

    @Test fun nextDateStartsOpenWithCarriedBalances() = runBlocking {
        completeDay()
        val oldRows = ledger()
        val context = close.load(cashier, day.plusDays(1))
        assertTrue(context.postingOpen)
        assertEquals(933_500L, context.expectedCashPaisa)
        assertEquals(oldRows, ledger())
        val receipt = sales.record(cashier, sale(milkId, 0.5), day.plusDays(1)).receipt
        assertEquals(9_500L, receipt.totalPaisa)
        double(0.5, receipt.quantity)
        assertEvent(receipt.eventId, "SALE", cashierId, "cashier", mapOf(milkId to -0.5), mapOf(cashId to 95.0), day.plusDays(1))
        val event = db.eventDao().findById(receipt.eventId)!!
        double(95.0, event.saleValue!!)
        balances(10)
        assertTrue(db.dailyCloseDao().isPostingClosed(day.toString()))
        assertTrue(close.load(cashier, day.plusDays(1)).postingOpen)
        assertEquals(1, db.backupDataDao().openingBatches().size)
        assertEquals(3, db.backupDataDao().dailyCloseActions().size)
    }

    private suspend fun completeDay() {
        refused<SecurityFailure.PermissionDenied> {
            operations.recordProduction(cashier, YogurtProductionInput(day.toString(), 20.0, 17.0))
        }
        refused<SecurityFailure.PermissionDenied> {
            NativeVendorPaymentFacade(db, security).record(cashier, NativeVendorPaymentRequest(vendorId, cashId, 100_000, null), day)
        }
        val purchase = operations.recordPurchase(operator, VendorPurchaseInput(vendorId, day.toString(), 100.0, 4.0, 28.0))
        double(8.6, purchase.snfPercentage)
        double(12.6, purchase.tsPercentage)
        double(96.92307692307692, purchase.adjustedLiters)
        double(17576.923076923078, purchase.amountOwed)
        assertEvent(purchase.eventId, "PURCHASE_RECEIPT", operatorId, "operator", mapOf(milkId to 100.0), mapOf(vendorId to 17576.923076923078))
        val header = db.eventDao().findById(purchase.eventId)!!
        double(100.0, header.rawLiters!!); double(4.0, header.fatPercentage!!); double(28.0, header.lrReading!!)
        double(8.6, header.snfPercentage!!); double(12.6, header.tsPercentage!!)
        double(96.92307692307692, header.adjustedLiters!!); double(17576.923076923078, header.amountOwed!!)
        double(170.0, header.milkRateUsed!!); double(11.0, header.deliveryFeeUsed!!); double(13.0, header.tsStandardUsed!!)
        balances(1)

        val production = operations.recordProduction(operator, YogurtProductionInput(day.toString(), 20.0, 17.0))
        assertEvent(production.eventId, "PRODUCTION", operatorId, "operator", mapOf(milkId to -20.0, yogurtId to 17.0), emptyMap())
        val produced = db.eventDao().findById(production.eventId)!!
        double(20.0, produced.milkLitersUsed!!); double(17.0, produced.yogurtKgProduced!!)
        assertTrue(db.backupDataDao().productionLossAssessments().isEmpty())
        assertTrue(db.backupDataDao().yieldPolicyVersions().isEmpty())
        balances(2)

        val milkSale = sales.record(cashier, sale(milkId, 2.0), day).receipt
        assertEquals(38_000L, milkSale.totalPaisa); double(2.0, milkSale.quantity)
        assertEvent(milkSale.eventId, "SALE", cashierId, "cashier", mapOf(milkId to -2.0), mapOf(cashId to 380.0))
        val original = db.eventDao().findById(milkSale.eventId)!!
        double(2.0, original.saleQuantity!!); double(380.0, original.saleValue!!)
        double(190.0, original.unitRate!!); double(0.0, original.surchargeApplied)
        val originalInventory = db.inventoryLogDao().findByEventId(milkSale.eventId)
        val originalMoney = db.financialLogDao().findByEventId(milkSale.eventId)
        balances(3)

        val yogurtSale = sales.record(cashier, sale(yogurtId, 1.0, credit = true), day).receipt
        assertEquals(28_000L, yogurtSale.totalPaisa)
        assertEvent(yogurtSale.eventId, "SALE", cashierId, "cashier", mapOf(yogurtId to -1.0), mapOf(customerId to 280.0))
        val credit = db.eventDao().findById(yogurtSale.eventId)!!
        double(1.0, credit.saleQuantity!!); double(280.0, credit.saleValue!!); double(280.0, credit.unitRate!!)
        balances(4)

        val collection = NativeCollectionFacade(db, security).record(cashier, NativeCollectionRequest(customerId, cashId, 10_000, null), day).receipt
        assertEquals(10_000L, collection.amountPaisa); assertEquals(18_000L, collection.customerBalanceAfterPaisa)
        assertEvent(collection.eventId, "CREDIT_COLLECTION", cashierId, "cashier", emptyMap(), mapOf(customerId to -100.0, cashId to 100.0))
        double(100.0, db.eventDao().findById(collection.eventId)!!.amount!!)
        balances(5)

        val expense = NativeExpenseFacade(db, security).record(cashier, NativeExpenseRequest(fuelId, cashId, 5_000, "Synthetic Fuel"), day).receipt
        assertEquals(5_000L, expense.amountPaisa)
        assertEvent(expense.eventId, "EXPENSE", cashierId, "cashier", emptyMap(), mapOf(cashId to -50.0, fuelId to 50.0))
        double(50.0, db.eventDao().findById(expense.eventId)!!.amount!!)
        balances(6)

        val payment = NativeVendorPaymentFacade(db, security).record(owner, NativeVendorPaymentRequest(vendorId, cashId, 100_000, null), day).receipt
        assertEquals(100_000L, payment.amountPaisa)
        assertEvent(payment.eventId, "VENDOR_PAYMENT", ownerId, "owner", emptyMap(), mapOf(vendorId to -1000.0, cashId to -1000.0))
        double(1000.0, db.eventDao().findById(payment.eventId)!!.amount!!)
        balances(7)

        val beforeDelivery = ledger()
        val deliveryId = deliveries.create(cashier, DeliveryInput(customerId, yogurtId, 1.0, businessDate = day.toString(), saleEventId = yogurtSale.eventId))
        assertEquals(beforeDelivery, ledger())
        val delivery = db.deliveryDao().findById(deliveryId)!!
        assertEquals(yogurtSale.eventId, delivery.saleEventId)
        assertEquals(customerId, delivery.customerAccountId); assertEquals(yogurtId, delivery.productId)
        double(1.0, delivery.quantity); assertEquals(day.toString(), delivery.businessDate)
        assertEquals("pending", db.deliveryDao().latestStatus(deliveryId)!!.status)
        assertTrue(db.securityAuditDao().all().any { it.action == DeliveryBridgeCommandService.ACTION_CREATE_DELIVERY && it.actorUserId == cashierId })
        balances(8)

        val correction = history.correct(owner, CorrectionInput.Sale(milkSale.eventId, day.toString(), 190.0, quantity = 1.5))
        assertEvent(correction.eventId, "SALE", ownerId, "owner", mapOf(milkId to 0.5), mapOf(cashId to -95.0))
        val corrected = db.eventDao().findById(correction.eventId)!!
        assertEquals(milkSale.eventId, corrected.correctsEventId)
        double(1.5, corrected.saleQuantity!!); double(285.0, corrected.saleValue!!)
        double(190.0, corrected.unitRate!!); double(0.0, corrected.surchargeApplied)
        assertEquals(original, db.eventDao().findById(milkSale.eventId))
        assertEquals(originalInventory, db.inventoryLogDao().findByEventId(milkSale.eventId))
        assertEquals(originalMoney, db.financialLogDao().findByEventId(milkSale.eventId))
        balances(9)
        costing(production.eventId, correction.eventId, yogurtSale.eventId)

        val ledgerBeforeCounts = ledger()
        val count = close.recordCount(cashier, NativeCashCloseCountRequest(933_000, "QA first staged count"), day).count
        assertEquals(933_500L, count.expectedCashPaisa); assertEquals(933_000L, count.countedCashPaisa); assertEquals(-500L, count.variancePaisa)
        refused<IllegalArgumentException>("pending deliveries") { close.closeDay(cashier, NativeCashCloseDayRequest(count.countId, VARIANCE_REASON), day) }
        close.resolvePendingDelivery(cashier, deliveryId, day)
        assertEquals(ledgerBeforeCounts, ledger())
        assertEquals(listOf("pending", "delivered"), db.deliveryDao().statusHistory(deliveryId).map { it.status })
        refused<BridgeCommandConflict> { deliveries.transition(cashier, deliveryId, "delivered", null) }
        refused<IllegalArgumentException>("variance") { close.closeDay(cashier, NativeCashCloseDayRequest(count.countId, null), day) }
        val first = close.closeDay(cashier, NativeCashCloseDayRequest(count.countId, VARIANCE_REASON), day)
        assertEquals(933_500L, first.receipt.expectedCashPaisa); assertEquals(933_000L, first.receipt.countedCashPaisa)
        assertEquals(-500L, first.receipt.variancePaisa); assertFalse(first.refreshedContext.postingOpen)
        refused<IllegalStateException>("already closed") { close.closeDay(cashier, NativeCashCloseDayRequest(count.countId, VARIANCE_REASON), day) }
        refused<BridgeCommandDayClosed> { operations.recordSale(cashier, SaleInput(milkId, day.toString(), "retail", 190.0, quantity = 0.5, payment = SalePayment.Account(cashId))) }
        refused<BridgeCommandDayClosed> { history.correct(owner, CorrectionInput.Sale(correction.eventId, day.toString(), 190.0, quantity = 1.0)) }
        refused<SecurityFailure.PermissionDenied> { close.reopenDay(cashier, REOPEN_REASON, STAFF_PHRASE.toCharArray(), day) }
        refused<IllegalArgumentException> { close.reopenDay(owner, " ", OWNER_PHRASE.toCharArray(), day) }
        refused<SecurityFailure.Authentication> { close.reopenDay(owner, REOPEN_REASON, "incorrect synthetic phrase".toCharArray(), day) }
        val phrase = OWNER_PHRASE.toCharArray()
        val reopened = close.reopenDay(owner, REOPEN_REASON, phrase, day)
        assertTrue(phrase.all { it == '\u0000' }); assertTrue(reopened.refreshedContext.postingOpen)
        refused<BridgeCommandConflict>("again after reopening") { close.closeDay(cashier, NativeCashCloseDayRequest(count.countId, VARIANCE_REASON), day) }
        val recount = close.recordCount(cashier, NativeCashCloseCountRequest(933_500, "QA recount includes the PKR 5 missed in the first count"), day).count
        val final = close.closeDay(cashier, NativeCashCloseDayRequest(recount.countId, null), day)
        assertEquals(933_500L, final.receipt.expectedCashPaisa); assertEquals(933_500L, final.receipt.countedCashPaisa)
        assertEquals(0L, final.receipt.variancePaisa)
        assertEquals(ledgerBeforeCounts, ledger())
        balances(9)
        val actions = db.dailyCloseDao().history(day.toString())
        assertEquals(listOf("close", "reopen", "close"), actions.map { it.action })
        assertEquals(listOf(cashierId, ownerId, cashierId), actions.map { it.actorUserId })
        assertEquals(listOf("cashier", "owner", "cashier"), actions.map { it.actorRole })
        assertEquals(listOf("Day Cashier", "Day Owner", "Day Cashier"), actions.map { it.actorName })
        assertEquals(listOf(null, first.receipt.actionId, reopened.actionId), actions.map { it.supersedesActionId })
        assertEquals(listOf(count.countId, null, recount.countId), actions.map { it.physicalCashCountId })
        assertEquals(listOf(VARIANCE_REASON, REOPEN_REASON, null), actions.map { it.reason })
        assertEquals(2, db.backupDataDao().physicalCashCounts().size)
        assertEquals(listOf(9330.0, 9335.0), db.backupDataDao().physicalCashCounts().map { it.countedAmount })
        assertTrue(db.backupDataDao().physicalCashCounts().all { it.businessDate == day.toString() && it.accountId == cashId })
        assertTrue(db.backupDataDao().physicalCashCounts().last().countedAt > actions[1].createdAt)
        assertEquals(9, db.backupDataDao().events().size)
        assertEquals(6, db.backupDataDao().inventoryLogs().size)
        assertEquals(11, db.backupDataDao().financialLogs().size)
    }

    private suspend fun costing(productionId: Long, milkSaleId: Long, yogurtSaleId: Long) {
        val projection = InventoryCostProjectionService(db).rebuild()
        assertEquals(CostCoverageCode.COMPLETE, projection.coverageCode)
        val movements = db.inventoryCostProjectionDao().allMovements()
        assertEquals(5, movements.size)
        assertEquals(351_538L, movements.single { it.sourceEventId == productionId && it.movementKind == "production_input" }.costOutPaisa)
        assertEquals(377_038L, movements.single { it.sourceEventId == productionId && it.movementKind == "production_output" }.costInPaisa)
        assertEquals(26_365L, movements.single { it.sourceEventId == milkSaleId }.costOutPaisa)
        assertEquals(22_179L, movements.single { it.sourceEventId == yogurtSaleId }.costOutPaisa)
        assertEquals(1_379_789L, movements.last { it.productId == milkId }.runningCostPaisa)
        assertEquals(354_859L, movements.last { it.productId == yogurtId }.runningCostPaisa)
        val report = ProfitabilityReadService(db).report(day.toString(), day.plusDays(1).toString())
        assertEquals(CostCoverageCode.COMPLETE, report.coverageCode)
        assertEquals(56_500L, report.products.sumOf { it.salesRevenuePaisa })
        assertEquals(48_544L, report.products.sumOf { it.costOfProductsSoldPaisa!! })
        assertEquals(7_956L, report.products.sumOf { it.profitBeforeShopExpensesPaisa!! })
        assertEquals(5_000L, report.recordedOperatingExpensesPaisa)
        assertEquals(0L, report.recordedSalaryPaymentsPaisa); assertEquals(0L, report.deliverySurchargePaisa)
        assertEquals(2_956L, report.estimatedRecordedResultPaisa)
    }

    private suspend fun assertEvent(id: Long, type: String, actorId: Long, role: String, inventory: Map<Long, Double>, money: Map<Long, Double>, date: LocalDate = day) {
        val event = db.eventDao().findById(id)!!
        assertEquals(type, event.eventType); assertEquals(date.toString(), event.occurredAt)
        assertTrue(event.eventUid.isNotBlank()); assertTrue(event.createdAt.isNotBlank())
        val savedInventory = db.inventoryLogDao().findByEventId(id)
        assertEquals(inventory.size, savedInventory.size)
        assertEquals(inventory.keys, savedInventory.map { it.productId }.toSet())
        savedInventory.forEach { double(inventory.getValue(it.productId), it.quantity) }
        val savedMoney = db.financialLogDao().findByEventId(id)
        assertEquals(money.size, savedMoney.size)
        assertEquals(money.keys, savedMoney.map { it.accountId }.toSet())
        savedMoney.forEach { double(money.getValue(it.accountId), it.amount) }
        val actor = db.eventActorLinkDao().find(id)!!
        assertEquals(actorId, actor.actorUserId); assertEquals(role, actor.actorRoleSnapshot)
        assertEquals("Day ${role.replaceFirstChar { it.uppercase() }}", actor.actorNameSnapshot)
        assertTrue(db.securityAuditDao().all().any { it.result == "success" && it.actorUserId == actorId && it.details?.split(';')?.contains("event_id=$id") == true })
    }

    private suspend fun balances(step: Int) {
        val expected = EXPECTED[step]
        for ((index, id) in listOf(cashId, customerId, vendorId).withIndex()) {
            val value = db.readModelDao().accountBalance(id)!!.balance
            val paisa = BigDecimal.valueOf(value).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
            assertEquals("step $step account $id", expected[index].toLong(), paisa)
        }
        val stock = db.readModelDao().currentStock().associate { it.productId to it.quantityOnHand }
        assertEquals("step $step Milk", expected[3], stock.getValue(milkId), 0.000001)
        assertEquals("step $step Yogurt", expected[4], stock.getValue(yogurtId), 0.000001)
        assertTrue(db.accountDao().findByCategory("Cash/Bank").filter { it.accountId != cashId }.all {
            (db.readModelDao().accountBalance(it.accountId)?.balance ?: 0.0) == 0.0
        })
    }

    private suspend fun ledger(): List<Any> = listOf(db.backupDataDao().events(), db.backupDataDao().inventoryLogs(), db.backupDataDao().financialLogs())
    private suspend fun facts(): List<Any> = ledger() + listOf(db.backupDataDao().dailyCloseActions(), db.backupDataDao().physicalCashCounts(), db.backupDataDao().deliveries(), db.backupDataDao().deliveryStatuses(), db.backupDataDao().eventActorLinks())

    private suspend inline fun <reified T : Throwable> refused(fragment: String? = null, block: () -> Unit) {
        val before = facts()
        val failure = runCatching(block).exceptionOrNull()
        assertTrue("Expected ${T::class.java.simpleName}, got $failure", failure is T)
        if (fragment != null) assertTrue("Expected '$fragment', got ${failure?.message}", failure!!.message.orEmpty().contains(fragment, true))
        assertEquals("Refused operation changed saved ledger/close/delivery/actor facts", before, facts())
    }

    private suspend fun staff(role: String, display: String): String {
        val enrollment = security.beginUserEnrollment(owner, CreateUserInput("day.$role", display, role))
        security.completeUserEnrollment(enrollment.enrollmentToken, STAFF_PHRASE.toCharArray())
        return security.login("day.$role", STAFF_PHRASE.toCharArray()).sessionId
    }
    private fun component(code: String, amount: Long) = ConversionCostComponentInput(code, amount, BigDecimal("1000"))
    private fun sale(productId: Long, quantity: Double, credit: Boolean = false) = NativeSaleRequest(
        productId, NativeSaleEntryMode.QUANTITY, quantity,
        if (credit) NativeSalePaymentKind.CUSTOMER_ACCOUNT else NativeSalePaymentKind.PAID_NOW,
        if (credit) null else cashId, if (credit) customerId else null,
    )
    private fun double(expected: Double, actual: Double) = assertEquals(expected, actual, 0.000001)

    companion object {
        private const val OWNER_PHRASE = "synthetic day owner phrase 2026"
        private const val STAFF_PHRASE = "synthetic day staff phrase 2026"
        private const val VARIANCE_REASON = "QA staged count: PKR 5 missed during first count"
        private const val REOPEN_REASON = "QA review the recorded drawer count"
        // Cash/customer/supplier paisa, physical Milk L/Yogurt kg. Independent EXPECTED.json.
        private val EXPECTED = listOf(
            doubleArrayOf(1_000_000.0, 0.0, 0.0, 0.0, 0.0),
            doubleArrayOf(1_000_000.0, 0.0, 1_757_692.0, 100.0, 0.0),
            doubleArrayOf(1_000_000.0, 0.0, 1_757_692.0, 80.0, 17.0),
            doubleArrayOf(1_038_000.0, 0.0, 1_757_692.0, 78.0, 17.0),
            doubleArrayOf(1_038_000.0, 28_000.0, 1_757_692.0, 78.0, 16.0),
            doubleArrayOf(1_048_000.0, 18_000.0, 1_757_692.0, 78.0, 16.0),
            doubleArrayOf(1_043_000.0, 18_000.0, 1_757_692.0, 78.0, 16.0),
            doubleArrayOf(943_000.0, 18_000.0, 1_657_692.0, 78.0, 16.0),
            doubleArrayOf(943_000.0, 18_000.0, 1_657_692.0, 78.0, 16.0),
            doubleArrayOf(933_500.0, 18_000.0, 1_657_692.0, 78.5, 16.0),
            doubleArrayOf(943_000.0, 18_000.0, 1_657_692.0, 78.0, 16.0),
        )
    }
}

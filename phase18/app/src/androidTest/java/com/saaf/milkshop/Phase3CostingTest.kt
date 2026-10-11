package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.ConversionCostComponentInput
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.CostCoverageCode
import com.saaf.milkshop.domain.CostReportingScope
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.InventoryCostProjectionService
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.OpeningCostBasisService
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.ProfitabilityReadService
import com.saaf.milkshop.domain.ProductionLossAssessmentInput
import com.saaf.milkshop.domain.ProductionLossAssessmentService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionService
import com.saaf.milkshop.domain.YieldAlertAssessment
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.domain.YieldPolicyService
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3CostingTest {
    private val database = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun milkPurchaseAndSale_releaseMovingAverageCost_andSeparateSurcharge() = runBlocking {
        val vendorId = vendor()
        val cashId = cash()
        val milk = database.productDao().findByName("Milk")!!
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, DATE, 100.0, 4.0, 28.0))
        SaleService(database).record(
            SaleInput(milk.productId, DATE, "retail", 190.0, quantity = 10.0,
                payment = SalePayment.Account(cashId), surchargeEnabled = true)
        )

        val projection = InventoryCostProjectionService(database).rebuild()
        val movements = database.inventoryCostProjectionDao().allMovements()
        val sale = movements.single { it.movementKind == "sale" }
        assertEquals(175_769L, sale.costOutPaisa)
        assertEquals("90", sale.runningQuantity)
        assertEquals(1_581_923L, sale.runningCostPaisa)
        assertEquals(CostCoverageCode.COMPLETE, projection.coverageCode)

        val report = ProfitabilityReadService(database).report(DATE, "2026-08-28")
        val milkProfit = report.products.single()
        assertEquals(190_000L, milkProfit.salesRevenuePaisa)
        assertEquals(175_769L, milkProfit.costOfProductsSoldPaisa)
        assertEquals(14_231L, milkProfit.profitBeforeShopExpensesPaisa)
        assertEquals(5_000L, report.deliverySurchargePaisa)
        assertEquals(19_231L, report.estimatedRecordedResultPaisa)
        assertEquals(CostReportingScope.REPORTING_BASIS, report.reportingBasis)
        assertEquals(CostReportingScope.VALUATION_SCOPE, report.valuationScope)
    }

    @Test
    fun correctedSale_replaysLeafTruth_notLedgerDelta() = runBlocking {
        val vendorId = vendor()
        val cashId = cash()
        val milk = database.productDao().findByName("Milk")!!
        VendorPurchaseService(database).record(VendorPurchaseInput(vendorId, DATE, 100.0, 4.0, 28.0))
        val original = SaleService(database).record(
            SaleInput(milk.productId, DATE, "retail", 190.0, quantity = 10.0, payment = SalePayment.Account(cashId))
        )
        CorrectionService(database).correct(
            CorrectionInput.Sale(original.eventId, DATE, 190.0, quantity = 20.0)
        )

        val report = ProfitabilityReadService(database).report(DATE, "2026-08-28")
        val row = report.products.single()
        assertEquals(0, BigDecimal("20.0").compareTo(row.quantitySold))
        assertEquals(380_000L, row.salesRevenuePaisa)
        assertEquals(351_538L, row.costOfProductsSoldPaisa)
        assertEquals(1, database.inventoryCostProjectionDao().allMovements().count { it.movementKind == "sale" })
    }

    @Test
    fun missingOpeningBasis_withholdsProfit_untilOwnerSuppliesAuditedBasis() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("cost.owner", "Cost Owner", OWNER_PASSWORD.toCharArray())
        StoreIdentityService(database).save(StoreIdentityInput("Saaf Test"))
        val milk = database.productDao().findByName("Milk")!!
        val opening = OpeningStateService(database).record(
            OpeningStateInput(DATE, inventoryBalances = listOf(OpeningInventoryBalance(milk.productId, 10.0)))
        )
        SaleService(database).record(
            SaleInput(milk.productId, DATE, "retail", 200.0, quantity = 2.0, payment = SalePayment.Account(cash()))
        )

        val unavailable = ProfitabilityReadService(database).report(DATE, "2026-08-28")
        assertNull(unavailable.products.single().costOfProductsSoldPaisa)
        assertNull(unavailable.estimatedRecordedResultPaisa)
        assertEquals(CostCoverageCode.MISSING_BASIS, unavailable.coverageCode)

        OpeningCostBasisService(database, security).record(
            owner.session.sessionId,
            OpeningCostBasisInput(
                openingEventId = opening.eventId,
                productId = milk.productId,
                quantity = BigDecimal("10"),
                totalCostPaisa = 150_000,
                basisKind = "owner_estimate",
                evidenceNotes = "Owner estimate from the purchase rate used on opening day",
            )
        )
        val available = ProfitabilityReadService(database).report(DATE, "2026-08-28")
        assertEquals(30_000L, available.products.single().costOfProductsSoldPaisa)
        assertEquals(10_000L, available.products.single().profitBeforeShopExpensesPaisa)
        assertEquals(CostCoverageCode.COMPLETE, available.coverageCode)
    }

    @Test
    fun yogurtProduction_transfersMilkCost_andAddsApprovedMonthlyConversionCost() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("yogurt.owner", "Yogurt Owner", OWNER_PASSWORD.toCharArray())
        val policy = CostingPolicyService(database, security).approve(
            owner.session.sessionId,
            CostingPolicyInput(
                calendarMonth = "2026-08",
                normalCapacityKg = BigDecimal("100"),
                reviewReason = "monthly",
                components = listOf(
                    ConversionCostComponentInput("culture", 1_000, BigDecimal.ONE),
                    ConversionCostComponentInput("packaging", 0, BigDecimal.ONE),
                    ConversionCostComponentInput("variable_energy", 0, BigDecimal.ONE),
                    ConversionCostComponentInput("direct_labor", 0, BigDecimal.ONE),
                    ConversionCostComponentInput("fixed_production_overhead", 0, BigDecimal("100")),
                ),
            )
        )
        assertEquals(BigDecimal("1000.000000000000"), policy.conversionCostPaisaPerKg)
        VendorPurchaseService(database).record(VendorPurchaseInput(vendor(), DATE, 100.0, 4.0, 28.0))
        YogurtProductionService(database).record(YogurtProductionInput(DATE, 10.0, 8.0))
        val yogurt = database.productDao().findByName("Yogurt")!!
        SaleService(database).record(
            SaleInput(yogurt.productId, DATE, "retail", 250.0, quantity = 4.0, payment = SalePayment.Account(cash()))
        )

        val report = ProfitabilityReadService(database).report(DATE, "2026-08-28")
        val row = report.products.single { it.productId == yogurt.productId }
        assertEquals(91_885L, row.costOfProductsSoldPaisa)
        assertEquals(8_115L, row.profitBeforeShopExpensesPaisa)
        assertEquals(CostCoverageCode.COMPLETE, row.coverageCode)
        assertTrue(database.inventoryCostProjectionDao().allMovements().any {
            it.movementKind == "production_output" && it.appliedPolicyId == policy.version.policyId
        })
    }

    @Test
    fun identifiedAbnormalProductionLoss_isExpensedInsteadOfLoadedIntoYogurt() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("loss.owner", "Loss Owner", OWNER_PASSWORD.toCharArray())
        approveSimplePolicy(security, owner.session.sessionId)
        VendorPurchaseService(database).record(VendorPurchaseInput(vendor(), DATE, 100.0, 4.0, 28.0))
        val production = YogurtProductionService(database).record(YogurtProductionInput(DATE, 10.0, 8.0))
        ProductionLossAssessmentService(database, security).record(
            owner.session.sessionId,
            ProductionLossAssessmentInput(
                productionEventId = production.eventId,
                classification = "abnormal",
                abnormalMilkLiters = BigDecimal("2"),
                reasonCode = "spill",
            ),
        )
        val yogurt = database.productDao().findByName("Yogurt")!!
        SaleService(database).record(
            SaleInput(yogurt.productId, DATE, "retail", 250.0, quantity = 8.0, payment = SalePayment.Account(cash()))
        )

        val report = ProfitabilityReadService(database).report(DATE, "2026-08-28")
        val row = report.products.single { it.productId == yogurt.productId }
        val milkLoss = report.products.single { it.productName == "Milk" }
        assertEquals(148_615L, row.costOfProductsSoldPaisa)
        assertEquals(35_154L, milkLoss.abnormalLossPaisa)
        assertEquals(16_231L, report.estimatedRecordedResultPaisa)
    }

    @Test
    fun yieldCalibration_requiresObservedHistory_andAlertNeverPostsALoss() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("yield.owner", "Yield Owner", OWNER_PASSWORD.toCharArray())
        VendorPurchaseService(database).record(VendorPurchaseInput(vendor(), "2026-07-01", 100.0, 4.0, 28.0))
        val productions = (0 until 30).map { offset ->
            YogurtProductionService(database).record(
                YogurtProductionInput(LocalDate.parse("2026-07-01").plusDays(offset.toLong()).toString(), 1.0, 0.85)
            ).eventId
        }
        val yields = YieldPolicyService(database, security)
        assertEquals("calibration_pending", yields.assess(productions.first()).status)
        val policy = yields.approve(
            owner.session.sessionId,
            YieldPolicyInput(productions, effectiveFrom = "2026-07-31"),
        )
        assertEquals("0.82", policy.lowerAlertLimit)
        assertEquals("0.88", policy.upperAlertLimit)
        val unusual = YogurtProductionService(database).record(YogurtProductionInput("2026-07-31", 1.0, 0.70))
        assertEquals("review_required", yields.assess(unusual.eventId).status)
        val normal = YogurtProductionService(database).record(YogurtProductionInput("2026-07-31", 1.0, 0.85))
        val batch = yields.assessAll(listOf(productions.first(), unusual.eventId, normal.eventId))
        assertEquals(yields.assess(productions.first()), batch.getValue(productions.first()))
        assertEquals(yields.assess(unusual.eventId), batch.getValue(unusual.eventId))
        assertEquals("within_expected_range", batch.getValue(normal.eventId).status)
        assertEquals(emptyMap<Long, YieldAlertAssessment>(), yields.assessAll(emptyList()))
        assertTrue(runCatching { yields.assessAll(listOf(unusual.eventId, Long.MIN_VALUE)) }.isFailure)
        assertNull(database.productionLossAssessmentDao().activeForEvent(unusual.eventId))
    }

    @Test
    fun materialConversionCostChange_requiresExplicitReviewReason() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("policy.owner", "Policy Owner", OWNER_PASSWORD.toCharArray())
        approveSimplePolicy(security, owner.session.sessionId)
        val changed = CostingPolicyInput(
            calendarMonth = "2026-09",
            normalCapacityKg = BigDecimal("100"),
            reviewReason = "monthly",
            components = listOf(
                ConversionCostComponentInput("culture", 1_200, BigDecimal.ONE),
                ConversionCostComponentInput("packaging", 0, BigDecimal.ONE),
                ConversionCostComponentInput("variable_energy", 0, BigDecimal.ONE),
                ConversionCostComponentInput("direct_labor", 0, BigDecimal.ONE),
                ConversionCostComponentInput("fixed_production_overhead", 0, BigDecimal("100")),
            ),
        )
        assertTrue(runCatching {
            CostingPolicyService(database, security).approve(owner.session.sessionId, changed)
        }.isFailure)
        assertNull(database.costingPolicyDao().activeForMonth("2026-09"))
        val reviewed = CostingPolicyService(database, security).approve(
            owner.session.sessionId,
            changed.copy(reviewReason = "component_change"),
        )
        assertEquals("component_change", reviewed.version.reviewReason)
    }

    private suspend fun approveSimplePolicy(security: UserSecurityService, sessionId: String) =
        CostingPolicyService(database, security).approve(
            sessionId,
            CostingPolicyInput(
                calendarMonth = "2026-08",
                normalCapacityKg = BigDecimal("100"),
                reviewReason = "monthly",
                components = listOf(
                    ConversionCostComponentInput("culture", 1_000, BigDecimal.ONE),
                    ConversionCostComponentInput("packaging", 0, BigDecimal.ONE),
                    ConversionCostComponentInput("variable_energy", 0, BigDecimal.ONE),
                    ConversionCostComponentInput("direct_labor", 0, BigDecimal.ONE),
                    ConversionCostComponentInput("fixed_production_overhead", 0, BigDecimal("100")),
                ),
            ),
        )

    private suspend fun vendor(): Long = database.accountDao().insert(
        AccountEntity(name = "Vendor: Cost ${System.nanoTime()}", type = "Liability", category = "Vendor")
    )

    private suspend fun cash(): Long =
        database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

    private companion object {
        const val DATE = "2026-08-27"
        const val OWNER_PASSWORD = "correct horse battery staple"
    }
}

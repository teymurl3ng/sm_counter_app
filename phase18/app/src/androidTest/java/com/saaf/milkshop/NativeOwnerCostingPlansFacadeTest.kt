package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.ConversionCostComponentInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.YieldPolicyInput
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerCostingPlansFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerCostingPlansFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String
    private var nowEpochMs: Long = 4_000_000L

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        nowEpochMs = 4_000_000L
        val sessions = NativeSessionManager { nowEpochMs }
        security = UserSecurityService(database, sessions = sessions, nowEpochMs = { nowEpochMs })
        facade = NativeOwnerCostingPlansFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.costing",
            "Costing Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "costing.cashier",
                displayName = "Costing Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("costing.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadPoliciesListsVersionsWithStatusAndRate() = runBlocking {
        val approved = facade.approvePolicy(
            ownerSession,
            CostingPolicyInput(
                calendarMonth = "2026-09",
                normalCapacityKg = BigDecimal("100"),
                reviewReason = "monthly",
                components = simpleComponents(),
            ),
        )

        val snapshot = facade.loadPolicies(ownerSession, "2026-09-21")
        assertEquals("2026-09", snapshot.businessMonth)
        assertTrue(snapshot.currentMonthSummary.contains("2026-09"))
        assertTrue(snapshot.policies.any { it.policyId == approved.row.policyId })
        val row = snapshot.policies.first { it.policyId == approved.row.policyId }
        assertEquals("Active", row.statusLabel)
        assertEquals("monthly", row.reviewReason)
        assertTrue(row.conversionCostLabel.contains("paisa/kg"))
    }

    @Test
    fun loadDetailReturnsComponents() = runBlocking {
        val approved = facade.approvePolicy(
            ownerSession,
            CostingPolicyInput(
                calendarMonth = "2026-08",
                normalCapacityKg = BigDecimal("50"),
                reviewReason = "monthly",
                components = simpleComponents(capacity = BigDecimal("50")),
            ),
        )
        val detail = facade.loadDetail(ownerSession, approved.row.policyId)
        assertEquals("2026-08", detail.row.calendarMonth)
        assertEquals("50", detail.normalCapacityKg)
        assertEquals(5, detail.components.size)
        assertTrue(detail.components.any { it.componentCode == "culture" })
        assertEquals(1_000L, detail.components.first { it.componentCode == "culture" }.sourceAmountPaisa)
    }

    @Test
    fun approvePolicyCreatesActiveVersionInTestDb() = runBlocking {
        val approved = facade.approvePolicy(
            ownerSession,
            CostingPolicyInput(
                calendarMonth = "2026-10",
                normalCapacityKg = BigDecimal("100"),
                reviewReason = "monthly",
                components = simpleComponents(),
                notes = "October standard",
            ),
        )
        assertEquals("2026-10", approved.row.calendarMonth)
        assertEquals("Active", approved.row.statusLabel)
        assertEquals("October standard", approved.notes)
        assertNotNull(database.costingPolicyDao().activeForMonth("2026-10"))
        assertEquals(approved.row.policyId, database.costingPolicyDao().activeForMonth("2026-10")!!.policyId)
    }

    @Test
    fun approvePolicySupersedesActiveVersion() = runBlocking {
        val first = facade.approvePolicy(
            ownerSession,
            CostingPolicyInput(
                calendarMonth = "2026-09",
                normalCapacityKg = BigDecimal("100"),
                reviewReason = "monthly",
                components = simpleComponents(),
            ),
        )
        val second = facade.approvePolicy(
            ownerSession,
            CostingPolicyInput(
                calendarMonth = "2026-09",
                normalCapacityKg = BigDecimal("100"),
                reviewReason = "monthly",
                components = simpleComponents(),
                notes = "Corrected",
                supersedesPolicyId = first.row.policyId,
            ),
        )
        val snapshot = facade.loadPolicies(ownerSession, "2026-09-21")
        val firstRow = snapshot.policies.first { it.policyId == first.row.policyId }
        val secondRow = snapshot.policies.first { it.policyId == second.row.policyId }
        assertEquals("Superseded", firstRow.statusLabel)
        assertEquals("Active", secondRow.statusLabel)
        assertEquals(second.row.policyId, database.costingPolicyDao().activeForMonth("2026-09")!!.policyId)
    }

    @Test
    fun reauthenticateWipesPassphrase() = runBlocking {
        val secret = OWNER_PASSPHRASE.toCharArray()
        facade.reauthenticate(ownerSession, secret)
        assertTrue(secret.all { it == '\u0000' })
        facade.approvePolicy(
            ownerSession,
            CostingPolicyInput(
                calendarMonth = "2026-11",
                normalCapacityKg = BigDecimal("100"),
                reviewReason = "monthly",
                components = simpleComponents(),
            ),
        )
        assertNotNull(database.costingPolicyDao().activeForMonth("2026-11"))
    }

    @Test
    fun loadPoliciesEmptyStateIsHonest() = runBlocking {
        val snapshot = facade.loadPolicies(ownerSession, "2026-09-21")
        assertTrue(snapshot.policies.isEmpty())
        assertTrue(snapshot.currentMonthSummary.contains("No costing policy"))
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.loadPolicies(cashierSession, "2026-09-21") }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun approveRejectsNonOwner() = runBlocking {
        val rejected = runCatching {
            facade.approvePolicy(
                cashierSession,
                CostingPolicyInput(
                    calendarMonth = "2026-09",
                    normalCapacityKg = BigDecimal("100"),
                    reviewReason = "monthly",
                    components = simpleComponents(),
                ),
            )
        }.exceptionOrNull()
        assertTrue(rejected is SecurityFailure.PermissionDenied || rejected != null)
    }

    @Test
    fun upsertBudgetCreatesMilkPlan() = runBlocking {
        val week = facade.loadPlanning(ownerSession).defaultMilkWeekStart
        val start = java.time.LocalDate.parse(week)
        val result = facade.upsertBudget(
            ownerSession,
            BudgetPlanInput(
                category = BUDGET_CATEGORY_MILK_PURCHASES,
                periodStart = start,
                periodEndExclusive = start.plusDays(7),
                plannedLiters = 120.0,
            ),
        )
        assertTrue(result.budgetId > 0L)
        val planning = facade.loadPlanning(ownerSession)
        assertTrue(planning.budgets.any { it.budgetId == result.budgetId && !it.archived })
    }

    @Test
    fun createOrReviseCollectionsTargetSavesWeek() = runBlocking {
        val week = java.time.LocalDate.parse(facade.loadPlanning(ownerSession).defaultMilkWeekStart)
        val result = facade.createOrReviseCollectionsTarget(ownerSession, week, 400_000.0)
        assertEquals(400_000.0, result.targetAmount, 1e-9)
        val planning = facade.loadPlanning(ownerSession)
        assertTrue(planning.collectionTargets.any { it.targetId == result.targetId && !it.archived })
    }

    @Test
    fun approveYieldPolicyFromThirtyBatches() = runBlocking {
        seedThirtyProductions()
        val context = facade.loadYieldContext(ownerSession)
        assertEquals(30, context.batches.size)
        val ids = context.batches.map { it.productionEventId }
        val end = database.inventoryCostProjectionDao().effectiveEvents()
            .filter { it.eventId in ids }
            .maxOf { java.time.LocalDate.parse(it.occurredAt) }
        val approved = facade.approveYieldPolicy(
            ownerSession,
            YieldPolicyInput(
                acceptedProductionEventIds = ids,
                effectiveFrom = end.plusDays(1).toString(),
            ),
        )
        assertEquals("Active", approved.statusLabel)
        assertEquals(30, approved.acceptedBatchCount)
        val refreshed = facade.loadYieldContext(ownerSession)
        assertEquals(approved.yieldPolicyId, refreshed.activePolicyId)
    }

    @Test
    fun planningRejectsNonOwner() = runBlocking {
        assertTrue(runCatching { facade.loadPlanning(cashierSession) }.isFailure)
    }

    @Test
    fun openingCostsReadRecordAndCorrectWithoutErasingHistory() = runBlocking {
        StoreIdentityService(database).save(StoreIdentityInput("Saaf Milk"))
        val milk = database.productDao().findByName("Milk")!!
        val opening = OpeningStateService(database).record(
            OpeningStateInput(
                openingDate = "2026-09-01",
                inventoryBalances = listOf(OpeningInventoryBalance(milk.productId, 10.0)),
            ),
        )

        val empty = facade.loadOpeningCosts(ownerSession)
        assertEquals(1, empty.rows.size)
        assertEquals(null, empty.rows.single().basisId)
        assertTrue(empty.summary.contains("0 of 1"))

        val firstSnapshot = facade.recordOpeningCostBasis(
            ownerSession,
            OpeningCostBasisInput(
                openingEventId = opening.eventId,
                productId = milk.productId,
                quantity = BigDecimal("10"),
                totalCostPaisa = 150_000L,
                basisKind = "documented_historical",
                evidenceNotes = "Supplier bill retained",
            ),
        )
        val first = firstSnapshot.rows.single()
        assertNotNull(first.basisId)
        assertEquals("PKR 1,500.00", first.totalCostLabel)
        assertTrue(firstSnapshot.summary.contains("All supported"))

        val correctedSnapshot = facade.recordOpeningCostBasis(
            ownerSession,
            OpeningCostBasisInput(
                openingEventId = opening.eventId,
                productId = milk.productId,
                quantity = BigDecimal("10"),
                totalCostPaisa = 160_000L,
                basisKind = "owner_estimate",
                evidenceNotes = "Reconstructed from the opening-day purchase rate",
                supersedesBasisId = first.basisId,
            ),
        )
        val corrected = correctedSnapshot.rows.single()
        assertTrue(corrected.basisId != first.basisId)
        assertEquals("Owner estimate", corrected.basisKindLabel)
        assertEquals(2, database.inventoryCostBasisDao().all().size)
        assertNotNull(database.inventoryCostBasisDao().find(first.basisId!!))
        assertEquals(
            corrected.basisId,
            database.inventoryCostBasisDao().active(opening.eventId, milk.productId)!!.basisId,
        )
    }

    @Test
    fun openingCostsRejectNonOwner() = runBlocking {
        val rejected = runCatching { facade.loadOpeningCosts(cashierSession) }.exceptionOrNull()
        assertTrue(rejected is SecurityFailure.PermissionDenied || rejected != null)
    }

    @Test
    fun openingCostSaveRequiresRecentFullAuthentication() = runBlocking {
        StoreIdentityService(database).save(StoreIdentityInput("Saaf Milk"))
        val milk = database.productDao().findByName("Milk")!!
        val opening = OpeningStateService(database).record(
            OpeningStateInput(
                openingDate = "2026-09-01",
                inventoryBalances = listOf(OpeningInventoryBalance(milk.productId, 10.0)),
            ),
        )
        nowEpochMs += NativeSessionManager.RECENT_AUTH_TIMEOUT_MS

        val rejected = runCatching {
            facade.recordOpeningCostBasis(
                ownerSession,
                OpeningCostBasisInput(
                    openingEventId = opening.eventId,
                    productId = milk.productId,
                    quantity = BigDecimal("10"),
                    totalCostPaisa = 150_000L,
                    basisKind = "documented_historical",
                ),
            )
        }.exceptionOrNull()

        assertTrue(rejected is SecurityFailure.RecentAuthenticationRequired)
        assertTrue(database.inventoryCostBasisDao().all().isEmpty())
    }

    private suspend fun seedThirtyProductions() {
        val vendorId = database.accountDao().insert(
            com.saaf.milkshop.data.AccountEntity(
                name = "Vendor: Yield seed",
                type = "Liability",
                category = "Vendor",
            ),
        )
        com.saaf.milkshop.domain.VendorPurchaseService(database).record(
            com.saaf.milkshop.domain.VendorPurchaseInput(
                vendorId,
                "2026-07-01",
                100.0,
                4.0,
                28.0,
            ),
        )
        repeat(30) { offset ->
            com.saaf.milkshop.domain.YogurtProductionService(database).record(
                com.saaf.milkshop.domain.YogurtProductionInput(
                    java.time.LocalDate.parse("2026-07-01").plusDays(offset.toLong()).toString(),
                    1.0,
                    0.85,
                ),
            )
        }
    }

    private fun simpleComponents(capacity: BigDecimal = BigDecimal("100")) = listOf(
        ConversionCostComponentInput("culture", 1_000, BigDecimal.ONE),
        ConversionCostComponentInput("packaging", 0, BigDecimal.ONE),
        ConversionCostComponentInput("variable_energy", 0, BigDecimal.ONE),
        ConversionCostComponentInput("direct_labor", 0, BigDecimal.ONE),
        ConversionCostComponentInput("fixed_production_overhead", 0, capacity),
    )

    companion object {
        private const val OWNER_PASSPHRASE = "owner costing plans passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "costing cashier passphrase 2026!!"
    }
}

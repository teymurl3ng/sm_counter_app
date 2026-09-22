package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.ConversionCostComponentInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
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

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
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
